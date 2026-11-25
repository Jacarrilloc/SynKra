# Tareas candidatas

Trabajo real que apareció durante el loop y quedó **fuera del alcance** de la tarea en curso. No se
hizo. Cada candidata dice de dónde salió, quién la propuso y qué decidió el orquestador.

| # | Candidata | Salió de | Propuesta por | Decisión |
|---|---|---|---|---|
| C-01 | Hacer más estricta la lectura de vuelta de la JVM del daemon: filtrar por el pid que reporta `./gradlew --info` en vez de leer todos los procesos `GradleDaemon` | Revisión de U01-T01, ronda 1 (F-01, AMARILLO) | Revisor | Aceptada como candidata por el orquestador: el criterio se cumple y el revisor confirmó el daemon correcto con `--info`. Se aplica al escribir tareas nuevas que lean la JVM |
| C-02 | Ignorar `.jqwik-database` (o moverla a `build/` con `jqwik.database` en `junit-platform.properties`) | Codificador y revisión de U01-T02, ronda 1 (F-01, AMARILLO) | Codificador; el revisor coincide | Aceptada como candidata por el orquestador: el `.gitignore` es de U01-T01 y quedó fuera del alcance de U01-T02. Propuesta al responsable para sumarla a U01-T03; hasta su visto bueno, no se hace |
