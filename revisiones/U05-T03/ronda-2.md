# Ronda 2 — U05-T03

VEREDICTO: NO-VERDE

Los tres criterios pasan y las mutaciones que pediste quedan atrapadas, pero quedan dos NARANJA. La validación de la imagen del init deja pasar imágenes sin registro (F-01). Y si `synkra.kubernetes.imagen-init` deja de llegar al adaptador, ninguna prueba lo nota (F-02).

Datos de la revisión:
- Rama `m6r/U05-T03` en `4de38d4`, con el código en `a1361e4`. Base `36f0cd3`.
- El hash de la tarea coincide: `1d7a200879ff2bdd2e4e19b893279beafd4831c5`.
- Corrí todo con `HOME`, `KUBECONFIG` y `-Duser.home` apuntando a archivos falsos de mi scratchpad. `GRADLE_USER_HOME=~/.gradle` solo sirvió para reutilizar los toolchains.
- No usé `kubectl` ni leí `~/.kube/config`.
- Las mutaciones y las sondas se hicieron en una copia (`git archive HEAD`) fuera del worktree.
- Al terminar, `git status --short` del worktree sale vacío y la copia quedó idéntica al worktree (`diff -r backend/src`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | YAML esperado, ≥6 pruebas, sin peticiones | `./gradlew :backend:test --rerun --tests '*ConstruirObjetosPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 6s`, `tests="29" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,07 y 0,95 s: se ejecutaron, no se saltaron |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 25s`. Son 25 XML y 366 pruebas, sin ningún `skipped`, `failures` ni `errors` distinto de 0. Los conteos coinciden con los que pegó la bitácora |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 36f0cd3...HEAD` | pasa: `4de38d4`, 12 archivos, +882 y −2. Todo está en `kubernetes/**` (main, test y recursos), más `application.properties` (permitido por el arbitraje de F-01), la bitácora y `revisiones/U05-T03/`. El comando literal del criterio usa `origin/main...HEAD` y añade los dos `tareas/*.md` del commit del orquestador `36f0cd3`, que no está en main. No es desborde |

## Arbitraje de la ronda 1, comprobado
| Punto | Estado |
|---|---|
| Propiedad `synkra.kubernetes.imagen-init`, con defecto `localhost:5001/synkra-init-descarga:1.0.0` | cumple. Con una sonda en la copia arranqué `SynkraApp` sin la propiedad: el init del Deployment sale con esa imagen |
| Un valor inválido impide arrancar | cumple. Arranqué `SynkraApp` con `--synkra.kubernetes.imagen-init=` y tres valores (`localhost:5001/synkra-init-descarga:latest`, `synkra-init-descarga` y `localhost:5001/synkra-init-descarga`). En los tres casos el contexto falla y la causa nombra `synkra.kubernetes.imagen-init` |
| Nunca `:latest` ni una imagen sin tag | cumple (prueba parametrizada y mutaciones M2b y M3c) |
| `imagePullPolicy: IfNotPresent` | cumple (YAML y `usaLaImagenDeInitConfigurada`) |
| `runAsUser` y `runAsGroup` 10001, como constantes y en los dos contenedores | cumple (YAML y mutación M6) |
| F-05 y F-06 quedan para el orquestador | no se tocaron, correcto |

## Mutaciones, en la copia
Corrí las cuatro clases de `kubernetes` más `SynkraAppTest`, 63 pruebas en total.

| Mutación | Resultado |
|---|---|
| M1 · `SEGURO = .+` en `PlanPais` | atrapada (7 fallos, igual que la bitácora) |
| M2a · defecto de la imagen del init en `:latest` | atrapada (60 fallos) |
| M2b · se quita el chequeo de `:latest` | atrapada (1 fallo) |
| M3a · defecto sin registro (`synkra-init-descarga:1.0.0`) | atrapada (60 fallos) |
| M3b · registro opcional en la regex | atrapada, pero solo por el caso `init:1.0.0` |
| M3c · tag opcional en la regex | atrapada (1 fallo) |
| M5 · `POLITICA_PULL = "Always"` | atrapada (2 fallos) |
| M6 · el core sin `runAsUser`/`runAsGroup` | atrapada (1 fallo) |
| M7 · `application.properties` con `:latest` | atrapada (los 3 de `SynkraAppTest`: el contexto no arranca) |
| M4 · `ConfiguracionKubernetes` usa el constructor de un argumento e ignora la propiedad | **sobrevive** (F-02) |
| M8 · `application.properties` con otra imagen válida (`localhost:5001/otra:9.9.9`) | **sobrevive** (F-03) |

## Hallazgos

### F-01 · NARANJA · backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java:233-234 · La validación de la imagen del init acepta imágenes sin registro
El patrón `[^\s/@:]+(:[0-9]+)?(/[^\s/@:]+)+(...)` solo exige que haya una `/`. No exige que el primer segmento sea un registro. Para Docker y el kubelet, el primer segmento es un registro solo si contiene `.` o `:`, o si es `localhost`. Si no, la imagen se resuelve contra `docker.io`.

Lo que dio la sonda sobre `exigirImagenInit`:
- `synkra/init:1.0.0`: ACEPTA. Se resuelve como `docker.io/synkra/init`.
- `library/synkra-init-descarga:1.0.0`: ACEPTA. Se resuelve como `docker.io/library/...`.
- `localhost:5001/X:1`: ACEPTA. Un repositorio en mayúsculas es una referencia inválida, y el kubelet lo rechaza con `InvalidImageName`.
- `localhost/x:1`: ACEPTA. Es correcto.

El mensaje de error (`:261`), el javadoc (`:257`, `AdaptadorFabric8.java:142`), el comentario de `application.properties` y la bitácora prometen todos «registro y tag o digest». Con `SYNKRA_IMAGEN_INIT=synkra/init-descarga:1.0.0` el arranque pasa y los pods quedan en `ErrImagePull`: es justo el fallo de F-01 de la ronda 1, el que esta validación debía cerrar.

La mutación M3b solo cae por `init:1.0.0`, que no tiene `/`. Ningún caso de la prueba cubre «con `/` pero sin registro». Hace falta exigir que el primer segmento lleve `.` o `:`, o que sea `localhost`, y añadir casos rechazados como `synkra/init:1.0.0` y, de paso, un repositorio en mayúsculas.

### F-02 · NARANJA · backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java:47-50 · Que la imagen sea configurable no lo prueba nada
Todas las pruebas de la imagen construyen `AdaptadorFabric8` a mano. Ninguna pasa `synkra.kubernetes.imagen-init` a través del contexto de Spring. Con la mutación M4 (el bean llama a `new AdaptadorFabric8(cliente.getIfAvailable())` y descarta la propiedad), las 366 pruebas del backend siguen en verde. El operador perdería la propiedad que pide el arbitraje sin que nada falle.

Hace falta una prueba de contexto que lea el valor de vuelta. Por ejemplo, arrancar con `--synkra.kubernetes.imagen-init=registro.local:5000/init:2.3.4` y comprobar la imagen del init en el `ObjetosPais` que devuelve el bean. Y otra que compruebe que un valor inválido impide arrancar, con el patrón de `RegistroPermitidoTest.unValorInvalidoImpideArrancarElContexto`. Yo comprobé las dos cosas con una sonda en la copia, pero esa evidencia es mía, no del diff.

### F-03 · AMARILLO · backend/src/main/resources/application.properties:30 y ConstructorObjetosPais.java:228 · El valor por defecto está escrito dos veces
`localhost:5001/synkra-init-descarga:1.0.0` aparece como literal en `application.properties` y en `IMAGEN_INIT_POR_DEFECTO`. Si los dos se separan, nada lo detecta: con la mutación M8 todo sigue en verde. Bastaría una prueba de contexto que compare el valor efectivo con la constante (encaja con F-02), o dejar en el `.properties` solo `${SYNKRA_IMAGEN_INIT:}` y tomar el defecto del código.

## Tareas candidatas (defectos reales fuera de alcance)
- Si se quiere una sola regla de referencias de imagen en el backend, `RegistroPermitido` (catálogo, H-31) y `exigirImagenInit` (kubernetes) podrían compartir el mismo análisis de registro, repositorio y tag. Hoy son dos expresiones distintas.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/log/`:
- `ca1.txt`
- `ca2.txt`
- `ca2-xml.txt`
- `arranque.txt` (sonda de arranque y de la regex)
- `mut-*.txt`

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java:233|La validación de la imagen del init acepta imágenes sin registro (synkra/init:1.0.0 pasa y va a docker.io)
NARANJA|backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java:47|Que la imagen sea configurable no lo prueba nada (la mutación que ignora la propiedad sobrevive)
INFORME: revisiones/U05-T03/ronda-2.md
```
