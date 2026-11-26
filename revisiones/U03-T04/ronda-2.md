# Ronda 2 — U03-T04

VEREDICTO: VERDE

Volví a correr yo los cuatro criterios y los cuatro pasan. F-01, el NARANJA de la ronda 1, está corregido. F-02 también. Para F-03 y F-04 el codificador dio respuestas razonadas, y las acepto según el arbitraje. No queda ningún ROJO ni NARANJA. Hay un AMARILLO nuevo y menor, que no bloquea.

Contexto que revisé:
- **Worktree y rama:** `/home/user/wt-m6r-U03-T04`, rama m6r/U03-T04, HEAD `07ca555`, base 3bf6e7d. Los cambios de esta ronda son `6437d01..07ca555`.
- **Hash de la tarea:** `git hash-object tareas/U03-T04-salud.md` da `27b52bf8127c5735b506ed38e75a45161023f549`, el mismo del despacho.
- **Archivos de código del diff:** `BaseDeDatos.java`, `SaludBaseDeDatos.java` (nuevo), `application.properties` y `SaludTest.java` (nuevo).
- **Archivos de documentación del diff:** la bitácora y los archivos de `revisiones/`.
- **Alcance:** no hay desborde. El diff no toca sondas, métricas, seguridad, PRAGMA, formato de error ni `V1__base.sql`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | SaludTest pasa sin caché | `./gradlew :backend:test --rerun --tests '*SaludTest'` | pasa: BUILD SUCCESSFUL. El XML trae el timestamp de mi corrida (2026-11-26T02:10:07Z). Los casos tardan 0.803 s, 0.073 s y 0.04 s: arrancan contextos reales, no se saltaron |
| 2 | DOWN con la base inutilizable, tests ≥ 3, skipped 0 | el `grep -ho ...` del criterio sobre `TEST-*SaludTest.xml` | pasa: `tests="3" skipped="0" failures="0" errors="0"` |
| 3 | Lectura de vuelta con el jar real en 18080 | `bootJar --rerun` (jar con mtime 02:10:21), luego el bloque del criterio tal cual | pasa: imprime `UP UP` y `ls $D` muestra `synkra.db` |
| 4 | Todo el backend pasa sin caché | `./gradlew :backend:check --rerun` | pasa: BUILD SUCCESSFUL. Son 7 suites y 31 pruebas, todas con `skipped="0" failures="0" errors="0"` y timestamps de 02:10:42Z a 02:10:47Z |

Lecturas de vuelta adicionales contra el jar real, en una corrida aparte:
- `/actuator/env` respondió 404.
- Después de `rm -rf $D`, `/actuator/health` respondió `code=503` con `{"status":"DOWN","components":{"db":{"status":"DOWN"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"},"ssl":{"status":"UP"}}}`.

En esa corrida aparte el `ls $D` salió vacío porque yo había borrado `$D` para probar el DOWN. Por eso repetí CA-3 literal, y ahí sí aparece `synkra.db`.

Estado al terminar:
- **Aplicación:** apagada. El puerto 18080 responde `000` después del `kill` y no queda ningún proceso de `backend-0.1.0.jar`.
- **Worktree:** limpio, `git status --short` sin salida.

## Verificación de las respuestas de la ronda 1
- **F-01 (NARANJA), corregido.** `BaseDeDatos.archivo(directorio)` (BaseDeDatos.java:19-21) es ahora la única fuente de la ruta.
  - `urlJdbc` usa `archivo()`, y el constructor de `SaludBaseDeDatos` también (línea 24).
  - `grep -rn "synkra.db" backend/src/main` deja el literal solo en BaseDeDatos.java:20, más el javadoc y el mensaje de F-05.
  - `RutaDatosPorDefectoTest` sigue afirmando `jdbc:sqlite:/data/synkra.db` y pasa, así que la normalización de `Path` no cambió la URL.
- **F-02 (AMARILLO), corregido.** SaludTest.java:69-71 afirma `components.db.status = DOWN` en el caso destructivo, y pasa con mi corrida.
- **F-03 (AMARILLO), aceptada la respuesta razonada.** Es un caso de borde secundario. La causa real que pide CA-2, el archivo ausente, sí está cubierta. Una pequeña objeción: la rama de excepción se podría alcanzar con un DataSource falso en una prueba unitaria, sin contexto. No bloquea.
- **F-04 (AMARILLO), aceptada la respuesta razonada.** `/actuator` solo lista `health`, y cerrar el descubrimiento no está en la tarea.

## Hallazgos
### F-05 · AMARILLO · backend/src/main/java/io/synkra/plataforma/SaludBaseDeDatos.java:30 · Queda una copia del nombre en el mensaje del detalle DOWN
`withDetail("error", "synkra.db no existe o no es escribible")` repite a mano el nombre que ahora vive solo en `BaseDeDatos.archivo`. No decide nada, porque la decisión usa `archivo`. Pero si cambia el nombre, el mensaje mentiría.
- **Arreglo posible:** construir el mensaje con `archivo.getFileName()`, o con `archivo` para dar la ruta completa.
- **Por qué no lo detecté en la ronda 1:** el barrido de clase de F-01 solo miró los puntos de decisión. No bloquea.
- **Visibilidad actual:** hoy el detalle no se ve en la respuesta HTTP, porque `show-details` está en su valor por defecto.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2-ca1.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2-jar.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/r2-ca4.log

Nota: mis herramientas son de solo lectura y no escribí `revisiones/U03-T04/ronda-2.md`. El texto de este informe es el contenido para ese archivo.

```
VEREDICTO: VERDE
AMARILLO|backend/src/main/java/io/synkra/plataforma/SaludBaseDeDatos.java:30|Queda una copia del nombre en el mensaje del detalle DOWN
INFORME: revisiones/U03-T04/ronda-2.md
```
