# Prueba manual de la etapa 3. Con la app corriendo, desde la carpeta del proyecto:
#   powershell -ExecutionPolicy Bypass -File docs/dev/probar-reservas.ps1
# Usa un telefono distinto en cada ejecucion, asi no choca con el limite de solicitudes pendientes.

$base = "http://localhost:8080"
$telefono = "9" + (Get-Random -Minimum 10000000 -Maximum 99999999)
$fecha = "2026-10-20"

function Llamar($metodo, $ruta, $cuerpo = $null) {
    try {
        if ($cuerpo -ne $null) {
            $json = $cuerpo | ConvertTo-Json
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
            return Invoke-RestMethod -Method $metodo -Uri "$base$ruta" -ContentType "application/json; charset=utf-8" -Body $bytes
        }
        return Invoke-RestMethod -Method $metodo -Uri "$base$ruta"
    } catch {
        $codigo = $_.Exception.Response.StatusCode.value__
        Write-Host "  -> ERROR $codigo : $($_.ErrorDetails.Message)" -ForegroundColor Yellow
        return $null
    }
}

function Titulo($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }

Titulo "1. Horarios libres del $fecha"
$horarios = Llamar "GET" "/api/horarios?fecha=$fecha"
$horarios.horarios | ForEach-Object { Write-Host "  $($_.hora)  $($_.inicio)" }
$h0 = $horarios.horarios[0].inicio
$h1 = $horarios.horarios[1].inicio
$h2 = $horarios.horarios[2].inicio

Titulo "2. Solicitar la primera hora (debe dar 201 y un token)"
$r1 = Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; instagram = "@ana.perez"; inicio = $h0 }
$r1 | Format-List
$token = $r1.token

Titulo "3. Ver la reserva con el token (estado SOLICITADA)"
Llamar "GET" "/api/reservas/$token" | Format-List

Titulo "4. Misma hora otra vez (debe dar 409: solicitud repetida)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; inicio = $h0 } | Out-Null

Titulo "5. Segunda hora distinta (debe funcionar: es la 2.a pendiente)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; inicio = $h1 } | Format-List

Titulo "6. Tercera hora (debe dar 409: maximo de 2 pendientes)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; inicio = $h2 } | Out-Null

Titulo "7. Datos invalidos (debe dar 400 con el detalle por campo)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "1"; telefono = "abc"; inicio = "nada" } | Out-Null

Titulo "8. Hora fuera de la lista, 19:30 (debe dar 409)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; inicio = "2026-10-20T19:30:00-03:00" } | Out-Null

Titulo "9. Token que no existe (debe dar 404)"
Llamar "GET" "/api/reservas/00000000-0000-0000-0000-000000000000" | Out-Null

Titulo "10. Cancelar la primera (debe quedar CANCELADA)"
Llamar "POST" "/api/reservas/$token/cancelar" | Format-List

Titulo "11. Cancelarla de nuevo (debe dar 409)"
Llamar "POST" "/api/reservas/$token/cancelar" | Out-Null

Titulo "12. Ahora la tercera hora si se puede (liberaste un cupo)"
Llamar "POST" "/api/reservas" @{ nombreCliente = "Ana Perez"; telefono = $telefono; inicio = $h2 } | Format-List

Write-Host "`nListo." -ForegroundColor Green
