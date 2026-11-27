# Ronda 3 — U05-T03

VEREDICTO: VERDE

Los tres criterios pasan con mi propia ejecución. Las dos mutaciones de la ronda 2 que sobrevivían o casi (M4 y M3b) ahora caen, y la sonda sobre `exigirImagenInit` rechaza los tres casos de F-01. La variable `SYNKRA_KUBERNETES_IMAGEN_INIT` llega de verdad a la propiedad, así que el comentario de `application.properties` dice la verdad. Queda un solo AMARILLO, que no bloquea (F-01).

Datos de la revisión:
- Rama `m6r/U05-T03` en `d54a10b`, con el código en `9f102c1`. Base `36f0cd3`.
- El hash de la tarea coincide: `1d7a200879ff2bdd2e4e19b893279beafd4831c5`.
- Corrí todo con `HOME`, `KUBECONFIG` y `-Duser.home` apuntando a archivos falsos de mi scratchpad (`u05r3/env.sh`). `GRADLE_USER_HOME=~/.gradle` solo sirvió para reutilizar los toolchains.
- No usé `kubectl` ni leí `~/.kube/config`.
- Las mutaciones y las sondas se hicieron en una copia (`git archive HEAD`) fuera del worktree. Al terminar, la copia quedó idéntica al worktree (`diff -r backend/src` sin diferencias).
- El `git status --short` del worktree sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | YAML esperado, ≥6 pruebas, sin peticiones | `./gradlew :backend:test --rerun --tests '*ConstruirObjetosPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 7s`, `tests="32" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,05 y 0,89 s, así que se ejecutaron. Están cubiertos los seis puntos que pide el criterio: el YAML de `synkra-pe`, un país sin plugins, una imagen sin digest, la guardia, el conteo de peticiones y la imagen exacta del core |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 25s`. Son 26 XML y 372 pruebas, sin ningún `skipped`, `failures` ni `errors` distinto de 0. Las líneas coinciden una por una con las que pegó la bitácora |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat origin/main...HEAD` | pasa: `d54a10b`. Contra `36f0cd3` son 15 archivos: `kubernetes/**` (main, test y recursos), `application.properties` (permitido por el arbitraje de la ronda 1), la bitácora y `revisiones/U05-T03/`. El comando literal da 19 archivos: los 4 de más (`aidlc-docs/audit.md`, `loop/README.md` y dos `tareas/*.md`) vienen del commit del orquestador `36f0cd3`, que no está en `origin/main`. No es desborde |

## Arbitraje de la ronda 2, comprobado
| Punto | Estado |
|---|---|
| El primer segmento es un registro según la regla de Docker y el repositorio va en minúsculas | cumple. Resultado de mi sonda sobre `exigirImagenInit`, en la copia: <br>- **Rechaza:** `synkra/init:1.0.0`, `library/synkra-init-descarga:1.0.0`, `localhost:5001/X:1`, `init:1.0.0`, `host:abc/x:1`, `localhost:5001//x:1`, `localhost:5001/x_:1`, `localhost:5001/x:latest`, `localhost:5001/x` y `localhost:5001/x:1 ` (con espacio al final). <br>- **Acepta:** `localhost/x:1`, `localhost:5001/synkra-init-descarga:1.0.0`, `registro.local:5000/init:2.3.4`, `synkra.io/a/b:1`, `docker.io/library/x:1`, `Registro.Local/x:1` y `localhost:5001/x@sha256:<64 hex>`. |
| La propiedad se prueba a través del contexto | cumple. `ImagenInitConfiguradaTest` lee de vuelta la imagen del init desde el bean (`registro.local:5000/init:2.3.4`) y comprueba que cinco valores inválidos impiden arrancar |
| El valor por defecto tiene un solo origen | cumple en el diff: `application.properties` ya no lleva el literal, solo el comentario, y el defecto vive en `IMAGEN_INIT_POR_DEFECTO`. Con una sonda de `SynkraApp` completa y sin la variable, el init sale con `localhost:5001/synkra-init-descarga:1.0.0`. Hay un hueco de cobertura en F-01 |

## Variable de entorno `SYNKRA_KUBERNETES_IMAGEN_INIT` (relaxed binding)
Arranqué el contexto real de `SynkraApp` en la copia (sonda en `SpringApplicationBuilder`, con `--synkra.data` temporal). La variable se exportaba al proceso de Gradle, y la sonda imprime el `System.getenv` que ve:

| Variable de entorno | Resultado |
|---|---|
| sin definir | arranca; la imagen del init es `localhost:5001/synkra-init-descarga:1.0.0` |
| `registro.local:5000/init:2.3.4` | arranca; la imagen del init es `registro.local:5000/init:2.3.4` |
| `synkra/init-descarga:1.0.0` | no arranca: `BeanCreationException`, con la causa `synkra.kubernetes.imagen-init debe llevar registro ...` |
| `localhost:5001/synkra-init-descarga:latest` | no arranca, con la misma causa |

La variable llega a la propiedad, así que el comentario de `application.properties:30` es veraz.

## Mutaciones, en la copia
Corrí `io.synkra.kubernetes.*` más `SynkraAppTest`, 69 pruebas en total. Sin mutar, salen todas en verde.

| Mutación | Resultado |
|---|---|
| M4 · el bean llama a `new AdaptadorFabric8(cliente.getIfAvailable())` e ignora la propiedad | atrapada: fallan 2 pruebas, `laPropiedadLlegaAlAdaptador` y `unValorInvalidoImpideArrancarElContexto`. Coincide con la bitácora |
| M3b · registro opcional en la regex (`"(?:" + REGISTRO + "/)?" + REPOSITORIO`) | atrapada: fallan 4 pruebas, los casos `[6] init:1.0.0`, `[9] synkra/init:1.0.0` y `[10] library/synkra-init-descarga:1.0.0`, más `unValorInvalidoImpideArrancarElContexto`. En la ronda 2 solo la atrapaba `init:1.0.0` |
| M8 · volver a escribir el defecto en `application.properties` con otra imagen válida (`localhost:5001/otra:9.9.9`) | **sobrevive**. Corrí todo el backend y solo falló `FronterasModulosTest.laDocumentacionGeneradaNoSeVersiona`, por `git check-ignore` = 128: la copia no es un repositorio git, así que ese fallo es del entorno y no detecta la mutación (F-01) |

## Hallazgos

### F-01 · AMARILLO · backend/src/test/java/io/synkra/kubernetes/ImagenInitConfiguradaTest.java:32 · `sinPropiedadSeUsaElDefectoDelCodigo` no ve `application.properties`
`ApplicationContextRunner` con `withUserConfiguration` no carga `application.properties`. Por eso la prueba fija que el defecto del `@Value` es la constante, pero no que nadie vuelva a escribir un segundo origen en el `.properties`. Con la mutación M8, ninguna prueba de la imagen falla.

Hoy el diff cumple el arbitraje en su estructura, así que esto no bloquea. Si se quiere que el arbitraje quede blindado, bastaría con añadir `ConfigDataApplicationContextInitializer` al runner, o con una aserción en `SynkraAppTest` sobre el bean.

Notas menores, que no son hallazgos:
- `localhost:5001/x:LATEST` se acepta. Es otro tag, y el kubelet solo trata `latest` en minúsculas como flotante.
- `tag@digest` se rechaza. Es más estricto que Docker, pero aceptable.

## Tareas candidatas (defectos reales fuera de alcance)
- Sigue en pie la de la ronda 2: `RegistroPermitido` (H-31) y `exigirImagenInit` podrían compartir un único análisis de registro, repositorio y tag.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r3/log/`:
- `ca1.txt` y `ca2.txt`: los criterios.
- `sonda.txt`: la sonda de la regex y de la variable de entorno.
- `sonda-gradle.txt`: la salida de Gradle de esa sonda.
- `mut-base.txt`, `mut-M4.txt`, `mut-M3b.txt` y `mut-M8.txt`: las mutaciones.

El código fuente de la sonda está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r3/sondas/SondaR3Test.java`.

```
VEREDICTO: VERDE
AMARILLO|backend/src/test/java/io/synkra/kubernetes/ImagenInitConfiguradaTest.java:32|La prueba del defecto no carga application.properties (la mutación M8 sobrevive)
INFORME: revisiones/U05-T03/ronda-3.md
```
