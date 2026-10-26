# Arranca todo el proyecto: la base de datos (Docker) y la aplicacion.
# Uso, desde la carpeta del proyecto:
#   powershell -ExecutionPolicy Bypass -File docs/dev/arrancar.ps1

if (-not (Test-Path "pom.xml")) {
    Write-Host "Ejecuta este script desde la carpeta del proyecto (la que tiene el archivo pom.xml)." -ForegroundColor Red
    exit 1
}

# 1) Docker en el PATH de esta ventana (solo si hace falta)
$carpetaDocker = Join-Path $env:LOCALAPPDATA "Programs\DockerDesktop\resources\bin"
if (-not (Get-Command docker -ErrorAction SilentlyContinue) -and (Test-Path $carpetaDocker)) {
    $env:Path += ";$carpetaDocker"
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "No se encontro el comando docker. Instala Docker Desktop o revisa el PATH." -ForegroundColor Red
    exit 1
}

# 2) Docker Desktop debe estar abierto y con el motor corriendo
$null = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop no esta corriendo. Abrelo, espera a que abajo a la izquierda diga 'Engine running' y vuelve a ejecutar este script." -ForegroundColor Yellow
    exit 1
}

# 3) Levantar la base de datos y esperar a que este lista
Write-Host "Levantando la base de datos..." -ForegroundColor Cyan
docker compose up -d
$lista = $false
for ($i = 0; $i -lt 30; $i++) {
    $estado = (docker compose ps 2>&1) -join "`n"
    if ($estado -match "\(healthy\)") { $lista = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $lista) {
    Write-Host "La base de datos no quedo lista a tiempo. Revisa con: docker compose ps" -ForegroundColor Red
    exit 1
}
Write-Host "Base de datos lista." -ForegroundColor Green

# 4) Credenciales del administrador: solo se usan la PRIMERA vez (cuando todavia no existe ningun administrador).
#    Si ya existe, puedes dejarlas vacias (Enter). Nada de esto se guarda en archivos.
if (-not $env:ADMIN_EMAIL) {
    $correo = Read-Host "Correo del administrador (Enter si ya existe)"
    if ($correo) { $env:ADMIN_EMAIL = $correo }
}
if (-not $env:ADMIN_PASSWORD) {
    $clave = Read-Host "Contrasena del administrador (minimo 12 caracteres; Enter si ya existe)" -AsSecureString
    $plana = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave))
    if ($plana) { $env:ADMIN_PASSWORD = $plana }
}

# 5) Arrancar la aplicacion
Write-Host ""
Write-Host "Cuando veas 'Started BarberiaReservasApplication', abre:" -ForegroundColor Cyan
Write-Host "  Clientes:       http://localhost:8080"
Write-Host "  Administrador:  http://localhost:8080/admin.html"
Write-Host "Para detener la aplicacion: Ctrl+C (y responde S)." -ForegroundColor Cyan
Write-Host ""
& .\mvnw.cmd spring-boot:run
