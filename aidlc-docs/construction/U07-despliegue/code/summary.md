# Resumen de U07: Despliegue y reversión

**Unidad:** U07, módulo `despliegue` (`io.synkra.despliegue`), tareas de código U07-A a U07-G (tareas del plan U07-T01 a U07-T13).
**Historias:** dueñas H-09, H-11, H-12, H-13, H-14, H-16, H-17 y H-20; contribuye a H-06 (U07-T11) y H-08 (U07-T03); el plan nombra además H-24 como contribución (ver «Trazabilidad»).
**Fecha de las corridas:** 2026-11-27. **sha de las corridas:** ba0b1fd, el último commit que toca `backend/` (`git log -1 --format=%h -- backend`); los commits posteriores solo traen documentación y evidencia. Base de la tarea: `2f47340` (`main` con U07-A a U07-F fusionadas).
**Lo único que tocó U07-G en código:** `backend/src/main/java/io/synkra/despliegue/package-info.java` y pruebas añadidas a `backend/src/test/java/io/synkra/FronterasModulosTest.java`.
**Clúster:** nada se corrió contra un clúster (AUTONOMIA-01): ni `kubectl` ni clúster real; solo el servidor simulado de Fabric8 y dobles de Mockito.
**Entorno:** `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (Java 21), x86_64, sin `sqlite3` (lecturas de vuelta con `python3 -I`).

## Qué expone

Paquete `io.synkra.despliegue`, **sin paquete `api` y sin clases públicas**: nadie fuera del módulo lo usa; las rutas HTTP son su única cara. Migración `V4__despliegue.sql` (U07-A): tabla `dep_plan` con sus CHECK (estado, tipo y origen juntos, JSON válido, `vence_en > creado_en`, país en minúscula), el índice `dep_plan_pais_estado` y `origen_id` sin clave foránea. Las migraciones de la punta:

```
V1__base.sql
V2__catalogo.sql
V3__registro.sql
V4__despliegue.sql
```

Clases de `backend/src/main/java/io/synkra/despliegue/` (salida de `ls`):

```
CalculoCambios.java
CambioCore.java
Cambios.java
CodigosDespliegue.java
ConfirmacionServicio.java
GuardaConfirmacion.java
MotivosDespliegue.java
PaisPermitido.java
Plan.java
PlanPreparado.java
PlanRepositorio.java
PlanServicio.java
PluginPedido.java
PluginResumen.java
ReservaConfirmacion.java
Resumen.java
ResumenCatalogo.java
RutasConfirmacion.java
RutasPlanes.java
RutasReversion.java
RutasVerificacion.java
TareasProgramadas.java
Usuario.java
VencimientoPlanes.java
VeredictoDto.java
VerificacionVivo.java
VigilanciaDespliegues.java
package-info.java
```

Rutas (salida de `grep -rn 'Mapping(' backend/src/main/java/io/synkra/despliegue`); `RutasPlanes` y `RutasVerificacion` llevan la ruta base en `@RequestMapping` y sus métodos la completan:

```
backend/src/main/java/io/synkra/despliegue/RutasReversion.java:24:    @PostMapping("/api/despliegues/{id}/reversion")
backend/src/main/java/io/synkra/despliegue/RutasConfirmacion.java:28:    @PostMapping(RUTA_DESPLIEGUES + "/{id}/confirmacion")
backend/src/main/java/io/synkra/despliegue/RutasVerificacion.java:11:@RequestMapping("/api/verificaciones")
backend/src/main/java/io/synkra/despliegue/RutasPlanes.java:18:@RequestMapping("/api/despliegues")
```

Las cuatro rutas de la unidad: `POST /api/verificaciones` (R-D2, U07-B), `POST /api/despliegues` (crear plan, R-D3 y R-D4, U07-B), `POST /api/despliegues/{id}/confirmacion` (U07-C la dejó con 501 temporal; la confirmación real es de U07-D) y `POST /api/despliegues/{id}/reversion` (U07-F). El `grep` no trae ninguna ruta de más.

Lo que U07 añadió fuera de `despliegue`, con la tarea que lo trajo (salida de `git diff --name-status <padre 1>...<padre 2>` de cada fusión, en `evidencia/datos.txt`):

- U07-A: `V4__despliegue.sql` y la propiedad `synkra.paises=${SYNKRA_PAISES:co,pe}` en `application.properties`.
- U07-B: el ayudante de prueba `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java` (servidor simulado de Fabric8 en modo CRUD, con `fallaLecturas`, `siembra` y `nuevasPeticiones`); lo ampliaron U07-D (`aceptaAplicar`) y U07-E (campo `respuestas`, `marcaListo`, `marcaFallo`), siempre con líneas añadidas (`bitacoras/U07-D.md`, desviación 1; `bitacoras/U07-E.md`, desviaciones 1, 2 y 4).
- U07-C: `plataforma/Reloj.java` (bean `Clock`, con `@Configuration("configuracionReloj")`) y `plataforma/RelojTest.java`.
- U07-D: solo una ampliación de `SimuladorKubernetes` y nada más fuera de `despliegue`.
- U07-E: `RegistroApi.aplicando()` y su implementación en `RegistroServicio` (C-116); en `kubernetes`, `ConfiguracionKubernetes` (`.withRequestRetryBackoffLimit(0)`, C-76) y `LectorPais` (`estado` juzga la versión nueva, C-82; `contexto` junta eventos del Deployment y sus ReplicaSet, C-83) (`bitacoras/U07-E.md`, «Cambio (commit 4869522)»); en `application.properties` las propiedades `synkra.programacion.activa`, `synkra.vigilancia.intervalo`, `synkra.vigilancia.plazo` y `synkra.planes.barrido`, y `backend/src/test/resources/config/application.properties` con `synkra.programacion.activa=false`; las pruebas nuevas `ContextoEventosTest`, `EstadoRolloutTest`, `ReintentosClienteTest`, el ayudante `LecturasDePrueba` y `registro/AplicandoTest`.
- U07-F: nada fuera de `despliegue`: el `git diff` de su fusión no lista ninguna ruta fuera de ese módulo.

Reglas R-D1 a R-D13 contra la clase de prueba que las ata (nombres comprobados con `ls backend/src/test/java/io/synkra/despliegue/`):

| Regla | Qué fija | Clase de prueba |
|---|---|---|
| R-D1 | país permitido (`synkra.paises`) y namespace `synkra-<país>` | `PaisPermitidoTest` (y `ReversionTest` para el país del origen) |
| R-D2 | verificar y alternativas sobre el catálogo, sin crear nada | `VerificacionVivoTest` |
| R-D3 | crear plan: R-D1, luego R-D2; si se rechaza no se guarda nada | `CrearPlanTest` (y `ReversionTest`) |
| R-D4 | resumen y cambios | `CrearPlanTest`, `CambiosPropiedades` (propiedad PBT-03), `PlanRepositorioTest` (esquema) |
| R-D5 | confirmar solo un plan `pendiente` y vigente; 409 o 410 | `PlanPendienteTest`, `VencimientoPlanesTest`, `ConfirmarTest` |
| R-D6 | volver a verificar al confirmar | `ConfirmarTest` (casos `ar` y `uy`) |
| R-D7 | un solo `aplicando` por país | `ConfirmarTest` (casos `co`, `cl`, `ec`) |
| R-D8 | `creado_por` y `confirmado_por` (`X-Synkra-Usuario`) | `ConfirmarTest`, `ConfirmarSinClusterTest` |
| R-D9 | confirmar = construir y aplicar, registrar `aplicando`, 202 | `ConfirmarTest`, `ConfirmarSinClusterTest` |
| R-D10 | vigilancia sin reintentos hasta `listo` o `fallido` | `VigilanciaTest`, `VigilanciaCoincideTest`, `VigilanciaDobleTest` |
| R-D11 | reversión como plan nuevo | `ReversionTest` |
| R-D12 | retomar la vigilancia tras un reinicio | `RetomarVigilanciaTest` |
| R-D13 | un plan pendiente nunca llama a `aplicar` | `PlanPendienteTest` (el criterio de confirmar sin clúster, además, en `ConfirmarSinClusterTest`) |

La columna «Qué fija» resume `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md`; la clase se ata por los comentarios de las pruebas (`grep -o 'R-D[0-9]*'` sobre `backend/src/test/java/io/synkra/despliegue/`) y por los nombres de caso de `ConfirmarTest`.

## Fronteras

Lo que fija U07-G (U07-T12):

1. `despliegue/package-info.java` declara `@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})`, igual que `registro` (U06-C). Sin esa línea Modulith permite usar el paquete base de otro módulo (la interfaz sin nombre, que siempre existe) y la mutación «interno de registro» no cae (`bitacoras/U07-G-evidencia/ca2-hueco.txt`).
2. `verificador` **no** va en `allowedDependencies`: es una biblioteca compartida (`@Modulithic(sharedModules = "verificador")`, C-36, prueba `verificadorEsModuloCompartido`), un proyecto Gradle aparte y no un módulo del backend con fronteras propias. Modulith suma los módulos compartidos a las dependencias permitidas, y `uniqueModules()` lo devuelve entre los destinos de `despliegue`.
3. Tres pruebas nuevas en `FronterasModulosTest`, solo líneas añadidas: `despliegueSoloDependeDeSusModulosPermitidos` (destinos exactos `catalogo`, `kubernetes`, `plataforma`, `registro`, `verificador`), `despliegueSoloUsaLasApisDeOtrosModulos` (ArchUnit: ningún tipo de `io.synkra` fuera de `despliegue`, de las API de los tres módulos, de `plataforma` y de `verificador`, ni de `io.fabric8`) y `ningunModuloDependeDeDespliegue` (ArchUnit sobre el código de producción, incluido el paquete raíz).
4. `despliegue` no expone nada (sin `api`, sin clases públicas).

Salida de `grep -n allowedDependencies` sobre los dos `package-info.java`:

```
backend/src/main/java/io/synkra/despliegue/package-info.java:2:@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})
backend/src/main/java/io/synkra/registro/package-info.java:2:@ApplicationModule(allowedDependencies = {"kubernetes :: api", "plataforma"})
```

Lista de `testcase` de `FronterasModulosTest` de la corrida fresca (`bitacoras/U07-G-evidencia/ca1.txt`, mismo sha ba0b1fd):

```
testcase name="laReglaDeApiMuerdeConUnaFirmaPublicaConFabric8()"
testcase name="soloElModuloKubernetesImportaFabric8()"
testcase name="laReglaDeFabric8FueraDeKubernetesMuerdeConUnaClaseDeJuguete()"
testcase name="generaLaDocumentacionDeModulos()"
testcase name="elBackendRealRespetaSusFronteras()"
testcase name="despliegueSoloDependeDeSusModulosPermitidos()"
testcase name="laApiPublicaDeKubernetesNoMencionaFabric8()"
testcase name="laDocumentacionGeneradaNoSeVersiona()"
testcase name="laApiDeRegistroSoloMencionaTiposPropios()"
testcase name="ningunModuloDependeDeDespliegue()"
testcase name="verifyFallaCuandoUnModuloImportaUnPaqueteInternoDeOtro()"
testcase name="verificadorEsModuloCompartido()"
testcase name="registroSoloDependeDeKubernetesApiYPlataforma()"
testcase name="despliegueSoloUsaLasApisDeOtrosModulos()"
testcase name="registroNoTieneRutasDeEscritura()"
```

Desviación del plan: U07-T12 dice «solo usa `catalogo.api`, `kubernetes.api` y `registro.api`»; en realidad también usa `plataforma` (como `registro`) y la biblioteca compartida `verificador` (ver «Desviaciones del diseño», punto 27). Nota de diseño del revisor de U07-F (`revisiones/U07-F/ronda-3.md`, «Tareas candidatas»): R-D1 se comprueba dos veces en la reversión, una en el paso explícito de `PlanServicio.prepararReversion` y otra dentro de `ResumenCatalogo.armar`; el paso explícito solo se distingue por el orden de los errores (R-D1 antes de R-D3), y por eso la prueba `mx` de `ReversionTest` lleva un origen incompatible (`bitacoras/U07-F.md`, «Ajuste de dato en la prueba»).

## Cómo se prueba

Sin clúster ni `kubectl` (AUTONOMIA-01):

- El servidor simulado de Fabric8 en modo CRUD (`SimuladorKubernetes`), con `aceptaAplicar(pais)` para el server-side apply que ese modo no entiende (C-121), o un doble de Mockito (`ConfirmarSinClusterTest`, `PaisPermitidoTest`, `VigilanciaDobleTest`).
- El reloj de prueba `RelojMovible` sobre el bean `Clock` de `plataforma/Reloj.java`.
- La programación apagada en las pruebas (`synkra.programacion.activa=false`, U07-E, C-117) y la vigilancia llamada a mano (`revisar()`).
- SQLite en `build/sqlite-pruebas/<Clase>`; las lecturas de vuelta, con `python3 -I`, sin `sqlite3`.
- Mutaciones en copias (`git archive` más `git init -q`): cada tarea y cada revisión las lista; U07-G demuestra las siete de fronteras en `bitacoras/U07-G-evidencia/ca2.txt`.

El filtro `*despliegue*` casa también con nombres de método, no solo con clases de `io.synkra.despliegue.*`: toma además dos pruebas de `FronterasModulosTest` (paquete `io.synkra`), `despliegueSoloDependeDeSusModulosPermitidos` y `despliegueSoloUsaLasApisDeOtrosModulos`; `ningunModuloDependeDeDespliegue` no entra porque lleva la `D` en mayúscula. **No** toma las demás pruebas de esa clase ni las pruebas que U07 añadió en otros paquetes: `plataforma.RelojTest`, `registro.AplicandoTest` y las de `kubernetes` de U07-E (`ContextoEventosTest`, `EstadoRolloutTest`, `ReintentosClienteTest`). Esas salen en la segunda corrida y en `:backend:check`.

## Salidas reales

Tres corridas con `JAVA_HOME` en Java 21, sobre el sha `ba0b1fd`, con su archivo completo (fecha, sha, `JAVA_HOME`, comando y salida) en `aidlc-docs/construction/U07-despliegue/code/evidencia/`. Lo pegado es `tail -6` o líneas copiadas tal cual.

**Corrida 1**, `./gradlew :backend:test --rerun --tests '*despliegue*'; echo "salida=$?"` (`gradle-despliegue.txt`):

```
$ tail -6 gradle-despliegue.txt
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 20s
9 actionable tasks: 1 executed, 8 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

Conteo por clase de todos los XML que produce esa corrida (`conteo-por-clase-despliegue.txt`; no es un glob por nombre de archivo, porque `FronterasModulosTest` entra por el nombre de sus métodos) y suma de `tests` (100, con las dos de `FronterasModulosTest`):

```
despliegue.CambiosPropiedades tests="4" skipped="0" failures="0" errors="0"
despliegue.CarpetaEquivocadaPlanTest tests="3" skipped="0" failures="0" errors="0"
despliegue.ConfirmarSinClusterTest tests="2" skipped="0" failures="0" errors="0"
despliegue.ConfirmarTest tests="10" skipped="0" failures="0" errors="0"
despliegue.CrearPlanTest tests="9" skipped="0" failures="0" errors="0"
despliegue.PaisPermitidoTest tests="6" skipped="0" failures="0" errors="0"
despliegue.PlanPendienteTest tests="9" skipped="0" failures="0" errors="0"
despliegue.PlanRepositorioTest tests="12" skipped="0" failures="0" errors="0"
despliegue.RetomarVigilanciaTest tests="2" skipped="0" failures="0" errors="0"
despliegue.ReversionTest tests="9" skipped="0" failures="0" errors="0"
despliegue.VencimientoPlanesTest tests="2" skipped="0" failures="0" errors="0"
despliegue.VerificacionVivoTest tests="15" skipped="0" failures="0" errors="0"
despliegue.VigilanciaCoincideTest tests="3" skipped="0" failures="0" errors="0"
despliegue.VigilanciaDobleTest tests="4" skipped="0" failures="0" errors="0"
despliegue.VigilanciaTest tests="8" skipped="0" failures="0" errors="0"
FronterasModulosTest tests="2" skipped="0" failures="0" errors="0"
$ cat backend/build/test-results/test/TEST-*.xml | grep -o -h '<testsuite [^>]*' | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' | paste -sd+ | bc
100
```

**Corrida 2**, las pruebas que el filtro `*despliegue*` no toma (`gradle-otros-paquetes.txt`): `./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*RelojTest' --tests '*AplicandoTest' --tests '*ContextoEventosTest' --tests '*EstadoRolloutTest' --tests '*ReintentosClienteTest'; echo "salida=$?"`. Esta corrida repite las dos de `FronterasModulosTest` del filtro de la corrida 1 y trae las 15 de la clase. Los nombres de las clases de `kubernetes` salen de `git diff --name-status --diff-filter=A 39a242c...555acad -- backend/src/test/java/io/synkra/kubernetes` (base de U07-E y punta VERDE de U07-E), que dio:

```
A	backend/src/test/java/io/synkra/kubernetes/ContextoEventosTest.java
A	backend/src/test/java/io/synkra/kubernetes/EstadoRolloutTest.java
A	backend/src/test/java/io/synkra/kubernetes/LecturasDePrueba.java
A	backend/src/test/java/io/synkra/kubernetes/ReintentosClienteTest.java
```

`LecturasDePrueba` es un ayudante sin `@Test`; las otras tres existen en el árbol (`ls`). Salida:

```
$ tail -6 gradle-otros-paquetes.txt
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 12s
9 actionable tasks: 1 executed, 8 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

```
FronterasModulosTest tests="15" skipped="0" failures="0" errors="0"
kubernetes.ContextoEventosTest tests="3" skipped="0" failures="0" errors="0"
kubernetes.EstadoRolloutTest tests="6" skipped="0" failures="0" errors="0"
kubernetes.ReintentosClienteTest tests="3" skipped="0" failures="0" errors="0"
plataforma.RelojTest tests="1" skipped="0" failures="0" errors="0"
registro.AplicandoTest tests="1" skipped="0" failures="0" errors="0"
```

**Corrida 3**, todo el backend, `./gradlew :backend:test --rerun :backend:check; echo "salida=$?"` (`gradle-check.txt`):

```
$ tail -6 gradle-check.txt
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 48s
9 actionable tasks: 1 executed, 8 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
salida=0
```

Recuento de XML (`conteo-xml.txt`): cuántos archivos, cuántos con `skipped`, `failures` o `errors` distinto de 0, y la suma de `tests`:

```
$ ls backend/build/test-results/test/TEST-*.xml | wc -l
60
$ grep -L 'skipped="0" failures="0" errors="0"' backend/build/test-results/test/TEST-*.xml | wc -l
0
$ cat backend/build/test-results/test/TEST-*.xml | grep -o -h '<testsuite [^>]*' | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' | paste -sd+ | bc
623
```

Las cifras de pruebas y de XML de las revisiones de U07-A a U07-F se citan en «Cifras y su fuente» con su fuente y **no** se presentan como las de hoy. Una cifra que la tarea de U07-G da como ejemplo para `main` `39a242c` no aparece en ningún archivo de revisiones ni de bitácoras de U07; no se usa. Las de la corrida fresca son las de `evidencia/`. La cifra fresca de XML (60) coincide con la que dio el revisor de U07-F en su ronda 3; las de pruebas por clase difieren de las de las primeras rondas en las clases que crecieron después: `ConfirmarSinClusterTest` (un caso nuevo en la ronda 2 de U07-D, `bitacoras/U07-D.md`), `VencimientoPlanesTest` (nueva en la ronda 2 de U07-C) y `VigilanciaCoincideTest` y `VigilanciaDobleTest` (nuevas en la ronda 2 de U07-E). No se ajustó nada para que cuadre.

## Desviaciones del diseño

Cada una dice qué decía el diseño, el plan o la tarea, qué se hizo, quién lo anotó y si el revisor la aceptó.

1. **U07-A, C-30.** El plan de U07 nombraba a una persona en su línea 3. Se reescribió como «aviso previo al responsable del proyecto». Anotó: `bitacoras/U07-A.md`, «Cambio». El informe de U07-A (`revisiones/U07-A/ronda-1.md`) lo verifica en CA-6 («la línea 3 del plan (C-30)») y no lo discute.
2. **U07-A, una prueba menos de la prevista.** La prueba de instante de ida y vuelta se fundió con la de alta para respetar un país por prueba. Anotó: `bitacoras/U07-A.md`, «Cambio», última nota. El informe no la comenta.
3. **U07-B, `leerPais` en vez del último `listo`.** R-D4 dice que los cambios se calculan «contra el último `listo` del país (U06)»; U07-B toma lo que corre de `AdaptadorKubernetes.leerPais`. Anotó: `bitacoras/U07-B.md`, «Desviación del diseño (decisión 2)». El informe no la comenta como desviación; solo observa que «la única llamada a él es `leerPais`». U07-D a U07-F la mantienen: en la punta, `grep -rn leerPais backend/src/main/java/io/synkra/despliegue` da `PlanServicio.java:120` y `VigilanciaDespliegues.java:117` (ver «Qué expone», `evidencia/datos.txt`).
4. **U07-B, `PlanDto` con `Plan.PENDIENTE`.** `PlanDto` respondía `estado` con la constante `Plan.PENDIENTE` para esquivar el `grep` de escrituras (C-108). Anotó: `bitacoras/U07-B.md`, «Notas de diseño y desviaciones». El revisor, F-02 AMARILLO: «No esconde nada hoy» (`revisiones/U07-B/ronda-1.md`). U07-D volvió a `plan.estado()`: «`PlanDto.de` usa `plan.estado()` (C-108)» (`bitacoras/U07-D.md`, «Cambio»).
5. **U07-B, el `grep` «ajenos» de U07-A ya no aplica.** `despliegue` usa `catalogo.api` y `kubernetes.api` a propósito. Anotó: `bitacoras/U07-B.md`. El informe no la comenta.
6. **U07-C, confirmar respondía 501 temporal.** Hasta U07-D, `POST .../confirmacion` de un plan vigente daba 501 `CONFIRMACION_NO_DISPONIBLE`. Anotó: `bitacoras/U07-C.md` (alcance y «Cambio»); el revisor lo comprueba en el JAR real (`revisiones/U07-C/ronda-2.md`, criterio 3: «410 dos veces, luego 501, 409 y 404»). U07-D lo quitó (`tareas/U07-D-confirmar.md`, decisión 10 y CA-4: «no queda rastro del 501») y la bitácora de U07-D dice «se quitó `CONFIRMACION_NO_DISPONIBLE`».
7. **U07-C, bean `Clock` con nombre explícito.** `plataforma/Reloj.java` es `@Configuration("configuracionReloj")`: sin nombre, el bean de la clase se llama `reloj` y choca con el del método. Anotó: `bitacoras/U07-C.md`, «Cambio». Revisor: «`@Configuration("configuracionReloj")`: aceptado» (`revisiones/U07-C/ronda-1.md`). `RegistroServicio` sigue con su reloj privado (C-113).
8. **U07-D, R-D9: `aplicando` antes de aplicar.** El diseño decía confirmar = construir y aplicar, y después registrar; se registra `aplicando` **antes** de aplicar. Aprobada por el responsable del proyecto; fuente: `bitacoras/U07-D.md`, desviación 4. Revisor: sobre la desviación de R-D9, «está aprobada y documentada» (`revisiones/U07-D/ronda-1.md`). El `aplicando` huérfano (C-115) lo cierra U07-E por plazo: la bitácora de U07-D dice «lo cierra la vigilancia por plazo de U07-E» y la de U07-E (CA-2) muestra el cierre `Venció el plazo de vigilancia sin que el despliegue quedara listo`; la bitácora de U07-E no nombra C-115.
9. **U07-D, `X-Synkra-Usuario` sin autenticar.** R-D8 guarda quién crea y quién confirma con la cabecera `X-Synkra-Usuario`, que es un marcador de identidad y no un control de acceso mientras U10 esté cortada (C-114). Anotó: `bitacoras/U07-D.md`, desviación 5. El informe no la comenta.
10. **U07-D, `SimuladorKubernetes.aceptaAplicar(pais)`.** El modo CRUD de Fabric8 7.9.0 no entiende server-side apply (C-121). Anotó: `bitacoras/U07-D.md`, desviación 1. Revisor: «`aceptaAplicar`: legítima» (`revisiones/U07-D/ronda-1.md`).
11. **U07-D, los `grep` de `REQUIRES_NEW` y `reg_`.** Casaban con comentarios (C-122). Anotó: `bitacoras/U07-D.md`, desviación 3 y ronda 2 (F-02 y F-03). Revisor: «`REQUIRES_NEW`: es un defecto del criterio, no del diff».
12. **U07-D, el diagnóstico del `grep -cx` de CA-0 era falso.** El comando literal da 1; el 0 salió de un arnés que expandió `${SYNKRA_PAISES:co,pe}` (desviación 2 de la bitácora, corregida en la ronda 2). Revisor: F-02 AMARILLO, «El diagnóstico del `grep -cx` es falso».
13. **U07-D, un comentario cambiado para contentar un `grep`.** `Plan.java` cambió un comentario por un `grep`; se revirtió en la ronda 2 (`bitacoras/U07-D.md`, «Cambio»). Revisor: F-03 AMARILLO, «Se cambió un comentario para contentar un `grep`».
14. **U07-D, C-35 y C-87.** C-35 no muerde en el despliegue (los objetos de Kubernetes tienen nombres fijos) y C-87 queda cerrada por construcción en este camino; no se añadió código. Anotó: `bitacoras/U07-D.md`, desviación 6. El informe no la comenta.
15. **U07-E, cambios en `kubernetes` y `registro` pedidos por la tarea.** C-76 (`.withRequestRetryBackoffLimit(0)`), C-82 (`estado` juzga la versión nueva), C-83 (eventos del Deployment y sus ReplicaSet) y `RegistroApi.aplicando()` (C-116): la tarea los asignó a U07-E, no son un desvío del plan. Anotó: `bitacoras/U07-E.md`, «Cambio (commit 4869522)». Con la programación apagada en las pruebas (`synkra.programacion.activa`, C-117) y el log de un solo pod, que sigue así (R-R4, `bitacoras/U07-E.md`; C-119).
16. **U07-E, `fallaLecturas` y el simulador.** El simulado atiende una expectativa por ruta y se queda con la primera; se añadió una línea que quita la previa. Anotó: `bitacoras/U07-E.md`, «Desviaciones», 1. Revisor: «aceptable» (`revisiones/U07-E/ronda-1.md`, F-03).
17. **U07-E, logs que el simulado no sirve.** `marcaListo` y `marcaFallo` sirven 400 a los logs inexistentes. Anotó: `bitacoras/U07-E.md`, desviación 2. Revisor: «aceptable, sigue el patrón de `logAunNoExiste`».
18. **U07-E, `metadata.generation`.** El modo CRUD fija `generation` en 1, así que la prueba siembra `observedGeneration` 0. Anotó: `bitacoras/U07-E.md`, desviación 3. Revisor: «misma regla (`observada < generation`)».
19. **U07-E, `marcaListo` y el estado del Deployment.** Anotó: `bitacoras/U07-E.md`, desviación 4. El informe no la comenta.
20. **U07-E, un `FALLIDO` con contexto ilegible.** La decisión no lo decía; la vigilancia lo trata como «sin lectura» y lo cierra el plazo. Anotó: `bitacoras/U07-E.md`, desviación 5, sin prueba propia en la ronda 1. Revisor: F-01 NARANJA en la ronda 1, «F-01 (resuelto)» en la ronda 2 con `VigilanciaDobleTest`. Queda C-126.
21. **U07-E, el glob de CA-1.** `TEST-*VigilanciaTest.xml` también casa `RetomarVigilanciaTest`. Anotó: `bitacoras/U07-E.md`, desviación 6. Revisor: «es un defecto de la plantilla, no del codificador».
22. **U07-E, la javadoc de `kubernetes.api`.** Sigue diciendo «eventos de los pods»: C-124. Anotó: `bitacoras/U07-E.md`, desviación 7.
23. **U07-F, `ConfirmacionServicio` sin cambios.** La tarea decía que pasaba el tipo a `NuevoDespliegue`; lo hace `ReservaConfirmacion` con `valueOf`. Anotó: `bitacoras/U07-F.md`, «Hallazgos para el orquestador». Revisor: «ConfirmacionServicio sin cambios: lo acepto» (`revisiones/U07-F/ronda-1.md`).
24. **U07-F, el dato de `mx` (974).** La decisión pedía S2; la prueba usa `kyc-pe` 2.0.0 para que quitar R-D1 muerda. Anotó: `bitacoras/U07-F.md`, «Ajuste de dato en la prueba». Revisor: F-03 AMARILLO, ratificado por el orquestador (`bitacoras/U07-F.md`, ronda 2).
25. **U07-F, `origenId` frente al `.original` de H-20.** El campo del registro se llama `origenId`; `.original` de la historia es ese campo (`bitacoras/U07-F.md`, «Cambio»). El informe no la comenta.
26. **U07-F, R-D1 dos veces.** Nota de diseño del revisor, ver «Fronteras» (`revisiones/U07-F/ronda-3.md`).
27. **U07-G, `plataforma` y `verificador`.** U07-T12 del plan dice que `despliegue` solo usa `catalogo.api`, `kubernetes.api` y `registro.api`; también usa `plataforma` y `verificador` (decisión 1 de `tareas/U07-G-fronteras-resumen.md`; `bitacoras/U07-G.md`). Revisión: por revisar.

## Trazabilidad

| Historia | Tareas que la cubren | Entregada por |
|---|---|---|
| H-09 | U07-T03 | U07-B |
| H-11 | U07-T01, U07-T04, U07-T05 | U07-A, U07-B |
| H-12 | U07-T07, U07-T12, U07-T13 | U07-D, U07-G |
| H-13 | U07-T02 | U07-A |
| H-14 | U07-T08, U07-T09 | U07-E |
| H-16 | U07-T08 | U07-E |
| H-17 | U07-T06 | U07-C |
| H-20 | U07-T10 | U07-F |
| H-06 (contribuye) | U07-T11 | U07-D |
| H-08 (contribuye) | U07-T03 | U07-B |

El plan nombra H-24 como contribución, pero ninguna tarea de U07 la ata por sí sola. Además de U07-T06 (U07-C), contribuyen a H-17 U07-D, U07-E (un plan que nadie toca vence solo, C-109) y U07-F, según la cabecera de cada tarea.

## Tareas

| Tarea | Qué construyó | Tareas del plan | PR | Rondas y veredictos |
|---|---|---|---|---|
| U07-A | `V4__despliegue.sql` (`dep_plan`), `Plan`, `PlanRepositorio`, `PaisPermitido` y `synkra.paises` | U07-T01 y U07-T02 | #96 | 1: ronda 1 VERDE |
| U07-B | `POST /api/verificaciones` y `POST /api/despliegues`, resumen y cambios, `SimuladorKubernetes` | U07-T03 a U07-T05 | #98 | 2: ronda 1 NO-VERDE, ronda 2 VERDE |
| U07-C | bean `Clock`, plan vencido, guarda de confirmación (501 temporal) | U07-T06 | #100 | 2: ronda 1 NO-VERDE, ronda 2 VERDE |
| U07-D | confirmar de verdad (R-D5 a R-D9), `ConfirmacionServicio`, `ReservaConfirmacion` | U07-T07 y U07-T11 | #104 | 2: ronda 1 NO-VERDE, ronda 2 VERDE |
| U07-E | vigilancia, tareas programadas, retomar tras un reinicio, C-76, C-82, C-83, C-116 | U07-T08 y U07-T09 | #105 | 2: ronda 1 NO-VERDE, ronda 2 VERDE |
| U07-F | reversión como plan nuevo | U07-T10 | #106 | 3: ronda 1 NO-VERDE, ronda 2 NO-VERDE, ronda 3 VERDE |
| U07-G | fronteras de `despliegue` y este resumen | U07-T12 y U07-T13 | sin PR todavía | por revisar |

Los PR salen de `git log --merges` y de `gh pr list --state merged`. U07-F arrancó en paralelo con U07-E por pedido del responsable del proyecto e incorporó U07-E por fusión (`bitacoras/U07-F.md`, ronda 2: fusión `--no-ff` de `origin/m6r/U07-E`), y por eso su orden de PR es #105 y luego #106. U07-F necesitó tres rondas: la ronda 1 dio ROJO por dos rechazos sin `mensaje` ni `detalles` exactos (F-01) y NARANJA por el lugar de `ORIGEN_NO_LISTO` (F-02), y la ronda 2 dio ROJO por el `cl` que drenaba las peticiones justo antes de mirarlas (F-04). El número de rondas es el de `ls revisiones/U07-*/ronda-?.md`; el veredicto de cada una, el de su línea `VEREDICTO:`.

## Candidatas abiertas

Fuente: `CANDIDATAS.md` de esta rama (hasta C-127). Ninguna se cierra ni se reabre aquí.

**Decididas en U07 según su bitácora o revisión** (cada una con su columna «Decisión» de `CANDIDATAS.md` tal como está, y la frase de la fuente que dice que se resolvió o se decidió; ninguna se cierra en `CANDIDATAS.md`):

- **C-30** (el plan de U07 nombraba a una persona). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: se corrige al escribir las tareas de U07». Fuente: `bitacoras/U07-A.md`: «aviso previo al responsable del proyecto».
- **C-35** (`Descriptor.NOMBRE` sin límite de longitud). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: se decide con U07 (despliegue), donde el nombre llega a Kubernetes». Fuente: `bitacoras/U07-D.md`: «no muerde en el despliegue».
- **C-87** (colisión de nombres de JAR en `construir`). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: alinear `PlanPais` con `Descriptor.NOMBRE` y semver, o rechazar la colisión en `construir`. Va con U07 (quien arma el `PlanPais`) o con el pulido de U05». Fuente: `bitacoras/U07-D.md`: «queda cerrada por construcción en este camino».
- **C-76** (reintentos implícitos del cliente de Fabric8). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: se decide en U07 (desactivar o acotar los reintentos del cliente). Ampliada en la revisión de U05-T05, ronda 2 (F-10): el cliente de producción usa los valores por defecto (hasta 11 `GET` y unos 19 s ante un 5xx o 429 por lectura), mientras que las pruebas de U05-T05 corren con `requestRetryBackoffLimit(0)`; U07 debe fijar la configuración real con una prueba, porque choca con el sondeo de 5 s de S5». Fuente: `bitacoras/U07-E.md`: «`.withRequestRetryBackoffLimit(0)` (C-76)».
- **C-82** (`estado` da `LISTO` antes de mirar los pods). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: se decide en U07 (vigilancia S5)». Fuente: `bitacoras/U07-E.md`: «`LectorPais.estado` (C-82)».
- **C-83** (`contextoFallo` sin los eventos del Deployment y el ReplicaSet). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: se decide en U07 (S5) junto con C-82 y con H-16». Fuente: `bitacoras/U07-E.md`: «`LectorPais.contexto` (C-83)».
- **C-108** (el `grep` de escrituras casa con `plan.estado()`). Decisión en `CANDIDATAS.md`: «Aceptada por el orquestador: no bloquea. Se aplica al escribir U07-C y U07-D». Fuente: `bitacoras/U07-D.md`: «`PlanDto.de` usa `plan.estado()` (C-108)».
- **C-109** (barrido programado de planes vencidos). Decisión en `CANDIDATAS.md`: «Aceptada: va en U07-E, que trae la primera tarea programada (vigilancia)». Fuente: `bitacoras/U07-E.md`: «C-109 | `s/vencimiento\.vencerPendientes();/;/` (TareasProgramadas)».
- **C-115** (un `aplicando` huérfano si el proceso muere antes de aplicar). Decisión en `CANDIDATAS.md`: «Aceptada: va en U07-E». Fuente: `bitacoras/U07-D.md`: «lo cierra la vigilancia por plazo de U07-E».
- **C-116** (`RegistroApi.aplicando()`). Decisión en `CANDIDATAS.md`: «Aceptada: va en U07-E». Fuente: `bitacoras/U07-E.md`: «`RegistroApi.aplicando()` (C-116)».
- **C-107** (no usar `REQUIRES_NEW` con el pool de una conexión). Decisión en `CANDIDATAS.md`: «Aceptada como candidata por el orquestador: no bloquea. Se avisa en las tareas de U07-D y U07-E». Fuente: `revisiones/U07-D/ronda-1.md`: «C-107 se cumple».
- **C-114** (`X-Synkra-Usuario` sin autenticar). Decisión en `CANDIDATAS.md`: «Aceptada como candidata: se documenta en el resumen de U07; se resuelve solo si vuelve U10». Fuente: `bitacoras/U07-D.md`: «es un marcador de identidad, no un control de acceso».

**Abiertas** (con su «Decisión» tal como está):

| # | Qué es | Decisión en `CANDIDATAS.md` |
|---|---|---|
| C-90 | `leerPais` valida con `PlanPais.SEGURO`, más laxo que el core | «para U06/U07, alinear `PlanPais` con el verificador o usar el lector compartido de C-88» |
| C-106 | pulido de U07-A: CHECK `json_valid` sin prueba y otros dos | «no bloquea. Va al pulido de U07» |
| C-110 | `synkra.paises` validado dos veces (`despliegue` y `registro`) | «pulido después de U07-D» |
| C-112 | índice único parcial de un solo `aplicando` por país | «pulido después de U07» |
| C-113 | `RegistroServicio` con su `Clock.systemUTC()` privado | «pulido después de U07» |
| C-117 | validar que `synkra.programacion.activa` no quede apagada en producción | «pulido de U07 o U08» |
| C-118 | tiempos de espera explícitos del cliente de Fabric8 | «pulido de U07 o U08» |
| C-119 | el log sigue siendo de un solo pod | «pulido después de U07» |
| C-122 | los `grep` de las plantillas de tareas | «Aceptada: se aplica al despachar U07-E y U07-F (aclaración en el despacho)» |
| C-121 | un simulador que entienda server-side apply | «pulido de pruebas» |
| C-123 | `ConfirmacionServicio`: se pierde la excepción original si `actualizarResultado` lanza | «revisar con U07-E» |
| C-124 | javadoc de `estado` y `contextoFallo` | «pulido de U05/U07» |
| C-125 | el literal `"synkra-" + pais` en tres sitios | «pulido después de U07» |
| C-126 | una fila `aplicando` cuyo `leerPais` siempre lanza nunca vence | «pulido de U07» |
| C-127 | pulido de pruebas de U07-E (`finally` y mutación casi equivalente) | «no bloquea; pulido de U07» |

**Relacionadas** (nombran a U07 en su «Decisión»; sin decir que U07 las resolvió):

- C-107 y C-110, arriba.
- C-40 (coerción de `Integer`: «se decide con el primero (U05 o U07)»), C-46, C-49, C-51 y C-52 (extracción del init container: «se decide con U07»), C-62 (N+1 de `Catalogo`: «se evalúa en U07») y C-73 (`ObjetosPais`: «se decide en U05-T09»; nombra a U07 en su texto): abiertas.

## Cifras y su fuente

| cifra | ruta | cadena |
|---|---|---|
| A: 35 XML y 457 pruebas, ronda 1 | `revisiones/U07-A/ronda-1.md` | `35 XML con 457 pruebas` |
| A: doce mutaciones | `revisiones/U07-A/ronda-1.md` | `una por mutación (12 en total)` |
| A: PlanRepositorioTest, 12 pruebas | `revisiones/U07-A/ronda-1.md` | `tests="12" skipped="0" failures="0" errors="0"` |
| A: rojo, 13 pruebas y 11 fallos | `bitacoras/U07-A.md` | `13 tests completed, 11 failed` |
| B: 38 XML y 485 pruebas, ronda 2 | `revisiones/U07-B/ronda-2.md` | `38 XML con 485 pruebas` |
| B: VerificacionVivoTest, 15 pruebas | `revisiones/U07-B/ronda-1.md` | `tests="15" skipped="0" failures="0" errors="0"` |
| B: rojo de VerificacionVivoTest, 14 de 15 | `revisiones/U07-B/ronda-1.md` | `da 14 de 15 fallos` |
| B: seis mutaciones del criterio | `revisiones/U07-B/ronda-1.md` | `Las seis mutaciones del criterio` |
| C: 40 XML, ronda 1 | `revisiones/U07-C/ronda-1.md` | `los 40 XML en` |
| C: 41 XML, ronda 2 | `revisiones/U07-C/ronda-2.md` | `los 41 XML en` |
| C: ocho mutaciones, ronda 2 | `revisiones/U07-C/ronda-2.md` | `las 8 dan BUILD FAILED` |
| D: 51 XML, ronda 2 | `revisiones/U07-D/ronda-2.md` | `51 XML con 0/0/0` |
| D: catorce mutaciones | `bitacoras/U07-D.md` | `Las catorce dan` |
| D: rojo, 23 pruebas y 11 fallos | `bitacoras/U07-D.md` | `23 tests completed, 11 failed` |
| D: ConfirmarSinClusterTest en el rojo, una prueba | `bitacoras/U07-D.md` | `ConfirmarSinClusterTest tests="1" failures="1"` |
| E: 57 XML, ronda 1 | `revisiones/U07-E/ronda-1.md` | `57 XML todos 0/0/0` |
| E: 59 XML, ronda 2 | `revisiones/U07-E/ronda-2.md` | `los 59 XML con 0/0/0` |
| E: doce mutaciones del revisor, ronda 2 | `revisiones/U07-E/ronda-2.md` | `12 mutaciones mías` |
| E: veinte mutaciones del codificador | `bitacoras/U07-E.md` | `veinte mutaciones` |
| E: rojo, 23 pruebas y 22 fallos | `bitacoras/U07-E.md` | `23 tests completed, 22 failed` |
| F: 60 XML, ronda 3 | `revisiones/U07-F/ronda-3.md` | `Hay 60 XML` |
| F: diez mutaciones de la tarea, ronda 3 | `revisiones/U07-F/ronda-3.md` | `las 10 de la tarea` |
| F: quince entre mutaciones y sondas, ronda 3 | `revisiones/U07-F/ronda-3.md` | `las 15 mutaciones y sondas` |
| F: rojo de ReversionTest, nueve de nueve | `bitacoras/U07-F.md` | `tests="9" failures="9"` |
