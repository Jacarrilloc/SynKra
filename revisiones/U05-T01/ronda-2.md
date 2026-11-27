# Ronda 2 — U05-T01

VEREDICTO: VERDE

Corrí yo los cuatro criterios y los cuatro pasan. Los tres NARANJA de la ronda 1 están cerrados: repetí las dos mutaciones y la sonda de `SYNKRA_KUBERNETES_URL`, y ahora las mutaciones mueren y la sonda ya no redirige. `VariablesPod` no se puede inyectar en producción. Quedan tres AMARILLO, que no bloquean.

Comprobaciones previas:
- Hash de la tarea: `5f6f9723f6929f7ca2297317a5c8aec3d83f67aa`, el esperado.
- HEAD del worktree: `57fa6ca`, el reportado.
- El worktree estaba limpio al empezar y sigue limpio al terminar (`git status --short` vacío).
- No usé `kubectl` y no leí el `~/.kube/config` real. Gradle y la JVM corrieron siempre con `HOME`, `KUBECONFIG` y un `.kube/config` falsos en el scratchpad (`GRADLE_USER_HOME` apuntaba a la caché real). Las pruebas ponen además `user.home` y `kubeconfig` en un `@TempDir`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Cliente con la cuenta del pod contra el servidor simulado | `./gradlew --no-daemon :backend:test --rerun --tests '*ClienteKubernetesTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="9" skipped="0" failures="0" errors="0"`. XML fresco (19:26:54Z, 3 s antes de mi `date`). Cada caso tarda entre 0,079 y 1,48 s, así que ninguno se saltó |
| 2 | Arranca sin clúster y sin el kubeconfig de la máquina | `bootJar` + `KUBECONFIG=/nonexistent java -jar backend-0.1.0.jar --server.port=18080`, `SYNKRA_DATA` en el scratchpad | pasa: `UP`. El log no menciona kube ni fabric8 (`grep -ciE "kube\|fabric8"` da 0). El puerto 18080 estaba libre antes y quedó libre después (0 y 0), sin procesos huérfanos |
| 3 | Todo el backend y las fronteras | `./gradlew --no-daemon :backend:test --rerun :backend:check` + los XML + grep de `io.fabric8` | pasa: `BUILD SUCCESSFUL`. 23 XML, todos con skipped, failures y errors en 0. `FronterasModulosTest` da 5 pruebas en 1,9 s. El grep da `ninguno` |
| 4 | Diff acotado | `git diff --stat b1a525f...HEAD` | pasa: 12 archivos y 625 inserciones, todos dentro de lo permitido (`build.gradle.kts`, `kubernetes/**` en main y test, bitácora, `revisiones/U05-T01/`). El `ca.crt` de `src/test/resources` ya no está, y `application.properties` no se toca |

## Cierre de los hallazgos de la ronda 1, verificado por mí

Las mutaciones y las sondas se hicieron en una copia (`git archive HEAD`) dentro del scratchpad, no en el worktree.

- **F-01, cerrado.** Sonda en la copia: la JVM de pruebas llevaba en su entorno `SYNKRA_KUBERNETES_URL=http://127.0.0.1:1`, `KUBERNETES_SERVICE_HOST=10.0.0.1` y `KUBERNETES_SERVICE_PORT=6443`. Además, como propiedades de Spring, `synkra.kubernetes.url=http://127.0.0.1:1`, `KUBERNETES_SERVICE_HOST=evil.example` y `kubernetes.service.host=evil2.example`. Resultado: `SONDA master=https://10.0.0.1:6443/` y `variablesPodBeans=0`. La URL sale solo de `System.getenv` y siempre por https; las propiedades no la cambian.
- **F-02, cerrado.** Mutación `withAutoConfigure(true)` con `HOME` y `KUBECONFIG` falsos: `elKubeconfigDeLaMaquinaNoSeUsa() FAILED` en `ClienteKubernetesTest.java:156` (la afirmación de que `getFile()` es nulo); `9 tests completed, 1 failed`.
- **F-03, cerrado.** Mutación sin `withCaCertFile`: fallan `conTokenYCaDeLaCuentaLaPeticionLlevaElBearerPorTls()` y `elKubeconfigDeLaMaquinaNoSeUsa()` con `SSLHandshakeException` y `SunCertPathBuilderException`. Ahora la CA de la cuenta sí participa en TLS.
- **F-04 y F-05, cerrados.** Existen las pruebas `unTokenIlegibleCuentaComoSinToken` y `conTokenPeroSinKubernetesServiceHostNoHayCliente` y pasan. Sus mutaciones están en la bitácora; esas no las repetí.

## `VariablesPod`: ¿se puede inyectar en producción?

No por ninguna vía de configuración.

- **Código de main.** Ningún archivo de main registra un bean `VariablesPod` (grep). El tipo es un `record` privado del paquete, sin `@Component`, así que el escaneo de componentes no lo recoge. Tampoco aparece en `src/main/resources`.
- **Contenido del jar.** El bootJar no trae clases de prueba ni `kubernetes-server-mock` (`unzip -l`).
- **`spring.main.sources`, la única vía por propiedad que registra una clase arbitraria como bean.** La probé con el bootJar real. Las variables del pod y un token falso estaban puestos, con `--management.endpoints.web.exposure.include=health,beans`:
  - Sin `spring.main.sources`: existen los beans `clienteKubernetes`, `adaptadorKubernetes` y `configuracionKubernetes`, y no hay ningún bean `VariablesPod`.
  - Con `--spring.main.sources=io.synkra.kubernetes.VariablesPod`: el arranque aborta con `Failed to instantiate [io.synkra.kubernetes.VariablesPod]: No default constructor found`. No hay sustitución silenciosa y, en el peor caso, la aplicación no arranca, sin redirigir nada.
- **Lo que queda fuera.** La única otra vía es meter una clase de configuración en el classpath, y eso equivale a cambiar el código o la imagen, fuera del modelo de amenaza de esta tarea.

## Hallazgos

### F-01 · AMARILLO · `ClienteKubernetesTest.laUrlNoSaleDeLasPropiedadesDeSpring` · La prueba depende de que el proceso de pruebas no tenga `KUBERNETES_SERVICE_HOST`
La prueba no registra `VariablesPod`, así que lee el `System.getenv()` real. En la copia, con `KUBERNETES_SERVICE_HOST` y `KUBERNETES_SERVICE_PORT` en el entorno de la tarea de pruebas (como en un CI que corre dentro de un pod), da `laUrlNoSaleDeLasPropiedadesDeSpring() FAILED`, `9 tests completed, 1 failed`. Falla de forma visible, sin dar un falso verde. Se arregla con `withBean(VariablesPod.class, () -> new VariablesPod(Map.of()))`, manteniendo las propiedades falsas: la prueba seguiría probando lo mismo y dejaría de depender del entorno. Log: `env-pod.log`.

### F-02 · AMARILLO · `ClienteKubernetesTest.sinLaCaDeLaCuentaLaConexionTlsFalla` · La afirmación acepta cualquier excepción
`assertThatThrownBy(cliente::getKubernetesVersion).isNotNull()` pasa con cualquier fallo, no solo con uno de TLS. Convendría exigir que la causa sea una `SSLHandshakeException`. Además usa `org.assertj...Assertions` con el nombre completo, aunque el import ya existe.

### F-03 · AMARILLO · varios · Detalles menores
- Los imports de la prueba siguen desordenados (`java.util.Base64` antes de `java.security.cert.X509Certificate`), aunque la respuesta a la ronda 1 dice «imports ordenados».
- `CuentaServicio.java:19`: el puerto por defecto `"443"` es un literal. No es un valor visible para el usuario, porque en un pod `KUBERNETES_SERVICE_PORT` siempre existe. Lo anoto para que lleve un nombre de constante, como `RUTA_POR_DEFECTO`.
- Sigue en pie lo que la ronda 1 dejó como opcional: `disponible()` es estático tras el arranque.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/`:
- `ca1.log`, `ca3.log`, `ca2.log`, `ca2-build.log` (criterios 1 a 3)
- `mut-auto.log` (mutación de `autoConfigure`)
- `mut-ca.log` (mutación sin la CA)
- `sonda-env.log` (sonda de `SYNKRA_KUBERNETES_URL` y propiedades)
- `vp-base.log`, `vp-sources.log`, `beans-base.json` (inyección de `VariablesPod` con `spring.main.sources`)
- `env-pod.log` (F-01)
- `copia/` (copia mutada con la sonda, fuera del worktree)

```
VEREDICTO: VERDE
AMARILLO|ClienteKubernetesTest.laUrlNoSaleDeLasPropiedadesDeSpring|depende de que el proceso de pruebas no tenga KUBERNETES_SERVICE_HOST (falla dentro de un pod)
INFORME: revisiones/U05-T01/ronda-2.md
```
