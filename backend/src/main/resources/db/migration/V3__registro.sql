-- Registro (U06). Fechas: INTEGER, epoch en milisegundos (C-05). Sin borrados en el MVP.
-- El id de reg_despliegue lo da quien registra (el id del plan de U07): sin autoincremento.
CREATE TABLE reg_despliegue (
    id             INTEGER PRIMARY KEY,
    pais           TEXT    NOT NULL CHECK (length(pais) = 2 AND pais GLOB '[a-z][a-z]'),
    tipo           TEXT    NOT NULL CHECK (tipo IN ('despliegue','reversion')),
    origen_id      INTEGER REFERENCES reg_despliegue (id) ON DELETE RESTRICT,
    core           TEXT    NOT NULL,
    version_core   TEXT    NOT NULL,
    imagen         TEXT    NOT NULL,
    creado_por     TEXT    NOT NULL,
    confirmado_por TEXT    NOT NULL,
    confirmado_en  INTEGER NOT NULL,
    estado         TEXT    NOT NULL CHECK (estado IN ('aplicando','listo','fallido')),
    terminado_en   INTEGER,
    veredicto      TEXT    NOT NULL CHECK (json_valid(veredicto)),
    CHECK ((tipo = 'reversion') = (origen_id IS NOT NULL)),
    CHECK ((estado = 'aplicando') = (terminado_en IS NULL))
);

CREATE INDEX reg_despliegue_pais ON reg_despliegue (pais, confirmado_en);

CREATE TABLE reg_plugin (
    despliegue_id INTEGER NOT NULL REFERENCES reg_despliegue (id) ON DELETE RESTRICT,
    nombre        TEXT    NOT NULL,
    version       TEXT    NOT NULL,
    sha256        TEXT    NOT NULL,
    etiquetas     TEXT    NOT NULL CHECK (json_valid(etiquetas)),
    PRIMARY KEY (despliegue_id, nombre)
);

-- eventos es texto (lineas), no JSON: truncarlo a 64 KB romperia un JSON.
CREATE TABLE reg_contexto (
    despliegue_id INTEGER PRIMARY KEY REFERENCES reg_despliegue (id) ON DELETE RESTRICT,
    log_core      TEXT NOT NULL,
    log_init      TEXT NOT NULL,
    eventos       TEXT NOT NULL
);
