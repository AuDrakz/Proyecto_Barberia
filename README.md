# Barbería Reservas

Sistema de reserva de horas para una barbería casera. Los clientes solicitan una hora desde el celular
y el barbero confirma o rechaza cada solicitud.

> Estado: en desarrollo (etapa 1: base del proyecto).

## Requisitos

- Java 21 
- Docker y Docker Compose

## Cómo ejecutarlo en local

```bash
# 1. Levantar la base de datos
docker compose up -d

# 2. Ejecutar la aplicación (Linux/macOS)
./mvnw spring-boot:run
# En Windows: mvnw.cmd spring-boot:run
```

Al iniciar, Flyway crea las tablas automáticamente.

## Decisiones de diseño

Ver [docs/decisiones.md](docs/decisiones.md).

## Roadmap

- [x] Etapa 1: base del proyecto y esquema de la base de datos
- [x] Etapa 2: disponibilidad, bloqueos y horarios libres
- [x] Etapa 3: solicitud de hora y cancelación con enlace
- [ ] Etapa 4: panel del administrador (login, confirmar y rechazar)
- [ ] Etapa 5: frontend para celular
- [ ] Etapa 6: CI, despliegue y documentación final
- [ ] v2: reagendar
