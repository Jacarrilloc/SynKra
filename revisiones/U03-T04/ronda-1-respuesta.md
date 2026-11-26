F-01: Corregido: `BaseDeDatos.archivo(directorio)` es la única fuente de la ruta; `urlJdbc` y `SaludBaseDeDatos` la usan (grep de "synkra.db" en src/main solo da BaseDeDatos).
F-02: Corregido: el caso DOWN de SaludTest ahora aserta también `components.db.status` = DOWN.
F-03: No es un defecto: es un borde secundario; la rama de excepción de SELECT 1 no es alcanzable sin falsear el DataSource, y isWritable da siempre verdadero como root. Cubierta la causa real (archivo ausente), lo que pide CA-2.
F-04: No es un defecto: `/actuator` solo lista health; no expone otro endpoint y cerrar el descubrimiento no está en la tarea.
