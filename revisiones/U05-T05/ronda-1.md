# Ronda 1 — U05-T05

VEREDICTO: NO-VERDE

Hash de la tarea: `git hash-object /home/user/wt-orq-U04/tareas/U05-T05-estado-contexto.md` da 65a449cecd38706d8a1f17df2d817a5e51150f09, que coincide. Revisé el HEAD `594f240` contra la base `0a52e2e` (`git diff 0a52e2e...HEAD`): 11 archivos, +680. Corrí Gradle con `HOME`, `KUBECONFIG` y `-Duser.home` en un `mktemp -d` y `GRADLE_USER_HOME=/root/.gradle`. No usé kubectl ni ningún clúster, solo el servidor simulado de Fabric8.

Los tres criterios pasan en mi propia corrida. Lo que bloquea son tres NARANJA: un caso del criterio que se siembra distinto de como se nombra, la ruta de error de la decisión 6 sin prueba, y un literal nuevo en un punto de decisión.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `EstadoYContextoTest` ≥ 8, sin saltos ni fallos | `./gradlew :backend:test --rerun --tests '*EstadoYContextoTest'` + grep del XML | **pasa**: `BUILD SUCCESSFUL in 8s`, `tests="12" skipped="0" failures="0" errors="0"`. El XML es de mi corrida (timestamp `2026-11-27T20:47:08Z`). Cada prueba tarda entre 0,023 y 0,294 s, con servidor simulado real detrás. Ver F-01 sobre el caso `Pending`. |
| 2 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | **pasa**: `BUILD SUCCESSFUL in 43s`, 28 XML, todos con `skipped="0" failures="0" errors="0"` |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 0a52e2e...HEAD` | **pasa**: `594f240`. Solo cambian `backend/src/{main,test}/java/io/synkra/kubernetes/**` y `bitacoras/U05-T05.md`. Ninguna escritura: solo `get`, `list` y `pods/log`. |

Sobre la lectura de vuelta en CA-1:
- **Log de 200 líneas.** El simulado no recorta con `tailLines`, así que la prueba sirve directamente las líneas 801 a 1000. La expectativa casa solo con la ruta exacta `...&tailLines=200`, y además se comprueba ese parámetro en la petición registrada. Es la vía que autoriza el plan de pruebas y la bitácora la declara.
- **Métodos y rutas.** El caso «todas GET» lee las peticiones registradas y comprueba método y ruta (`/namespaces/synkra-pe/`).

## Hallazgos

### F-01 · NARANJA · `EstadoYContextoTest.java` (`siembraPod`, `podPendienteEsEnCursoYElContextoNoFallaConLogsVacios`) · El caso «pod `Pending`» siembra un pod en `Running`
`siembraPod` fija `withPhase("Running")` en todos los pods, también en el que la prueba llama pendiente. Ese pod lleva además un init `waiting` con `PodInitializing`. CA-1 pide explícitamente «pod `Pending` sin logs», y ese objeto no se siembra en ninguna parte.

No lo marco ROJO porque `LectorPais` no lee `status.phase`, así que el resultado sería el mismo. Aun así, la evidencia no corresponde al caso que nombra el criterio. También queda sin probar el `Pending` realista: un pod aún sin programar no tiene `containerStatuses` ni `initContainerStatuses`, y eso pasa por la rama `estados()` vacía.

Lo esperado: un pod con `phase: Pending`, sin estados de contenedor, con las respuestas 400 de log que ya están, y las mismas aserciones.

### F-02 · NARANJA · `LectorPais.java` (`leer`, los `catch` de `estado` y `contexto`) · La ruta de error de la decisión 6 no tiene ninguna prueba
La decisión 6 fija que solo las `KubernetesClientException` se envuelven en excepción del módulo. Además, `leer()` convierte en log vacío cualquier 400 o 404. Ninguna prueba ejercita lo siguiente:
- que un 500 de la API (en el GET del Deployment, en el list de pods o de eventos) sale como `LecturaFallidaException`, con el namespace y la causa;
- que un 500 o un 403 al pedir un log **no** se traga como vacío.

La frontera 400/404 contra «otro código sube» es justo la que puede ocultar un fallo real de diagnóstico: un contexto vacío parece «aún no hay logs». Hoy esa frontera es solo una afirmación de la bitácora.

Para comprobarlo: `grep -n 'LecturaFallida\|500\|403' backend/src/test/java/io/synkra/kubernetes/EstadoYContextoTest.java` no devuelve nada.

### F-03 · NARANJA · `LectorPais.java:44` · Las réplicas deseadas por defecto son un literal `1` en el punto de decisión
```java
int deseadas = d.getSpec() != null && d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 1;
```
- **Visible y decisivo.** El valor sale al usuario en `EstadoPais.replicasDeseadas` y decide `LISTO` (`listas == deseadas && deseadas > 0`).
- **Sin nombre.** Es el valor por defecto de Kubernetes, pero entra sin nombre, al revés de lo que pide la decisión 3 y de la lección F-01 de U05-T04.
- **Sin prueba.** Ninguna prueba siembra un Deployment sin `spec.replicas`.

Lo esperado: una constante con nombre (por ejemplo `REPLICAS_POR_DEFECTO_K8S`) y un caso de prueba.

### F-04 · AMARILLO · Literales repetidos en vez de constantes (barrido de la clase)
- **`DespliegueAusenteException.java:7`.** El mensaje incrusta `"synkra-core"` en lugar de recibir el nombre desde `ConstructorObjetosPais.NOMBRE_CORE`, que es privada de paquete y no se ve desde `api`. Podría pasarse como argumento.
- **`EstadoYContextoTest.java`.** Repite literales que ya existen como constantes:
  - `"synkra-core"` en `siembraDeployment`;
  - `"app.kubernetes.io/name"` y `"synkra-core"` en `siembraPod`;
  - `"core"` y `"descarga"` en `sirveLog` y `logAunNoExiste`;
  - `tailLines=200` en tres rutas.

  Es la misma clase que F-01 de U05-T04. En pruebas puede defenderse como oráculo independiente, pero no se decidió así.

### F-05 · AMARILLO · `LectorPais.java` (`estado`) · Mensaje de error que nombra el objeto equivocado
Si falla el `list` de pods dentro de `estado`, la excepción dice «No se pudo leer el Deployment synkra-core», y no fue el Deployment lo que falló.

### F-06 · AMARILLO · `LectorPais.java` (`hora`) · Eventos ordenados como texto mezclando formatos
`lastTimestamp` (`...:02Z`) y `eventTime` (`...:02.000000Z`) se comparan como cadenas. Casi siempre ordena bien, pero comparar `Instant` sería exacto. Un evento sin ninguna marca de tiempo sale primero.

### F-07 · AMARILLO · `EstadoYContextoTest.java` (`sinClienteHayErrorPropioYCeroPeticiones`) · El «cero peticiones» no puede fallar
`new AdaptadorFabric8(null)` no tiene ninguna conexión con `servidor`, así que `peticiones()` sale vacío pase lo que pase. El caso sí prueba la excepción propia. Es inherente al diseño y no bloquea. La comprobación con dientes es la de los namespaces prohibidos, que usa un cliente conectado.

**Estado del worktree al terminar:** limpio. `git status --short` sale vacío en `/home/user/wt-m6r-U05-T05`. No toqué `/home/user/wt-m6r-U02-T05`.

## Tareas candidatas (defectos reales fuera de alcance)
- **Precedencia de `LISTO` frente a `FALLIDO` durante un rollout.** Lo observó el codificador. Con la decisión 2 tal como está escrita, un pod nuevo en `CrashLoopBackOff` junto a uno viejo listo da `LISTO`. U07 (S5) debe decidir si mira `updatedReplicas` u `observedGeneration` antes de dar por listo.
- **Contexto de un solo pod.** `contextoFallo` toma el log del pod con más reinicios y deja fuera a los demás. Basta con una réplica, pero con varias U06 o U07 podrían querer el del pod nuevo del rollout.
- **Eventos fuera del contexto.** Solo entran los eventos de los pods, así que se pierden los del ReplicaSet o el Deployment (por ejemplo `FailedCreate` por cuota). Conviene valorarlo junto con H-16.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/tasks/b8rrfb9rg.output (CA-2 con los 28 XML y los tiempos por prueba)

```
VEREDICTO: NO-VERDE
NARANJA|EstadoYContextoTest.java:siembraPod|El caso «pod Pending» de CA-1 siembra un pod en fase Running
NARANJA|LectorPais.java:leer y catch|La ruta de error de la decisión 6 (LecturaFallida, 500 de log no tragado) no tiene ninguna prueba
NARANJA|LectorPais.java:44|Las réplicas deseadas por defecto son un literal 1 en el punto de decisión, sin nombre ni prueba
INFORME: revisiones/U05-T05/ronda-1.md
```
