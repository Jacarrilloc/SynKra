-- Catalogo (U04). Fechas: INTEGER, epoch en milisegundos (C-05). Sin borrados en el MVP (R-C14).
CREATE TABLE cat_core (
    nombre TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE cat_version_core (
    core           TEXT    NOT NULL REFERENCES cat_core (nombre) ON DELETE RESTRICT,
    version        TEXT    NOT NULL,
    imagen         TEXT    NOT NULL,
    registrado_por TEXT    NOT NULL,
    registrado_en  INTEGER NOT NULL,
    PRIMARY KEY (core, version)
);

CREATE TABLE cat_plugin (
    core            TEXT    NOT NULL REFERENCES cat_core (nombre) ON DELETE RESTRICT,
    nombre          TEXT    NOT NULL,
    version         TEXT    NOT NULL,
    core_compatible TEXT    NOT NULL,
    sha256          TEXT    NOT NULL,
    tamano          INTEGER NOT NULL,
    ruta            TEXT    NOT NULL,
    descripcion     TEXT,
    subido_por      TEXT    NOT NULL,
    subido_en       INTEGER NOT NULL,
    PRIMARY KEY (core, nombre, version)
);

CREATE INDEX cat_plugin_sha256 ON cat_plugin (sha256);

CREATE TABLE cat_etiqueta (
    core    TEXT NOT NULL,
    plugin  TEXT NOT NULL,
    version TEXT NOT NULL,
    clave   TEXT NOT NULL,
    valor   TEXT NOT NULL,
    origen  TEXT NOT NULL CHECK (origen IN ('descriptor','subida')),
    PRIMARY KEY (core, plugin, version, clave),
    FOREIGN KEY (core, plugin, version) REFERENCES cat_plugin (core, nombre, version) ON DELETE RESTRICT
);
