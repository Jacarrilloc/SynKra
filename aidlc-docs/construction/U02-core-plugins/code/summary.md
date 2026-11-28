# U02 Core, contrato y plugins de país: resumen de la unidad

Unidad: U02 (`plugin-api/`, `core/`, `plugins/kyc-co/` y `plugins/kyc-pe/`, paquete `io.synkra`). Historia dueña de la unidad: H-21, más H-22 y H-23; contribuye a H-10 y H-28 (plan de la unidad y `tareas/U02-T13-resumen-unidad.md`).
Este resumen lo escribe U02-T13 sin cambiar código, pruebas, build ni scripts. Las salidas de abajo se produjeron en esta tarea, el 2026-11-27 (UTC 2026-11-28 00:20 a 00:21), con `--rerun`, sobre la rama `m6r/U02-T13` en el sha `63d99fa` (la punta de U02-T12; esta tarea solo añade documentos). Cada archivo de `evidencia/` (junto a este resumen) trae su fecha, ese sha, el comando exacto y la salida completa. Nada se corrió contra un clúster (AUTONOMIA-01): la prueba de humo usa Docker local.

## Qué expone

- **`plugin-api/`** (T01, sin dependencias): `SynkraPlugin` (`nombre`, `iniciar`, `detener`), `ContextoPlugin`, `PuntoKyc`, `SolicitudKyc` y `ResultadoKyc`. `evidencia/dependencias-plugin-api.txt` muestra `No dependencies` en el `runtimeClasspath`.
- **Core** (`core/`, Spring Boot, T02 a T08): la versión `2.1.0` va en `synkra-core.properties` dentro del JAR, escrita al construir, no leída del entorno (T02). `CargadorPlugins` da un `URLClassLoader` por plugin (T03). `ArranquePlugins` lee `/config/plugins-activos.yaml` y `/plugins`, verifica la selección con el `Verificador` de U01 contra la versión compilada y activa; si el veredicto es `RECHAZADA` el proceso termina con código distinto de 0 y el motivo (por ejemplo `VERSION_INCOMPATIBLE plugin=kyc-pe`) en el log; los plugins presentes y no activos no se cargan (T04). La suite de U01 corre contra el arranque (T05). Un fallo en `iniciar` deja la readiness en 503 (`OUT_OF_SERVICE`) y el log nombra el plugin (T06). El core no contiene nombres de países (T08, P3).
- **Endpoints y salud** (T06 y T07): `GET /plugins` (nombre y versión de los activos), `POST /kyc/{plugin}` (delega en el `PuntoKyc` con el cargador del plugin como contexto) y `/actuator/health/readiness`, que incluye el grupo `plugins` (`management.endpoint.health.group.readiness.include=readinessState,plugins`).
- **Plugins** (T09 y T10): `kyc-co 1.0.0` (`pais: co`, core `2.x`), `kyc-pe 1.3.0` (`pais: pe`, core `2.x`) y `kyc-pe 2.0.0` (`pais: pe`, core `3.x`, para la demostración de CU4). Los dos `kyc-pe` salen del mismo código y de una misma plantilla de descriptor; `evidencia/descriptores.txt` muestra el `synkra-plugin.yaml` de cada JAR.
- **Imagen** (T11): `synkra-core:2.1.0`, `core/Dockerfile` de dos etapas (extrae capas del `core-*.jar` ya compilado en el host), base `eclipse-temurin:21-jre-alpine` fijada por digest (el plan de la unidad hablaba de distroless; el Dockerfile aceptado usa esta base), usuario numérico `10001:10001` (C-72), `/plugins` y `/config` como volúmenes, puerto 8080.

## Cómo se prueba

- **Suite de U01 contra el arranque:** `ConcordanciaSuiteTest` (T05) toma `SuiteCombinaciones.casos()` de U01 y compara el arranque del core con el veredicto esperado (H-10); S5 no se puede ejecutar contra el core 2.1.0 (C-84).
- **Pruebas por módulo:** `:core:check`, `:plugins:kyc-co:check` y `:plugins:kyc-pe:check` (89 pruebas, ver abajo).
- **Contenedor local:** `core/smoke.sh` (T12) hace `docker run` de la imagen con `/plugins` y `/config` montados desde un directorio temporal: con `kyc-pe 1.3.0` activo comprueba readiness 200 y `GET /plugins`; con `kyc-pe 2.0.0` activo comprueba que el core termina con código distinto de 0 y que el log trae `VERSION_INCOMPATIBLE plugin=kyc-pe`. El formato de `plugins-activos.yaml` es el canónico del core (C-81).
- **Nada contra un clúster.**

## Salidas reales

### `./gradlew ... check` de los tres módulos

Comando exacto de `evidencia/gradle-check.txt` (fecha y sha en sus primeras líneas) y sus últimas 6 líneas, copiadas literalmente con `tail -6`:

```
$ ./gradlew :core:test --rerun :core:check :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-pe:test --rerun :plugins:kyc-pe:check; echo "salida=$?"
[...]
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 11s
23 actionable tasks: 23 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

Conteo por clase, de los XML de `build/test-results/test/` de cada módulo (`evidencia/conteo-por-clase.txt`, copiado sin sus cuatro líneas de cabecera):

```
== core
io.synkra.core.ArranquePluginsTest tests="10" skipped="0" failures="0" errors="0"
io.synkra.core.CargadorPluginsTest tests="10" skipped="0" failures="0" errors="0"
io.synkra.core.ConcordanciaSuiteTest tests="10" skipped="0" failures="0" errors="0"
io.synkra.core.CoreSinPaisesTest tests="1" skipped="0" failures="0" errors="0"
io.synkra.core.EndpointsCoreTest tests="12" skipped="0" failures="0" errors="0"
io.synkra.core.FalloInicioPluginTest tests="7" skipped="0" failures="0" errors="0"
io.synkra.core.VersionCoreTest tests="4" skipped="0" failures="0" errors="0"
== plugins/kyc-co
io.synkra.kycco.KycCoTest tests="17" skipped="0" failures="0" errors="0"
== plugins/kyc-pe
io.synkra.kycpe.KycPeTest tests="18" skipped="0" failures="0" errors="0"
```

Suman 89 pruebas (54 del core, 17 de `kyc-co` y 18 de `kyc-pe`), sin saltos ni fallos. `plugin-api/` no tiene pruebas propias: su criterio es el de dependencias (`evidencia/dependencias-plugin-api.txt`).

### Prueba de humo del contenedor

Con el JAR del core, los JAR de `kyc-pe` y la imagen ya construidos (`evidencia/construir-jar.txt` e `evidencia/imagen.txt`). Archivo completo `evidencia/smoke.txt`, copiado sin sus tres líneas de cabecera:

```
$ core/smoke.sh; echo "salida=$?"
ok caso1-kyc-pe-1.3.0
ok caso2-kyc-pe-2.0.0
salida=0
```

### Imagen del core

Comando y últimas 4 líneas de `evidencia/imagen.txt` (el `docker build` completo está en el archivo), copiadas con `tail -4`:

```
$ docker build --progress=plain -t synkra-core:2.1.0 core/; echo "salida=$?"; docker inspect -f "{{.Config.User}} {{json .Config.Entrypoint}} {{json .Config.ExposedPorts}}" synkra-core:2.1.0
[...]
#14 unpacking to docker.io/library/synkra-core:2.1.0 done
#14 DONE 0.0s
salida=0
10001:10001 ["java","org.springframework.boot.loader.launch.JarLauncher"] {"8080/tcp":{}}
```

Usuario numérico no root (`10001:10001`, C-72), puerto 8080 y entrypoint `JarLauncher`.

### Versión del core y descriptores de los JAR

`evidencia/descriptores.txt`, copiado sin sus tres líneas de cabecera:

```
$ unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties; for j in plugins/kyc-co/build/libs/kyc-co-1.0.0.jar plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar; do echo "== $j"; unzip -p $j synkra-plugin.yaml; done
version=2.1.0
== plugins/kyc-co/build/libs/kyc-co-1.0.0.jar
nombre: kyc-co
version: 1.0.0
core: synkra-core
coreCompatible: 2.x
etiquetas:
  pais: co
descripcion: Validación KYC simulada para Colombia
== plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar
nombre: kyc-pe
version: 1.3.0
core: synkra-core
coreCompatible: 2.x
etiquetas:
  pais: pe
descripcion: Validación KYC simulada para Perú
== plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar
nombre: kyc-pe
version: 2.0.0
core: synkra-core
coreCompatible: 3.x
etiquetas:
  pais: pe
descripcion: Validación KYC simulada para Perú
```

## Trazabilidad

Sigue la del plan (`U02-core-plugins-code-generation-plan.md`, sección Trazabilidad) y los campos «Historias que implementa» de las tareas; no se amplió.

| Historia | Tareas que la cubren |
|---|---|
| H-21 El core verifica al arrancar (dueña) | U02-T01 (contrato), U02-T02 (versión compilada), U02-T04 (arranque), U02-T05 (suite), U02-T07 (endpoints), U02-T08 (sin países), U02-T12 (humo) y U02-T13 (este resumen) |
| H-22 Un plugin que falla no deja el core a medias (dueña) | U02-T03 (un cargador por plugin), U02-T06 (fallo al iniciar y readiness) |
| H-23 Plugins de Colombia y Perú (dueña) | U02-T09 (`kyc-co 1.0.0`), U02-T10 (`kyc-pe 1.3.0` y `2.0.0`) |
| H-28 Imágenes no root (contribuye) | U02-T11 (imagen del core) |
| H-10 La suite bloquea el build (contribuye) | U02-T05 (la suite de U01 aplicada al arranque) |

## Tareas

| Tarea | Qué construyó | PR | Rondas de revisión |
|---|---|---|---|
| U02-T01 | `plugin-api/`: contrato sin dependencias | #69 (fusionado) | 1 |
| U02-T02 | Proyecto `core/` con la versión `2.1.0` compilada | #72 (fusionado) | 2 |
| U02-T03 | `CargadorPlugins`: un `URLClassLoader` por plugin | #75 (fusionado) | 2 |
| U02-T04 | `ArranquePlugins`: leer, verificar con U01 y activar | #79 (fusionado) | 2 |
| U02-T05 | La suite de U01 aplicada al arranque | #80 (fusionado) | 2 |
| U02-T06 | Fallo al iniciar un plugin y readiness | #82 (fusionado) | 3 |
| U02-T07 | Endpoints del core | #84 (abierto, base `main`) | 2 |
| U02-T08 | El core no conoce países | #86 (abierto, apilado sobre T07) | 1 |
| U02-T09 | Plugin `kyc-co 1.0.0` | #88 (abierto, apilado sobre T08) | 2 |
| U02-T10 | Plugin `kyc-pe` 1.3.0 y 2.0.0 | #89 (abierto, apilado sobre T09) | 1 |
| U02-T11 | Imagen del core, no root | #91 (abierto, apilado sobre T10) | 1 |
| U02-T12 | Prueba de humo del contenedor | #93 (abierto, apilado sobre T11) | 3 |
| U02-T13 | Este resumen | este PR (U02-T13) | (pendiente) |

Las rondas son los informes `revisiones/U02-T<NN>/ronda-N.md` (sin contar `ronda-N-respuesta.md`), contados con `ls`; la última ronda de cada tarea terminó en VERDE (las anteriores, cuando las hay, en NO-VERDE). Los PR salen de `git log --merges` (T01 a T06) y de `gh pr list` (T07 a T12). Los PR #68, #73 y #74 son de la rama del orquestador (`orq/U05-U02-tareas`), no de tareas.

## Candidatas abiertas que nombran U02

Tomadas de `CANDIDATAS.md` completo (105 candidatas, C-01 a C-105, rama `orq/U05-U02-tareas` en `b28e7b0`), barrido por «U02» en cualquier columna y revisado a mano. Ninguna fila de U02 trae marca de «Cerrada» o «Resuelta».

- **Pulido de U01/U02 (tarea sin asignar):**
  - C-74 (`CargadorPlugins` delega padre primero; falta la prueba del cierre en la ruta «más de una implementación» y documentar la costura `cargar(Path, ClassLoader)`).
  - C-77 (lector YAML seguro compartido en `verificador/`), junto con C-80 (límite de alias sin prueba; el YAML se analiza tres veces) y C-88 (las copias del lector ya divergieron; nace en U05-T06).
  - C-78 (versión de `snakeyaml-engine` repetida en tres `build.gradle.kts`).
  - C-84 (S5 no se ejecuta contra el core 2.1.0; texto `remiteA` de S12; NPE ante un plugin sin JAR).
  - C-85 y C-105 (evidencia: transcripciones en un scratchpad, `f06.txt` y `senal.txt` resumidos).
  - C-86 (los plugins iniciados no reciben `detener()` al cerrar el contexto).
  - C-89 (Javadoc de `IniciadorPlugins` y afirmación del agregado por `status`).
  - C-96 (`control-core-sin-paises.sh` frente a `CoreSinPaisesTest`).
  - C-99 y C-100 (rama de documento nulo de los plugins sin prueba; evidencia de CA-3 de T10).
- **Sin destino asignado:** C-91 (`POST /kyc/{plugin}` sin tope de cuerpo propio) y C-92 (las rutas fuera de las decisiones de T07 responden con el cuerpo de error por defecto de Spring).
- **Pulido de imágenes (U08):** C-101 y C-103 (digest en `backend/Dockerfile` y literales repetidos en las tres imágenes), con C-98 (nace en U05-T08).
- **Nacidas en U05 y que tocan el core:** C-81 (contrato `plugins-activos.yaml`; la decisión dice que se arregló en U05-T06 y U02-T12 usa el formato canónico, sin marca de cierre en el archivo), C-94 (el core monta `/plugins` con escritura), C-97 (`emptyDir` de `/plugins` escribible para el UID 10001) y C-90 (`leerPais` más laxo que el core).
- **Aceptadas como reglas ya aplicadas, sin trabajo pendiente:** C-29 (plantilla de resúmenes: este resumen da la cola de `tail -6` y lo demás por archivo), C-69 (`:X:test --rerun :X:check`, aplicado en el CA-1 de esta tarea), C-71 (`SYNKRA_CORE_VERSION=9.9.9` y los `--stat` con su sha) y C-72 (contratos de las imágenes, cumplidos por T11; C-103 recoge lo que falta).

## Candidatas nuevas de esta tarea

Ninguna.
