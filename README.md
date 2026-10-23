# Barbería Reservas

Sistema de reserva de horas para una barbería casera. Los clientes solicitan una hora desde el celular
y el barbero confirma o rechaza cada solicitud.

> Estado: en desarrollo (etapa 1: base del proyecto).

## Requisitos

- Java 21 
- Docker y Docker Compose

## Cómo ejecutarlo en local

```bash
   ## Cómo ejecutarlo en local

   1. Levantar la base de datos: `docker compose up -d`
   2. Definir las credenciales del administrador (se crea al primer arranque, solo si no existe ninguno):
```powershell
      $env:ADMIN_EMAIL = "admin@ejemplo.com"
      $env:ADMIN_PASSWORD = Read-Host "Contraseña (mínimo 12 caracteres)"
```
   3. Arrancar la aplicación: `./mvnw spring-boot:run`

## Decisiones de diseño

Ver [docs/decisiones.md](docs/decisiones.md).

| Endpoint | Acceso | Qué hace |
|---|---|---|
| `GET /api/horarios?fecha=AAAA-MM-DD` | Público | Horarios libres de un día |
| `POST /api/reservas` | Público | Solicitar una hora |
| `GET /api/reservas/{token}` | Público (con el token) | Ver una solicitud |
| `POST /api/reservas/{token}/cancelar` | Público (con el token) | Cancelar |
| `/api/admin/**` | Administrador | Login, panel de reservas, disponibilidad y bloqueos |
| `GET/POST /api/admin/disponibilidad`, `DELETE .../{id}` | Administrador | Tramos semanales |
| `GET/POST /api/admin/bloqueos`, `DELETE .../{id}` | Administrador | Bloqueos puntuales |

## Roadmap

- [x] Etapa 1: base del proyecto y esquema de la base de datos
- [x] Etapa 2: disponibilidad, bloqueos y horarios libres
- [x] Etapa 3: solicitud de hora y cancelación con enlace
- [x] Etapa 4: panel del administrador (login, confirmar y rechazar)
- [x] Etapa 5: frontend para celular
- [ ] Etapa 6: CI, despliegue y documentación final
- [ ] v2: reagendar
