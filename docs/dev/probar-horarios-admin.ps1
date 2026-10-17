# Prueba manual de la etapa 4b (disponibilidad y bloqueos desde la API).
# Requisitos: base de datos arriba y la app corriendo en OTRA ventana, arrancada con ADMIN_EMAIL y ADMIN_PASSWORD.
# Ejecutar desde la carpeta del proyecto:
#   powershell -ExecutionPolicy Bypass -File docs/dev/probar-horarios-admin.ps1
# Usa un DOMINGO de madrugada (03:00 a 07:00) como datos de prueba para no mezclarse con los datos reales,
# y limpia todo al final.

$base = "http://localhost:8080"
$sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$script:ok = 0
$script:fallos = 0

$correo = Read-Host "Correo del administrador"
$claveSegura = Read-Host "Contrasena del administrador" -AsSecureString
$clave = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveSegura))

# Proximo domingo desde manana (siempre cae dentro de la ventana de reservas)
$d = (Get-Date).Date.AddDays(1)
while ($d.DayOfWeek -ne [System.DayOfWeek]::Sunday) { $d = $d.AddDays(1) }
$domingo = $d.ToString("yyyy-MM-dd")

function TokenCsrf { return $sesion.Cookies.GetCookies([Uri]$base)["XSRF-TOKEN"].Value }

function Probar($titulo, $esperado, $metodo, $ruta, $json = $null, $form = $null, $csrf = $null) {
    # PowerShell guarda en la sesion los encabezados enviados con -Headers: se borra antes de cada llamada.
    $sesion.Headers.Remove("X-XSRF-TOKEN") | Out-Null
    $parametros = @{ Method = $metodo; Uri = "$base$ruta"; WebSession = $sesion; UseBasicParsing = $true }
    if ($csrf) { $parametros.Headers = @{ "X-XSRF-TOKEN" = $csrf } }
    if ($form) { $parametros.Body = $form }
    if ($json) {
        $parametros.ContentType = "application/json; charset=utf-8"
        $parametros.Body = [System.Text.Encoding]::UTF8.GetBytes(($json | ConvertTo-Json))
    }
    $codigo = 0
    $cuerpo = $null
    try {
        $resp = Invoke-WebRequest @parametros
        $codigo = [int]$resp.StatusCode
        $cuerpo = $resp.Content
    } catch {
        if ($_.Exception.Response -eq $null) {
            Write-Host "No se pudo conectar con $base . La app esta corriendo en otra ventana?" -ForegroundColor Red
            exit 1
        }
        $codigo = [int]$_.Exception.Response.StatusCode
        $cuerpo = $_.ErrorDetails.Message
    }
    if ($codigo -eq $esperado) {
        $script:ok++
        Write-Host ("  OK     {0}  (HTTP {1})" -f $titulo, $codigo) -ForegroundColor Green
    } else {
        $script:fallos++
        Write-Host ("  FALLA  {0}  (esperaba {1}, llego {2})" -f $titulo, $esperado, $codigo) -ForegroundColor Red
        if ($cuerpo) { Write-Host "         $cuerpo" -ForegroundColor Yellow }
    }
    if ($cuerpo) {
        try { return ($cuerpo | ConvertFrom-Json) } catch { return $null }
    }
    return $null
}

function Comprobar($titulo, $condicion) {
    if ($condicion) { $script:ok++; Write-Host "  OK     $titulo" -ForegroundColor Green }
    else { $script:fallos++; Write-Host "  FALLA  $titulo" -ForegroundColor Red }
}

function HorasLibres() {
    $r = Probar "Horarios libres del $domingo" 200 "GET" "/api/horarios?fecha=$domingo"
    return @($r.horarios | ForEach-Object { $_.hora })
}

function Titulo($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }

Titulo "A. Entrar y limpiar restos de ejecuciones anteriores"
Probar "Sin sesion, la disponibilidad esta protegida" 401 "GET" "/api/admin/disponibilidad" | Out-Null
Probar "Cargar el token CSRF" 200 "GET" "/api/admin/sesion" | Out-Null
Probar "Login correcto" 200 "POST" "/api/admin/login" -form @{ email = $correo; password = $clave } -csrf (TokenCsrf) | Out-Null
Probar "Refrescar el token tras el login" 200 "GET" "/api/admin/sesion" | Out-Null

$previos = Probar "Listar la disponibilidad actual" 200 "GET" "/api/admin/disponibilidad"
foreach ($p in @($previos | Where-Object { $_.diaSemana -eq 7 })) {
    Probar "Limpiar un tramo de domingo previo" 204 "DELETE" "/api/admin/disponibilidad/$($p.id)" -csrf (TokenCsrf) | Out-Null
}
$bloqPrevios = Probar "Listar los bloqueos actuales" 200 "GET" "/api/admin/bloqueos"
foreach ($b in @($bloqPrevios | Where-Object { $_.fecha -eq $domingo })) {
    Probar "Limpiar un bloqueo previo del domingo" 204 "DELETE" "/api/admin/bloqueos/$($b.id)" -csrf (TokenCsrf) | Out-Null
}

Titulo "B. Disponibilidad semanal"
Probar "Crear SIN token CSRF es rechazado" 403 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 7; horaInicio = "03:00"; horaFin = "05:00" } | Out-Null
$t1 = Probar "Crear domingo 03:00 a 05:00" 201 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 7; horaInicio = "03:00"; horaFin = "05:00" } -csrf (TokenCsrf)
Write-Host ("         id = {0}, dia = {1}" -f $t1.id, $t1.nombreDia)
Probar "Un tramo que se superpone es rechazado" 409 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 7; horaInicio = "04:00"; horaFin = "06:00" } -csrf (TokenCsrf) | Out-Null
$t2 = Probar "Un tramo contiguo (05:00 a 07:00) es valido" 201 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 7; horaInicio = "05:00"; horaFin = "07:00" } -csrf (TokenCsrf)
Probar "Datos invalidos dan 400 con el detalle" 400 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 9; horaInicio = "25:00"; horaFin = "07:00" } -csrf (TokenCsrf) | Out-Null
Probar "Un tramo mas corto que una cita es rechazado" 400 "POST" "/api/admin/disponibilidad" -json @{ diaSemana = 6; horaInicio = "10:00"; horaFin = "10:30" } -csrf (TokenCsrf) | Out-Null
$libres = HorasLibres
Comprobar "El domingo ofrece 03:00, 04:00, 05:00 y 06:00 (hay: $($libres -join ', '))" (($libres -join ",") -eq "03:00,04:00,05:00,06:00")

Titulo "C. Bloqueos"
Probar "Bloqueo con fecha pasada es rechazado" 400 "POST" "/api/admin/bloqueos" -json @{ fecha = "2020-01-01" } -csrf (TokenCsrf) | Out-Null
Probar "Bloqueo con una sola hora es rechazado" 400 "POST" "/api/admin/bloqueos" -json @{ fecha = $domingo; horaInicio = "04:00" } -csrf (TokenCsrf) | Out-Null
$b1 = Probar "Bloquear el domingo de 04:00 a 05:00" 201 "POST" "/api/admin/bloqueos" -json @{ fecha = $domingo; horaInicio = "04:00"; horaFin = "05:00"; motivo = "Prueba" } -csrf (TokenCsrf)
Write-Host ("         solicitudes pendientes afectadas = {0}" -f $b1.solicitudesPendientesAfectadas)
$libres = HorasLibres
Comprobar "Ya no se ofrece el horario de las 04:00 (hay: $($libres -join ', '))" (($libres -join ",") -eq "03:00,05:00,06:00")

Titulo "D. Un bloqueo no puede pisar una reserva confirmada"
$telefono = "9" + (Get-Random -Minimum 10000000 -Maximum 99999999)
$h = Probar "Horarios del domingo" 200 "GET" "/api/horarios?fecha=$domingo"
$inicio03 = ($h.horarios | Where-Object { $_.hora -eq "03:00" }).inicio
$cli = Probar "Un cliente solicita las 03:00" 201 "POST" "/api/reservas" -json @{ nombreCliente = "Prueba Bloqueo"; telefono = $telefono; inicio = $inicio03 }
$lista = Probar "Listar solicitudes pendientes" 200 "GET" "/api/admin/reservas?estado=SOLICITADA"
$idReserva = ($lista | Where-Object { $_.telefono -eq "+56$telefono" }).id
Probar "El administrador confirma las 03:00" 200 "POST" "/api/admin/reservas/$idReserva/confirmar" -csrf (TokenCsrf) | Out-Null
Probar "Bloquear todo el domingo con una confirmada es rechazado" 409 "POST" "/api/admin/bloqueos" -json @{ fecha = $domingo } -csrf (TokenCsrf) | Out-Null
Probar "Bloquear 05:00 a 06:00 (no toca la confirmada) es valido" 201 "POST" "/api/admin/bloqueos" -json @{ fecha = $domingo; horaInicio = "05:00"; horaFin = "06:00" } -csrf (TokenCsrf) | Out-Null

Titulo "E. Limpiar todo"
Probar "Cancelar la reserva de prueba" 200 "POST" "/api/admin/reservas/$idReserva/cancelar" -csrf (TokenCsrf) | Out-Null
$bloqs = Probar "Listar bloqueos" 200 "GET" "/api/admin/bloqueos"
foreach ($b in @($bloqs | Where-Object { $_.fecha -eq $domingo })) {
    Probar "Eliminar bloqueo $($b.id)" 204 "DELETE" "/api/admin/bloqueos/$($b.id)" -csrf (TokenCsrf) | Out-Null
}
Probar "Eliminar el tramo 03:00-05:00" 204 "DELETE" "/api/admin/disponibilidad/$($t1.id)" -csrf (TokenCsrf) | Out-Null
Probar "Eliminar el tramo 05:00-07:00" 204 "DELETE" "/api/admin/disponibilidad/$($t2.id)" -csrf (TokenCsrf) | Out-Null
Probar "Eliminar un tramo que ya no existe da 404" 404 "DELETE" "/api/admin/disponibilidad/$($t1.id)" -csrf (TokenCsrf) | Out-Null
$libres = HorasLibres
Comprobar "El domingo vuelve a no ofrecer horarios" ($libres.Count -eq 0)
Probar "Logout" 200 "POST" "/api/admin/logout" -csrf (TokenCsrf) | Out-Null

Write-Host ""
if ($script:fallos -eq 0) { Write-Host ("TODO BIEN: {0} comprobaciones correctas." -f $script:ok) -ForegroundColor Green }
else { Write-Host ("HAY {0} FALLAS ({1} correctas). Copia las lineas FALLA y mandamelas." -f $script:fallos, $script:ok) -ForegroundColor Red }
