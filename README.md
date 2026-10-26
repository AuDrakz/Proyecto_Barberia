# Barbería Reservas

Sistema de reserva de horas para una barbería casera. Los clientes solicitan una hora desde el celular
y el barbero confirma o rechaza cada solicitud.

> Estado: producto funcional en local; falta dejarlo listo para publicar (etapa 6).

## Requisitos

- Java 21 
- Docker y Docker Compose

## Cómo ejecutarlo en local

Con Docker Desktop abierto, desde la carpeta del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File docs/dev/arrancar.ps1
```

Levanta la base de datos y la aplicación. La primera vez te pide el correo y la contraseña del administrador
(mínimo 12 caracteres). Luego abre:

- Clientes: http://localhost:8080
- Administrador: http://localhost:8080/admin.html

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
- [x] Etapa 4: login del administrador y panel para confirmar, rechazar y cancelar
- [x] Etapa 4b: disponibilidad y bloqueos desde la API
- [x] Etapa 5a: páginas del cliente
- [x] Etapa 5b: panel del administrador
- [ ] Etapa 6: Docker, integración continua, seguridad de producción y despliegue
- [ ] v2: reagendar
