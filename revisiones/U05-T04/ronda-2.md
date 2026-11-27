# Ronda 2 — U05-T04

VEREDICTO: VERDE

La tarea cumple los tres criterios en mi propia corrida, y los cinco hallazgos de la ronda 1 quedan cerrados. Ninguno de los dos hallazgos nuevos es ROJO ni NARANJA. Verde significa que vale la pena fusionar, y fusiona el humano.

- **Hash de la tarea:** `git hash-object tareas/U05-T04-aplicar.md` da 33e2a2cb488a1a08c7e71a5c1b0d7cd7f113c08f y coincide.
- **Revisado:** HEAD `cd2ec6f`, diff `git diff ceb914c...HEAD`, 7 archivos, +521.
- **Gradle:** `HOME` y `KUBECONFIG` apuntan a un `mktemp -d` dentro del scratchpad, con `-Duser.home` en ese mismo directorio y `GRADLE_USER_HOME=/root/.gradle`.
- **Clúster:** no usé ninguno, ni `kubectl`; las pruebas corren solo contra el servidor simulado de Fabric8.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `AplicarTest`: al menos 5 pruebas, sin saltos ni fallos, con lectura de vuelta | `./gradlew :backend:test --rerun --tests '*AplicarTest'` y luego el grep del XML | **pasa.** `BUILD SUCCESSFUL in 4s`, `tests="8" skipped="0" failures="0" errors="0"`. El XML es de mi corrida: timestamp 2026-11-27T20:38:23Z, y empecé a las 20:38 UTC. Cada prueba tarda entre 0,023 y 0,83 s, con servidor detrás. La lectura de vuelta usa el PATCH registrado, la vía alterna que autoriza el plan de pruebas y que ya se juzgó en la ronda 1. |
| 2 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` y luego el bucle de grep | **pasa.** `BUILD SUCCESSFUL in 55s`. Son 27 XML con 380 pruebas en total: 0 skipped, 0 failures y 0 errors. Los timestamps van de 20:38:36Z a 20:39:27Z. La suma coincide con lo que dice la bitácora (380 = 379 + 1 prueba nueva). |
| 3 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat ceb914c...HEAD` | **pasa.** HEAD es `cd2ec6f`. Solo cambian `backend/src/{main,test}/java/io/synkra/kubernetes/**`, `bitacoras/U05-T04.md` y `revisiones/U05-T04/`. |

## Estado de los hallazgos de la ronda 1
| Id | Sev. | Estado | Cómo lo comprobé |
|---|---|---|---|
| F-01 | NARANJA | **cerrado** | Con `grep -rn '"synkra"\|fieldManager=synkra'` sobre `kubernetes/` de main y sobre `AplicarTest.java`, la única aparición que queda es `ConstructorObjetosPais.java:30` (`GESTOR`). `aplicar` usa `ConstructorObjetosPais.GESTOR`. La prueba usa `GESTOR`, `NOMBRE_CORE` y `NOMBRE_CONFIG` en las aserciones y en las expectativas de `arranca()`. |
| F-02 | NARANJA | **cerrado** | Sin cliente, `aplicar` lanza `IllegalStateException` sin hacer ninguna petición, después de la guardia de namespaces. El `catch` ahora atrapa solo `KubernetesClientException`, y lo documenta el Javadoc de la interfaz. Comprobé con un mutante que la prueba nueva lo detecta (ver más abajo). |
| F-03 | NARANJA | **cerrado** | La prueba ahora pide exactamente `containsExactly("PATCH", "PATCH")` sobre todas las peticiones registradas, sin filtrar. Comprobé con un mutante que detecta un `DELETE` que deshaga el ConfigMap. |
| F-04 | AMARILLO | **cerrado** | El Javadoc de la clase dice «modo de expectativas (no CRUD)». |
| F-05 | AMARILLO | **cerrado** | `peticiones()` lee exactamente `getRequestCount() - leidas` peticiones, con `takeRequest(5, SECONDS)` como tope y no como corte. |

**Prueba de mutantes:** la hice en una copia `git archive HEAD` dentro del scratchpad, nunca en el worktree. Quité la comprobación de cliente nulo, volví a `catch (RuntimeException)` y añadí un `DELETE` del ConfigMap después del fallo. Resultado:
- `./gradlew :backend:test --rerun --tests '*AplicarTest'` dio `8 tests completed, 2 failed`, `BUILD FAILED`.
- Falló `sinClienteAplicarFallaConErrorPropioYSinAcusarALaApi` con `AssertionError at AplicarTest.java:227`.
- Falló `siFallaElSegundoNombraElObjetoYElPrimeroSeQueda` con `AssertionFailedError at AplicarTest.java:216`, la línea de `containsExactly`.

Son las mismas líneas del rojo que la bitácora dice haber visto. La afirmación del rojo primero en un worktree temporal, ya borrado, no se puede verificar directamente, pero mi mutante la reproduce. Después borré la copia.

## Hallazgos nuevos

### F-06 · AMARILLO · `AplicarTest.java:89-95` (`peticiones()`) · Si se agota la espera, entra un `null` en la lista
`takeRequest(5, SECONDS)` devuelve `null` si se agota el tiempo, y `peticiones()` lo añade a la lista sin comprobarlo. Una carrera entre el contador y la cola, o un reinicio mal hecho de `leidas`, daría un `NullPointerException` lejos de la causa. La bitácora cuenta que eso le pasó al codificador: «2 NPE extra en AplicarTest:102». Bastaría con un `assertThat(req).isNotNull()` o con fallar con un mensaje. No bloquea.

### F-07 · AMARILLO · `AdaptadorFabric8.java:63` (`namespaceComun`) · No valida los metadatos
`namespaceComun` lee `getMetadata().getNamespace()` sin comprobar que `configMap()`, `service()` y `deployment()` no sean nulos. Si `ObjetosPais` no lo garantiza, un objeto incompleto da `NullPointerException` en vez de un error con nombre. Como los objetos salen de `construir`, el riesgo es bajo. Si conviene, se puede resolver junto con C-70 o C-73 (frontera de `ObjetosPais`, U05-T09).

**Estado del worktree al terminar:** limpio. `git status --short` sale vacío. Ignorados: `.gradle/`, `build/`, `backend/build/`, `backend/.jqwik-database` y `verificador/build/`; este último no viene de esta tarea ni de mi corrida.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-75 (apply contra un servidor real) y C-76 (reintentos de Fabric8 frente a U07) siguen registradas y siguen fuera de alcance. El diff no las toca.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2-ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2-ca2.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2-ca2-xml.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/mutante.log

```
VEREDICTO: VERDE
AMARILLO|AplicarTest.java:89-95|peticiones() puede añadir null si se agota la espera
INFORME: revisiones/U05-T04/ronda-2.md
```
