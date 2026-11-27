# Ronda 2 — U02-T05

VEREDICTO: VERDE

Corrí yo los cuatro criterios y los cuatro pasan. También corrí el mutante de CA-2 y un segundo mutante propio, para ver si las aserciones nuevas de F-01 muerden por sí solas. Los cuatro hallazgos de la ronda 1 están cerrados. Quedan dos AMARILLO nuevos, que no bloquean.

Comprobaciones previas:
- **Hash de la tarea:** `f95311817e9138f94cf701f894d68d17d2519d5c`, coincide (`git hash-object`).
- **HEAD y diff:** HEAD es `ad0948a`. Diff revisado: `git diff ff3f184...HEAD`.
- **Entorno:** HOME y `-Duser.home` en `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/home`, con `GRADLE_USER_HOME=/root/.gradle`. No usé kubectl ni el clúster. No toqué `/home/user/wt-m6r-U05-T05`.
- **Worktree:** al terminar, `git status --short` está vacío. Solo quedan ignorados `.gradle/`, `build/` y `verificador/.jqwik-database`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La suite concuerda con el arranque | `./gradlew :core:test --rerun --tests '*ConcordanciaSuiteTest' -i` y grep del XML | pasa: `BUILD SUCCESSFUL in 10s`, `tests="10" skipped="0" failures="0" errors="0"` (9 ejecutables más 1 de conteo). Ejecutados: S1, S2, S3, S4, S6, S7, S8, S9, S10. No ejecutables: S5, S11 y S12, con su razón. Cada caso tarda entre 0,29 y 3,86 s, así que no hubo saltos encubiertos. |
| 2 | La prueba muerde | Copia con `git archive HEAD` en `rev2/copia`. En `ArranquePlugins.java:94` cambié `if (veredicto.global() == Veredicto.Global.RECHAZADA) {` por `if (false) {` y corrí CA-1 | pasa: `10 tests completed, 6 failed`, XML `failures="6" errors="0"`. S4, S7 y S10 fallan en la línea 131 («el contexto debía no arrancar»). S6, S8 y S9 fallan en la 133 («debía terminar en ArranqueRechazado y terminó en BeanCreationException…»). Todos son `AssertionFailedError`, no fallos de compilación. |
| 3 | El resto del build sigue pasando | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | pasa: `BUILD SUCCESSFUL in 48s`. Hay 17 XML (core 4, plugin-api 1, verificador 12) y todos tienen `skipped="0" failures="0" errors="0"`. |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat ff3f184...HEAD` | pasa: `ad0948a`, 7 archivos con 1423 inserciones. El diff solo contiene:<br>- la bitácora;<br>- `core/build.gradle.kts`, con solo `testImplementation(testFixtures(project(":verificador")))`;<br>- `ConcordanciaSuiteTest.java`;<br>- `revisiones/U02-T05/` (informe, respuesta y evidencia).<br>Un grep de `ArranquePlugins.java`, `verificador/`, `plugin-api/` y `backend/` sobre `--name-only` no encuentra nada. |

## Estado de los hallazgos de la ronda 1
### F-01 · NARANJA · cerrado
La línea 133 ahora exige `ArranqueRechazado` en la cadena de causas (`conCausa`). Con el mutante de CA-2, S8 y S9 pasaban en la ronda 1 y ahora fallan por aserción en la 133.

La segunda mitad del hallazgo era la línea que liga `descriptor.jar` con el código y el campo (líneas 148-151). La comprobé con un mutante B propio, en la copia `rev2/copiaB`. En `ArranquePlugins.java:152` cambié `jar.getFileName()` por `"?"`, de modo que el WARN del lector deja de nombrar el JAR. Resultado: `10 tests completed, 2 failed`, con S8 y S9 fallando en `ConcordanciaSuiteTest.java:149`. Esa aserción muerde por sí sola. No queda ninguna aserción que se cumpla con una caída cualquiera.

### F-02 · NARANJA · cerrado
La transcripción nueva está en `revisiones/U02-T05/evidencia/ronda-2-ca2-mutante.log`. Le quité las líneas en blanco a mi `rev2/ca2.log` y normalicé solo el puerto del proxy, la ruta de la copia y los segundos. El `diff` contra el archivo del codificador da 0: es una salida real del diff actual.

El XML de evidencia trae `tests="10" failures="6" errors="0"`, igual que el mío. La sección vieja de la bitácora quedó marcada como REEMPLAZADA (línea 28) sin borrarla, y eso preserva la historia.

### F-03 · AMARILLO · cerrado
`ronda-1-respuesta.md` ahora responde de verdad, hallazgo por hallazgo.

### F-04 · AMARILLO · cerrado
Ahora hay una sola sección CA-4 vigente, con sha y `--stat`. Lo comprobé con `git diff --stat ff3f184...131e10d`, que reproduce exactamente lo pegado (7 archivos, 1374 inserciones). El sha es el del último commit de evidencia, no el de HEAD, y la bitácora lo explica: los commits posteriores solo tocan la bitácora y la respuesta. Lo acepto. El sha de HEAD (`ad0948a`) lo anoto yo arriba.

## Hallazgos nuevos
### F-05 · AMARILLO · bitacoras/U02-T05.md (Verde CA-1 y CA-3, ronda 2) · Transcripciones referidas a un scratchpad, por nombre relativo
La bitácora remite a «scratchpad `r2-ca1.log`» y «scratchpad `r2-ca3.log`». Son nombres relativos en un directorio que no es durable y que además es el scratchpad compartido de la sesión (`/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/`). La evidencia de CA-2 sí se versionó en `revisiones/U02-T05/evidencia/`; la de CA-1 y CA-3 no. No bloquea, porque los resultados resumidos coinciden con mis ejecuciones. Pero si ese directorio se limpia, esas afirmaciones se quedan sin fuente. Sugerencia: dar la ruta absoluta o versionar esas transcripciones junto a la de CA-2.

### F-06 · AMARILLO · core/src/test/java/io/synkra/core/ConcordanciaSuiteTest.java:115-116 y 154 · El nombre `kyc-co` y la versión `1.0.0` repetidos como literales
El JAR del descriptor, la línea del ConfigMap y la aserción `"PLUGIN_AUSENTE plugin=kyc-co"` repiten el mismo literal. Si alguien cambia uno, la aserción de la línea 154 falla con un mensaje engañoso. Es código de prueba y el usuario no ve ese valor, así que no es NARANJA según la invariante 8. Bastaría una constante local.

Observación sin severidad: la decisión 2 dice que en el rechazo del lector «el motivo nombra el descriptor». La prueba lo cumple con la línea WARN del lector (149). La aserción extra `PLUGIN_AUSENTE plugin=kyc-co` fija además el mecanismo de rechazo actual de `ArranquePlugins`. Es más estricta que el criterio, no menos, así que no la marco.

## Tareas candidatas (defectos reales fuera de alcance)
Ya están registradas como C-84, según el arbitraje. Confirmo que siguen vigentes:
- **S5 contra el arranque**, con un core compilado en otra versión.
- **Texto `remiteA` de S12 en `SuiteCombinaciones`.** Sigue diciendo «prueba de plugin no activado pendiente en U02», como se ve en mi salida de CA-1.
- **NPE en `ArranquePlugins` al cargar un plugin sin JAR.** Bajo el mutante se sigue viendo una `BeanCreationException` con una NPE dentro, en vez de un error con nombre.

No hay candidatas nuevas.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/ca2.log (y el XML del mutante en /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/copia/core/build/test-results/test/)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/ca2b.log (mutante B, en la copia /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/copiaB)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/ca3.log y /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/rev2/ca3.txt

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U02-T05.md (CA-1/CA-3 ronda 2)|Transcripciones referidas a un scratchpad no durable con nombre relativo
AMARILLO|core/src/test/java/io/synkra/core/ConcordanciaSuiteTest.java:115-116,154|Literal kyc-co/1.0.0 repetido en lugar de una constante
INFORME: revisiones/U02-T05/ronda-2.md
```
