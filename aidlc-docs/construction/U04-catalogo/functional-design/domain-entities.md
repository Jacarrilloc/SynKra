# U04 · Entidades del dominio (Catálogo)

Tablas propias del módulo `catalogo` (prefijo `cat_`); ningún otro módulo las lee (D14). Los
demás usan la interfaz pública `CatalogoApi`, que además implementa `Catalogo` de U01.

| Entidad / tabla | Campos | Reglas |
|---|---|---|
| `Core` / `cat_core` | `nombre` (PK) | Se crea con la primera versión; nunca se borra (Q3) |
| `VersionCore` / `cat_version_core` | `core`, `version`, `imagen` (`registro/repo@sha256:…`), `registrado_por`, `registrado_en` | PK (`core`, `version`); inmutable |
| `Plugin` / `cat_plugin` | `core`, `nombre`, `version`, `core_compatible` (texto del rango), `sha256`, `tamano`, `ruta`, `descripcion`, `subido_por`, `subido_en` | PK (`core`, `nombre`, `version`); inmutable (Q2); `ruta` dentro de `/data/artefactos/<core>/<nombre>/<version>.jar` |
| `Etiqueta` / `cat_etiqueta` | `core`, `plugin`, `version`, `clave`, `valor`, `origen` (`descriptor` o `subida`) | PK (`core`, `plugin`, `version`, `clave`) |

Índice por `sha256` para la descarga del init container (H-15).
