-- Datos SOLO para probar a mano en desarrollo. No son una migración: no se versionan en Flyway.
-- Ejecutar UNA vez. Para empezar de cero: docker compose down -v  (borra los datos) y volver a levantar.
-- Las fechas suponen que "hoy" es 13-oct-2026; si ya pasó esa semana, cambia las fechas por otras futuras
-- dentro de los próximos 30 días.

-- Se atiende de martes a viernes, de 19:00 a 22:00 (dia_semana: 1 = lunes ... 7 = domingo)
INSERT INTO disponibilidad (dia_semana, hora_inicio, hora_fin) VALUES
    (2, '19:00', '22:00'),
    (3, '19:00', '22:00'),
    (4, '19:00', '22:00'),
    (5, '19:00', '22:00');

-- Mie 21-oct: bloqueo parcial de 20:00 a 21:00. Jue 22-oct: dia completo bloqueado.
INSERT INTO bloqueo (fecha, hora_inicio, hora_fin, motivo) VALUES
    ('2026-10-21', '20:00', '21:00', 'Prueba: bloqueo parcial'),
    ('2026-10-22', NULL, NULL, 'Prueba: dia completo');

-- Vie 23-oct a las 19:00 (hora de Chile, UTC-3): reserva confirmada.
INSERT INTO reserva (nombre_cliente, telefono, inicio, estado) VALUES
    ('Cliente de prueba', '+56900000000', '2026-10-23 19:00:00-03', 'CONFIRMADA');
