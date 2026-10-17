# Prueba manual de la etapa 4 (login y panel del administrador).
# Requisitos: base de datos arriba y la app corriendo en OTRA ventana, arrancada con ADMIN_EMAIL y ADMIN_PASSWORD.
# Ejecutar desde la carpeta del proyecto:
#   powershell -ExecutionPolicy Bypass -File docs/dev/probar-admin.ps1
# Pide el correo y la contrasena del administrador (la contrasena no se muestra ni se guarda en ningun archivo).

$base = "http://localhost:8080"
$fecha = "2026-10-20"
$horaPrueba = "21:00"
$sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$script:ok = 0
$script:fallos = 0
$script:ultimosHeaders = $null

$correo = Read-Host "Correo del administrador"
$claveSegura = Read-Host "Contrasena del administrador" -AsSecureString
$clave = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($claveSegura))

function TokenCsrf { return $sesion.Cookies.GetCookies([Uri]$base)["XSRF-TOKEN"].Value }

# Hace la llamada, compara el codigo HTTP con el esperado y devuelve el cuerpo (JSON) ya convertido.
function Probar($titulo, $esperado, $metodo, $ruta, $form = $null, $json = $null, $csrf = $null) {
    # PowerShell guarda en la sesion los encabezados enviados con -Headers: sin esta linea, una llamada
    # "sin token" arrastraria el token de la llamada anterior y la prueba no probaria lo que dice probar.
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
        $script:ultimosHeaders = $resp.Headers
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

function Titulo($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }

Titulo "A. Sin sesion"
Probar "Panel sin iniciar sesion es rechazado" 401 "GET" "/api/admin/reservas" | Out-Null
$s = Probar "El estado de la sesion es publico y dice 'no autenticado'" 200 "GET" "/api/admin/sesion"
Write-Host ("         autenticado = {0}" -f $s.autenticado)
Probar "Una ruta desconocida esta denegada (no 404 informativo)" 401 "GET" "/api/cosa-que-no-existe" | Out-Null

Titulo "B. Inicio de sesion"
Probar "Login SIN token CSRF es rechazado" 403 "POST" "/api/admin/login" -form @{ email = $correo; password = $clave } | Out-Null
Probar "Login con contrasena incorrecta" 401 "POST" "/api/admin/login" -form @{ email = $correo; password = "esta-no-es-la-clave-123" } -csrf (TokenCsrf) | Out-Null
Probar "Login correcto" 200 "POST" "/api/admin/login" -form @{ email = $correo; password = $clave } -csrf (TokenCsrf) | Out-Null
Write-Host "         Cookies de la respuesta (revisa HttpOnly y SameSite=Lax):"
Write-Host ("         " + ($script:ultimosHeaders["Set-Cookie"] -join " | "))
$s = Probar "Ahora la sesion dice 'autenticado'" 200 "GET" "/api/admin/sesion"
Write-Host ("         autenticado = {0}, email = {1}" -f $s.autenticado, $s.email)

Titulo "C. Preparar dos solicitudes para el mismo horario (como clientes, sin sesion)"
$telA = "9" + (Get-Random -Minimum 10000000 -Maximum 99999999)
$telB = "9" + (Get-Random -Minimum 10000000 -Maximum 99999999)
$h = Probar "Horarios libres del $fecha" 200 "GET" "/api/horarios?fecha=$fecha"
$inicio = ($h.horarios | Where-Object { $_.hora -eq $horaPrueba }).inicio
if (-not $inicio) { Write-Host "No hay horario de las $horaPrueba libre el $fecha (revisa la fecha y los datos de prueba)." -ForegroundColor Red; exit 1 }
$a = Probar "Cliente A solicita las $horaPrueba" 201 "POST" "/api/reservas" -json @{ nombreCliente = "Prueba Admin A"; telefono = $telA; inicio = $inicio }
$b = Probar "Cliente B solicita las $horaPrueba" 201 "POST" "/api/reservas" -json @{ nombreCliente = "Prueba Admin B"; telefono = $telB; inicio = $inicio }

Titulo "D. Panel del administrador"
$lista = Probar "Listar solicitudes pendientes" 200 "GET" "/api/admin/reservas?estado=SOLICITADA"
$idA = ($lista | Where-Object { $_.telefono -eq "+56$telA" }).id
$idB = ($lista | Where-Object { $_.telefono -eq "+56$telB" }).id
Write-Host ("         id de A = {0}, id de B = {1}" -f $idA, $idB)
Probar "Confirmar SIN token CSRF es rechazado" 403 "POST" "/api/admin/reservas/$idA/confirmar" | Out-Null
$r = Probar "Confirmar a A" 200 "POST" "/api/admin/reservas/$idA/confirmar" -csrf (TokenCsrf)
Write-Host ("         estado de A = {0}" -f $r.estado)
$rb = Probar "B (mismo horario) quedo rechazada automaticamente" 200 "GET" ("/api/reservas/" + $b.token)
Write-Host ("         estado de B = {0}   (esperado: RECHAZADA)" -f $rb.estado)
if ($rb.estado -eq "RECHAZADA") { $script:ok++ } else { $script:fallos++ ; Write-Host "  FALLA  B deberia estar RECHAZADA" -ForegroundColor Red }

Titulo "E. El horario confirmado ya no se ofrece"
$h2 = Probar "Horarios libres despues de confirmar" 200 "GET" "/api/horarios?fecha=$fecha"
if (($h2.horarios | Where-Object { $_.hora -eq $horaPrueba }) -eq $null) { $script:ok++; Write-Host "  OK     El horario de las $horaPrueba ya no aparece" -ForegroundColor Green } else { $script:fallos++; Write-Host "  FALLA  El horario de las $horaPrueba sigue apareciendo" -ForegroundColor Red }
Probar "Un cliente nuevo ya no puede pedir ese horario" 409 "POST" "/api/reservas" -json @{ nombreCliente = "Prueba Admin C"; telefono = ("9" + (Get-Random -Minimum 10000000 -Maximum 99999999)); inicio = $inicio } | Out-Null

Titulo "F. Cancelar y liberar"
$c = Probar "El administrador cancela a A" 200 "POST" "/api/admin/reservas/$idA/cancelar" -csrf (TokenCsrf)
Write-Host ("         estado = {0}, cancelada por = {1}" -f $c.estado, $c.canceladaPor)
$h3 = Probar "Horarios libres despues de cancelar" 200 "GET" "/api/horarios?fecha=$fecha"
if (($h3.horarios | Where-Object { $_.hora -eq $horaPrueba }) -ne $null) { $script:ok++; Write-Host "  OK     El horario de las $horaPrueba volvio a estar libre" -ForegroundColor Green } else { $script:fallos++; Write-Host "  FALLA  El horario no se libero" -ForegroundColor Red }

Titulo "G. Cerrar sesion"
Probar "Logout SIN token CSRF es rechazado" 403 "POST" "/api/admin/logout" | Out-Null
Probar "Logout correcto" 200 "POST" "/api/admin/logout" -csrf (TokenCsrf) | Out-Null
Probar "Despues del logout el panel vuelve a estar protegido" 401 "GET" "/api/admin/reservas" | Out-Null

Write-Host ""
if ($script:fallos -eq 0) { Write-Host ("TODO BIEN: {0} comprobaciones correctas." -f $script:ok) -ForegroundColor Green }
else { Write-Host ("HAY {0} FALLAS ({1} correctas). Copia las lineas FALLA y mandamelas." -f $script:fallos, $script:ok) -ForegroundColor Red }
