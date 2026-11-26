# Ronda 1 — U03-T04

VEREDICTO: NO-VERDE

Los cuatro criterios pasan con mis propias corridas. Queda un NARANJA en pie: el indicador de salud decide UP o DOWN con un nombre de archivo escrito a mano, copiado de `BaseDeDatos`.

Contexto revisado: worktree `/home/user/wt-m6r-U03-T04`, rama m6r/U03-T04, HEAD `451c4fc0411df9840bf7918045bbf44af2034547`, base 3bf6e7d. El hash de la tarea coincide (`27b52bf8127c5735b506ed38e75a45161023f549`). El diff toca 4 archivos: `SaludBaseDeDatos.java` (nuevo), `application.properties`, `SaludTest.java` (nuevo) y `bitacoras/U03-T04.md`. No hay desborde de alcance: no toca sondas de K8s, métricas, seguridad, PRAGMA, formato de error ni `V1__base.sql`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | SaludTest pasa sin caché | `./gradlew :backend:test --rerun --tests '*SaludTest'` | pasa: BUILD SUCCESSFUL, XML con timestamp de mi corrida (02:07:16Z); tiempos por caso 0.755 s, 0.065 s y 0.039 s (arrancan contextos reales, no se saltaron) |
| 2 | DOWN con base inutilizable, tests ≥ 3, skipped 0 | el `grep -ho ...` del criterio sobre `TEST-*SaludTest.xml` | pasa: `tests="3" skipped="0" failures="0" errors="0"` |
| 3 | Lectura de vuelta con el jar real en 18080 | el bloque del criterio tal cual; jar reconstruido con `bootJar --rerun` (mtime 02:07:44) | pasa: imprime `UP UP` y `ls $D` muestra `synkra.db` |
| 4 | Todo el backend pasa sin caché | `./gradlew :backend:check --rerun` | pasa: BUILD SUCCESSFUL; 7 suites y 31 pruebas, todas con `skipped="0" failures="0" errors="0"` y timestamps frescos |

Lecturas de vuelta adicionales contra el jar real:
- `/actuator/env` respondió 404.
- Después de `rm -rf $D`, `/actuator/health` respondió `code=503` con `{"status":"DOWN","components":{"db":{"status":"DOWN"},...}}`. El caso DOWN se comporta igual fuera de la prueba.
- Después del `kill`, el puerto 18080 dejó de responder (`000`). La aplicación quedó apagada.

Al terminar, el worktree está limpio (`git status --short` sin salida). No toqué `/home/user/wt-m6r-U01-T05`.

## Hallazgos

### F-01 · NARANJA · backend/src/main/java/io/synkra/plataforma/SaludBaseDeDatos.java:24 · Nombre del archivo de la base duplicado como literal en el punto de decisión de la salud
`this.archivo = Path.of(directorio, "synkra.db");` vuelve a escribir a mano el nombre que ya define `BaseDeDatos.urlJdbc` (`"jdbc:sqlite:" + directorio + "/synkra.db"`, BaseDeDatos.java:19). El nombre es visible para el usuario: es el archivo de su volumen, y CA-3 lo lista.
- Ese literal decide UP o DOWN: `if (!Files.isRegularFile(archivo) || ...)` en la línea 29.
- Si alguien cambia el nombre o la ruta en `BaseDeDatos`, la salud queda en DOWN para siempre mientras la base funciona. El caso contrario también puede pasar: mira un archivo que no es el que usa el pool.
- Barrido de clase: es la única copia del nombre en `src/main` fuera de `BaseDeDatos` (`grep -rn "synkra.db" backend/src`). Las apariciones en pruebas (`BaseDeDatosTest:33`, `RutaDatosPorDefectoTest:21`) son aserciones legítimas.
- Arreglo esperado: una sola fuente, por ejemplo un `static Path archivo(String directorio)` en `BaseDeDatos` que usen `urlJdbc` y el indicador.

### F-02 · AMARILLO · backend/src/test/java/io/synkra/plataforma/SaludTest.java:65-69 · El caso DOWN no comprueba `components.db.status`
La prueba solo mira el `status` global (lo que CA-2 pide literalmente). No comprueba que sea `db` quien cae. Cualquier otro indicador en DOWN la haría pasar por la razón equivocada. Conviene añadir `components.db.status = DOWN`.

### F-03 · AMARILLO · SaludBaseDeDatos.java:32-37 · La rama DOWN por excepción de `SELECT 1` no está cubierta
Solo se prueba la rama del archivo ausente. La comprobación `Files.isWritable` tampoco se prueba (y si el proceso corre como root, siempre da verdadero). Es un caso de borde secundario.

### F-04 · AMARILLO · application.properties · La página de descubrimiento `/actuator` responde 200
Lista solo `health` y `health-path`, así que no expone otro endpoint y no rompe el alcance («el resto de endpoints de Actuator queda sin exponer»). Si se quisiera cerrarla, sería con `management.endpoints.web.discovery.enabled=false`. Es una nota, no bloquea.

El `HealthIndicator` propio está justificado en la bitácora, como pedía la nota de la tarea: con la única conexión del pool ya abierta, el indicador por defecto sigue en UP aunque se borre el archivo. El nombre del bean `dbHealthIndicator` reemplaza al de la autoconfiguración de forma correcta.

La evidencia del codificador es coherente con el diff actual. Para el rojo primero dejó salida de los fallos, y mis corridas reproducen todos los resultados en verde.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca1.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca4.log

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/SaludBaseDeDatos.java:24|Nombre del archivo de la base duplicado como literal en el punto de decisión de la salud
INFORME: revisiones/U03-T04/ronda-1.md
```
