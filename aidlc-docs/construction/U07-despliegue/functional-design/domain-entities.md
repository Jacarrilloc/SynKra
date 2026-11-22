# U07 · Entidades del dominio (Despliegue)

Tabla del módulo `despliegue` (prefijo `dep_`).

| Entidad / tabla | Campos | Reglas |
|---|---|---|
| `Plan` / `dep_plan` | `id`, `pais`, `tipo` (`despliegue` o `reversion`), `origen_id`, `core`, `version_core`, `plugins` (JSON: nombre, versión), `resumen` (JSON), `cambios` (JSON: añadidos, quitados, iguales), `estado` (`pendiente`, `confirmado`, `vencido`), `creado_por`, `creado_en`, `vence_en` | `vence_en = creado_en + 30 min` (Q6) |
| `Seleccion` (no se guarda) | `pais`, `core`, `version_core`, `plugins` | Entrada de la verificación y del plan |
