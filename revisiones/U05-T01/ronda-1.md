# Ronda 1 — U05-T01

VEREDICTO: NO-VERDE

Los cuatro criterios pasan con mi propia ejecución. Quedan en pie tres NARANJA: una vía de configuración que funciona en producción, y dos mutaciones que sobreviven a la suite.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Cliente con la cuenta del pod contra el servidor simulado | `./gradlew :backend:test --rerun --tests '*ClienteKubernetesTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="4" skipped="0" failures="0" errors="0"`. Tiempos por caso entre 0,026 s y 0,708 s, y el XML es fresco (14:17:49) |
| 2 | Arranca sin clúster y sin el kubeconfig de la máquina | bootJar + `KUBECONFIG=/nonexistent java -jar ... --server.port=18080`, con `SYNKRA_DATA` en el scratchpad | pasa: `UP`. El log no menciona kube ni fabric8 (`grep -ciE "kube|fabric8"` da 0). Puerto 18080 libre antes y después (`ss -ltn | grep -c 18080` da 0) |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + los XML + grep de `io.fabric8` | pasa: `BUILD SUCCESSFUL`. 23 XML con skipped, failures y errors en 0, incluido `FronterasModulosTest` (5, con `verify()` real). El grep da `ninguno` |
| 4 | Diff acotado | `git diff --stat b1a525f...HEAD` | pasa: 10 archivos y 360 inserciones. Son `build.gradle.kts`, `kubernetes/**` en main y test, `src/test/resources/kubernetes/ca.crt` y la bitácora. `application.properties` no se toca |

El worktree quedó limpio al terminar (`git status --short` vacío). No corrí `kubectl` y no leí el `~/.kube/config` real. Las sondas usaron un kubeconfig falso en el scratchpad, con `HOME`, `user.home` y `KUBECONFIG` apuntando ahí.

## Lo que me pediste juzgar

**Fabric8 7.9.0 frente a 8.0.0: correcto.** En Maven Central, la última versión publicada es la 8.0.0. Su `kubernetes-client-api-8.0.0.pom` depende de `tools.jackson.core:jackson-databind` y `jackson-core`, es decir, de Jackson 3. La 7.9.0 es la última 7.x (la serie va 7.6.0, 7.6.1, 7.7.0, 7.8.0, 7.9.0). El `runtimeClasspath` resuelve un único `com.fasterxml.jackson.core:jackson-databind:2.21.4`, alineado con Spring Boot 3.5.16, y no aparece `tools.jackson`. La versión está fijada sin rangos, y `kubernetes-server-mock` está solo en `testImplementation`. Cumple la decisión 1.

**`synkra.kubernetes.url`: sí abre una vía en producción.** Ver F-01.

**`autoConfigure` apagado: funciona, pero ninguna prueba lo protege.** Ver F-02. Con una sonda (`Sonda.java` en el scratchpad) contra los jars del bootJar comprobé lo siguiente, con `KUBECONFIG` falso, `KUBERNETES_NAMESPACE=ns-env` y `KUBERNETES_MASTER` puestos:
- `withAutoConfigure(false)` da `ctx=null file=null ns=null`: no lee nada.
- `withAutoConfigure(true)` carga el contexto `ajeno` del archivo falso, `file=<kubeconfig-falso>` y `ns=ns-env`.

El código actual no lee nada del entorno, y la línea `withAutoConfigure(false)` es la que lo consigue.

## Hallazgos

### F-01 · NARANJA · `CuentaServicio.java:15-21` · La URL de «solo pruebas» sirve en producción y puede mandar el token del pod a cualquier servidor
`CuentaServicio.de` lee `synkra.kubernetes.url` del `Environment` de Spring. Eso incluye las variables de entorno (por enlace relajado, `SYNKRA_KUBERNETES_URL`), los argumentos `--synkra.kubernetes.url=` y `application.properties`.

Lo probé en una copia del árbol en el scratchpad (`git archive`), con `SYNKRA_KUBERNETES_URL=http://127.0.0.1:1` en el entorno de la JVM de pruebas y solo `synkra.kubernetes.cuenta` como propiedad. El resultado fue `SONDA master=http://127.0.0.1:1/` y el bean `KubernetesClient` se creó (log en `sonda-env.log`).

En el pod, cualquiera que pueda poner una variable en el Deployment manda el `Bearer` de la cuenta de servicio a un host arbitrario, y además por `http` en claro. La decisión 2 dice que la URL sale de `KUBERNETES_SERVICE_HOST`/`KUBERNETES_SERVICE_PORT`. El `package-info` dice «nada sale de otro sitio».

Hay una variante del mismo problema: esas dos variables también se leen por el `Environment`, así que se pueden fijar con `--KUBERNETES_SERVICE_HOST=` o desde un archivo de propiedades. Conviene leerlas con `System.getenv`.

Para las pruebas no hace falta una propiedad de producción. Basta con inyectar la URL por una vía que solo exista en el código de prueba, por ejemplo un constructor o fábrica de paquete, o un bean que solo registre la prueba.

### F-02 · NARANJA · `ClienteKubernetesTest.elKubeconfigDeLaMaquinaNoSeUsa` · La prueba no se rompe si se enciende `autoConfigure`
Mutación en la copia del scratchpad: cambié `withAutoConfigure(false)` por `(true)`, con `KUBECONFIG`, `HOME` y `user.home` falsos en la tarea de pruebas. Resultado: `BUILD SUCCESSFUL` y `tests="4" ... failures="0"` (log en `mut-auto.log`).

La prueba pasa porque la URL y el token explícitos ganan a los del kubeconfig. El cliente sí lee el archivo ajeno y toma de él contexto y namespace, y la prueba no lo ve. El namespace es justo lo que vigilará la guardia de U05-T02.

Falta la lectura de vuelta del estado que se quiere proteger. Por ejemplo, con el kubeconfig falso puesto, afirmar que `getConfiguration()` tiene `getFile()`, `getCurrentContext()` y `getNamespace()` en nulo.

La bitácora dice que no se hizo esta mutación porque «leería el `~/.kube/config` real». No es así: basta con un `KUBECONFIG` o `kubeconfig` falso, como acabo de hacer.

### F-03 · NARANJA · `ConfiguracionKubernetes.java:40` · Ninguna prueba cubre la CA de la cuenta
Mutación en la copia: borré la línea `withCaCertFile(...)`. Resultado: `BUILD SUCCESSFUL` y `tests="4" ... failures="0"` (log en `mut-ca.log`).

El servidor simulado es `KubernetesMockServer(false)`, en `http`, así que el `ca.crt` que se copia al `@TempDir` nunca participa. En producción la URL es `https://` y la conexión depende de esa CA. El criterio pide «token y CA de prueba». Sin una prueba en TLS (`KubernetesMockServer(true)` con su certificado como `ca.crt`), lo de la CA queda afirmado pero sin verificar.

### F-04 · AMARILLO · `CuentaServicio.java:40-44` · Un error de lectura del token puede tumbar el arranque
Si el archivo del token existe y se puede leer pero la lectura falla, `token()` lanza `UncheckedIOException`, y lo hace dentro de la `Condition`, al arrancar. La decisión 2 pide que no salga ninguna excepción al arrancar el backend. Conviene tratarlo como «sin token».

### F-05 · AMARILLO · varios · Detalles menores
- `disponible()` solo mira si hay bean (`cliente != null`). Si el token desaparece después de arrancar, sigue devolviendo verdadero.
- Falta el caso «hay token pero no hay `KUBERNETES_SERVICE_HOST`».
- El caso de `KUBECONFIG` usa la propiedad de sistema `kubeconfig` y no la variable de entorno. Fabric8 sigue la misma ruta de lectura y la bitácora lo justifica. Lo dejo anotado.
- `java.nio.file.Files` está escrito con el nombre completo dentro de `ConfiguracionKubernetes`, y los imports están desordenados en la prueba (`RecordedRequest`).

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut-auto.log (F-02)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut-ca.log (F-03)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/sonda-env.log (F-01)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/probe/Sonda.java (sonda de `autoConfigure`)
- Copia mutada, fuera del worktree: /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/copia

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/kubernetes/CuentaServicio.java:15|synkra.kubernetes.url (y KUBERNETES_SERVICE_*) se leen del Environment: en producción redirigen el token del pod
NARANJA|ClienteKubernetesTest.elKubeconfigDeLaMaquinaNoSeUsa|la mutación withAutoConfigure(true) sobrevive: falta leer de vuelta file/contexto/namespace
NARANJA|backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java:40|la CA no tiene cobertura: quitar withCaCertFile sobrevive (servidor simulado en http)
INFORME: revisiones/U05-T01/ronda-1.md
```
