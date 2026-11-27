# Ronda 2 — U05-T05

VEREDICTO: NO-VERDE

Comprobé el hash de la tarea con `git hash-object` y da 65a449cecd38706d8a1f17df2d817a5e51150f09, que coincide. Revisé el HEAD `efcabf1` contra la base `0a52e2e`. Gradle corrió con `HOME`, `KUBECONFIG` y `-Duser.home` en mi scratchpad, y con `GRADLE_USER_HOME=/root/.gradle`. No usé kubectl ni ningún clúster, solo el servidor simulado.

Los tres criterios pasan y F-01 a F-05 quedan cerrados con prueba. F-06 queda cerrado solo a medias. El orden por `Instant` sí está probado. La mitad «los eventos sin marca van al final» no está probada: el servidor simulado pone `creationTimestamp` en cada objeto que se crea, así que el evento «sin marca» de la prueba sí tiene marca. Un mutante con `nullsFirst` sobrevive (F-08).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `EstadoYContextoTest`: al menos 8 pruebas, sin saltos ni fallos | `./gradlew :backend:test --rerun --tests '*EstadoYContextoTest'` + grep del XML | **pasa**: `BUILD SUCCESSFUL in 6s`, `tests="20" skipped="0" failures="0" errors="0"`, XML de mi corrida (`2026-11-27T20:55:06Z`). Cada prueba tarda entre 0,037 y 1,6 s, con el servidor simulado real detrás. |
| 2 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | **pasa**: `BUILD SUCCESSFUL in 53s`, 28 XML, todos con `skipped="0" failures="0" errors="0"` (marcas de tiempo de 20:55:43Z a 20:56:33Z) |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 0a52e2e...HEAD` | **pasa**: `efcabf1`, 13 archivos, +966. Todos están en `backend/src/{main,test}/java/io/synkra/kubernetes/**`, `bitacoras/U05-T05.md` o `revisiones/U05-T05/`. El código de `main` no añade ninguna llamada que escriba. |

## Estado de los hallazgos de la ronda 1
| Id | Estado | Evidencia mía |
|---|---|---|
| F-01 | **cerrado** | `siembraPod(POD, "Pending", null, null)`: el pod está en `phase: Pending` y no tiene estados de contenedor. `podPendienteEsEnCurso...` pasa en mi corrida. |
| F-02 | **cerrado** | Hay pruebas para un 500 en el Deployment, en el list de pods y en los eventos, para un log con 500 y con 403 (se comprueba `getCode()`), y para una excepción ajena con Mockito que no se envuelve. Todas pasan en mi corrida. Los mutantes M1 y M2 de la bitácora no los repetí; sí repetí uno equivalente para F-06. |
| F-03 | **cerrado** | `REPLICAS_POR_DEFECTO_K8S` existe. `deploymentSinReplicasEspecificadas...` siembra un Deployment sin `spec.replicas` y comprueba `isEqualTo(1)` y `LISTO`. |
| F-04 | **cerrado, con un resto** | `DespliegueAusenteException(deployment, namespace)` recibe `NOMBRE_CORE` como argumento y la prueba usa las constantes. El resto va en F-09. |
| F-05 | **cerrado** | Cada lectura pasa por su propio `envuelve`. La prueba exige «pods» y prohíbe «Deployment» en el mensaje. |
| F-06 | **abierto a medias** | El orden por `Instant` está probado (`:02Z` frente a `:02.500000Z`). El caso sin marca no lo está: ver F-08. |
| F-07 | **no aplica** | Arbitrado como inherente; no se cambia. |

## Hallazgos nuevos

### F-08 · NARANJA · `EstadoYContextoTest.java` (`losEventosSeOrdenanPorInstante...YLosSinMarcaVanAlFinal`) · La rama «sin marca va al final» no está probada
`siembraEvento("e0", ..., null)` crea el evento por CRUD. El `PostHandler` de `kubernetes-server-mock` 7.9.0 llama siempre a `setCreationTimestamp(ZonedDateTime.now(UTC))`; lo comprobé desensamblando la clase con `javap`.

Así, `hora(e0)` no da `null`, sino el instante de la corrida (2026-11-27). Ese instante cae después de las fechas sembradas de 2026-01-01, así que e0 sale último por orden natural. Ni `nullsLast` ni el `catch (DateTimeParseException)` se ejecutan nunca.

- **Mutante.** Copié el HEAD con `git archive` a `scratchpad/r2/mut` y cambié `nullsLast` por `nullsFirst` en `LectorPais.java:125`.
- **Resultado.** `BUILD SUCCESSFUL`, `tests="20" ... failures="0"`. El mutante sobrevive.
- **Afirmación sin evidencia.** La respuesta dice «prueba con ... y un evento sin marca». El nombre de la prueba afirma un comportamiento que la prueba no puede detectar.

Lo esperado es una de dos cosas:
- servir la lista de eventos con una expectativa, de modo que un evento llegue sin ninguna marca (o con una marca que no se puede analizar);
- o sembrar las fechas en el futuro, para que el orden natural ponga e0 primero y la prueba tenga dientes.

Nota: un API server real también pone siempre `creationTimestamp`. En la práctica, un `null` solo saldrá de una marca que no se puede analizar.

### F-09 · AMARILLO · `EstadoYContextoTest.java` (`RUTA_PODS`) · Al barrido de F-04 se le escapa un literal
`"app.kubernetes.io%2Fname%3Dsynkra-core"` repite `ETIQUETA_NOMBRE` y `NOMBRE_CORE` como texto. Se podría construir con `URLEncoder` a partir de las constantes. No bloquea.

### F-10 · AMARILLO · prueba con `requestRetryBackoffLimit(0)` (cambio no pedido) · Juicio sobre si oculta algo
Revisé `StandardHttpClient.shouldRetry` en `kubernetes-client-api` 7.9.0. Fabric8 reintenta ante 429, ante cualquier código ≥ 500 y ante errores de E/S. No reintenta ante 400, 403 ni 404.

El código de `main` no configura los reintentos (`grep RetryBackoff` en `backend/src/main` sale vacío). Con los valores por defecto (10 reintentos, 100 ms, exponente máximo 5) hay estas diferencias con producción:
- **Lo que no cambia.** El resultado es el mismo: `LecturaFallidaException` con la causa y el código. Todo son `GET` idempotentes al mismo namespace, y la frontera 400/404 frente al resto se comporta igual.
- **Lo que no se ve en las pruebas.** Ante un 5xx o un 429 en producción, cada lectura hace hasta 11 `GET`. Tarda unos 19 s (100+200+400+800+1600+3200×5 ms) antes de lanzar la excepción. Ninguna prueba fija ese comportamiento.

Mi juicio: no oculta nada de la semántica que pide esta tarea, pero sí la latencia y el número de peticiones. Eso choca de lleno con S5 de U07, que sondea cada 5 s con un plazo de 3 min. Es exactamente C-76. Está bien en la prueba porque lo declara el comentario y la bitácora. Conviene anotar en C-76 que la suite de U05-T05 corre sin reintentos y que U07 debe fijar la configuración de producción con una prueba. No bloquea.

### F-11 · AMARILLO · `LectorPais.java` · Detalle de estilo
`java.util.function.Supplier` aparece con nombre completo, y los imports de la prueba están desordenados. No bloquea.

**Estado del worktree al terminar:** limpio. `git status --short` sale vacío en `/home/user/wt-m6r-U05-T05`. El mutante se corrió en una copia dentro del scratchpad. No toqué `/home/user/wt-m6r-U02-T05`.

## Tareas candidatas (defectos reales fuera de alcance)
- **Ampliar C-76.** El cliente de producción no configura los reintentos. Una lectura de `estado` o `contextoFallo` ante un 5xx o un 429 tarda unos 19 s y hace 11 `GET`, y las pruebas de U05-T05 lo neutralizan con `requestRetryBackoffLimit(0)`. U07 debe decidir la configuración y fijarla con una prueba que use la configuración real.
- C-82 y C-83 siguen en pie, tal como las registró el orquestador.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca2.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca2-xml.txt (los 28 XML)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/mut1.log (el mutante `nullsFirst` sobrevive)

```
VEREDICTO: NO-VERDE
NARANJA|EstadoYContextoTest.java:losEventosSeOrdenanPorInstante...SinMarcaVanAlFinal|El evento «sin marca» recibe creationTimestamp del simulado; nullsLast sin probar (el mutante nullsFirst sobrevive)
INFORME: revisiones/U05-T05/ronda-2.md
```
