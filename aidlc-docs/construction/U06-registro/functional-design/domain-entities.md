# U06 · Entidades del dominio (Registro)

Tablas del módulo `registro` (prefijo `reg_`). U07 escribe por la interfaz `RegistroApi`; nadie
lee las tablas directamente (D14).

| Entidad / tabla | Campos | Reglas |
|---|---|---|
| `Despliegue` / `reg_despliegue` | `id`, `pais`, `tipo` (`despliegue` o `reversion`), `origen_id` (si es reversión), `core`, `version_core`, `imagen`, `creado_por`, `confirmado_por`, `confirmado_en`, `estado` (`aplicando`, `listo`, `fallido`), `terminado_en`, `veredicto` (JSON) | Solo existe tras una confirmación (un plan sin confirmar no está aquí, H-17) |
| `PluginDesplegado` / `reg_plugin` | `despliegue_id`, `nombre`, `version`, `sha256`, `etiquetas` (JSON) | |
| `ContextoFallo` / `reg_contexto` | `despliegue_id`, `log_core`, `log_init`, `eventos` (JSON) | Cada texto truncado a 64 KB; el log del core, a sus últimas 200 líneas (Q5) |
| `EstadoPais` (no se guarda) | `pais`, `core`, `version_core`, `imagen`, `plugins` (nombre, versión, hash), `estado_k8s`, `concuerda`, `ultimo_despliegue_id` | Se calcula en cada consulta |
