-- Datos SOLO para probar a mano en desarrollo. No son una migracion: no se versionan en Flyway.
-- Ejecutar UNA vez, despues de arrancar la aplicacion al menos una vez (Flyway crea las tablas).
-- Para empezar de cero: docker compose down -v  (borra los datos) y volver a levantar.
-- Las fechas son relativas a hoy: siempre quedan dentro de la ventana de reservas.

-- Se atiende de martes a viernes, de 19:00 a 22:00 (dia_semana: 1 = lunes ... 7 = domingo)
INSERT INTO disponibilidad (dia_semana, hora_inicio, hora_fin) VALUES
    (2, '19:00', '22:00'),
    (3, '19:00', '22:00'),
    (4, '19:00', '22:00'),
    (5, '19:00', '22:00');

-- Proximo miercoles, jueves y viernes a partir de pasado manana (la anticipacion minima es de 1 dia).
CREATE TEMP TABLE fechas AS
SELECT
    d + ((3 - extract(isodow FROM d)::int + 7) % 7) AS miercoles,
    d + ((4 - extract(isodow FROM d)::int + 7) % 7) AS jueves,
    d + ((5 - extract(isodow FROM d)::int + 7) % 7) AS viernes
FROM (SELECT current_date + 2 AS d) AS base;

-- Miercoles: bloqueo parcial de 20:00 a 21:00. Jueves: dia completo bloqueado.
INSERT INTO bloqueo (fecha, hora_inicio, hora_fin, motivo)
SELECT miercoles, TIME '20:00', TIME '21:00', 'Prueba: bloqueo parcial' FROM fechas
UNION ALL
SELECT jueves, NULL::time, NULL::time, 'Prueba: dia completo' FROM fechas;

-- Viernes a las 19:00 (hora de Chile): reserva confirmada.
INSERT INTO reserva (nombre_cliente, telefono, inicio, estado)
SELECT 'Cliente de prueba', '+56900000000',
       (viernes + TIME '19:00') AT TIME ZONE 'America/Santiago', 'CONFIRMADA'
FROM fechas;

DROP TABLE fechas;