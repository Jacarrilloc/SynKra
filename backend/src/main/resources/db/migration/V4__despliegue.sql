-- Despliegue (U07). Fechas: INTEGER, epoch en milisegundos (C-05). La lista de países la aplica PaisPermitido.
-- origen_id apunta a reg_despliegue (módulo registro, D14): sin clave foránea a propósito.
-- Los 30 minutos de vigencia viven en el código (Plan.VIGENCIA), no en el esquema.
CREATE TABLE dep_plan (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    pais         TEXT    NOT NULL CHECK (length(pais) = 2 AND pais GLOB '[a-z][a-z]'),
    tipo         TEXT    NOT NULL CHECK (tipo IN ('despliegue', 'reversion')),
    origen_id    INTEGER,
    core         TEXT    NOT NULL,
    version_core TEXT    NOT NULL,
    plugins      TEXT    NOT NULL CHECK (json_valid(plugins)),
    resumen      TEXT    NOT NULL CHECK (json_valid(resumen)),
    cambios      TEXT    NOT NULL CHECK (json_valid(cambios)),
    estado       TEXT    NOT NULL CHECK (estado IN ('pendiente', 'confirmado', 'vencido')),
    creado_por   TEXT    NOT NULL,
    creado_en    INTEGER NOT NULL,
    vence_en     INTEGER NOT NULL,
    CHECK ((tipo = 'reversion') = (origen_id IS NOT NULL)),
    CHECK (vence_en > creado_en)
);

CREATE INDEX dep_plan_pais_estado ON dep_plan (pais, estado);
