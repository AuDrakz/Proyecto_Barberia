-- V1: esquema inicial de la barbería.
-- Regla de oro: una migración que ya se ejecutó NO se edita. Los cambios van en V2, V3...

-- Administrador (tu amigo). Un solo barbero por ahora, así que no hay tabla "barbero".
CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,        -- NUNCA la contraseña en texto plano
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Disponibilidad SEMANAL recurrente (ej: martes de 19:00 a 22:00).
-- Las horas son "hora de pared" local (TIME), no un instante en el tiempo.
CREATE TABLE disponibilidad (
    id          BIGSERIAL PRIMARY KEY,
    dia_semana  SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),  -- 1 = lunes ... 7 = domingo (ISO)
    hora_inicio TIME     NOT NULL,
    hora_fin    TIME     NOT NULL,
    CONSTRAINT ck_disponibilidad_rango CHECK (hora_fin > hora_inicio)
);

-- Excepciones PUNTUALES a la disponibilidad ("este martes no puedo").
-- Si hora_inicio y hora_fin son NULL, se bloquea el día completo.
CREATE TABLE bloqueo (
    id          BIGSERIAL PRIMARY KEY,
    fecha       DATE         NOT NULL,
    hora_inicio TIME,
    hora_fin    TIME,
    motivo      VARCHAR(200),
    CONSTRAINT ck_bloqueo_horas_juntas CHECK ((hora_inicio IS NULL) = (hora_fin IS NULL)),
    CONSTRAINT ck_bloqueo_rango        CHECK (hora_fin IS NULL OR hora_fin > hora_inicio)
);

-- Reservas. El cliente no tiene cuenta: deja nombre y teléfono.
CREATE TABLE reserva (
    id                 BIGSERIAL PRIMARY KEY,
    nombre_cliente     VARCHAR(100) NOT NULL,
    telefono           VARCHAR(20)  NOT NULL,
    instagram          VARCHAR(30),                         -- opcional: ayuda a tu amigo a reconocer al cliente
    inicio             TIMESTAMPTZ  NOT NULL,               -- instante exacto; cada hora dura 1 hora
    estado             VARCHAR(20)  NOT NULL DEFAULT 'SOLICITADA',
    cancelada_por      VARCHAR(10),                         -- CLIENTE o ADMIN, solo si está CANCELADA
    token_cancelacion  UUID         NOT NULL DEFAULT gen_random_uuid(),  -- para el enlace del cliente (evita IDOR)
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('SOLICITADA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT ck_reserva_cancelada_por CHECK (cancelada_por IN ('CLIENTE', 'ADMIN')),
    -- "cancelada_por" tiene valor si y solo si el estado es CANCELADA.
    CONSTRAINT ck_reserva_cancelacion_coherente CHECK ((estado = 'CANCELADA') = (cancelada_por IS NOT NULL)),
    CONSTRAINT uq_reserva_token UNIQUE (token_cancelacion)
);

-- LA PIEZA CLAVE: la base de datos impide dos reservas CONFIRMADAS a la misma hora.
-- Es un índice único PARCIAL: solo aplica a las filas confirmadas, así que puede haber
-- varias SOLICITADAS para el mismo horario y las rechazadas/canceladas no estorban.
CREATE UNIQUE INDEX ux_reserva_confirmada_inicio
    ON reserva (inicio)
    WHERE estado = 'CONFIRMADA';

-- Para el panel del administrador (listar solicitudes pendientes, próximas citas, etc.).
CREATE INDEX ix_reserva_estado_inicio ON reserva (estado, inicio);
