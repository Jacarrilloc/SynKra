# U01 Verificador y suite: resumen de la unidad

Unidad: U01 (`verificador/`, Java 21, sin Spring). Historia: H-10 (la suite S1-S12 bloquea el build); la unidad también implementa la regla de compatibilidad del PRD.
Este resumen lo escribe U01-T14 sin cambiar código, pruebas, build ni scripts. La unidad queda lista para que el orquestador la cierre (las casillas del plan y `aidlc-state.md` los toca el orquestador).

## Qué expone (API pública)

Paquete `io.synkra.verificador`. Todo es inmutable. Los fallos de datos (un descriptor, una versión o un rango mal escritos) vuelven como `Resultado.Fallo` con una lista de `Motivo`, sin excepción. Las precondiciones de programación sí lanzan `IllegalArgumentException`: los constructores de `Descriptor`, `Seleccion`, `Alternativa`, `Motivo`, `Version`, `RangoVersion`, `Veredicto`, `VeredictoPlugin` y `Resultado.Fallo` rechazan valores que rompen sus invariantes, y `Verificador.alternativas` lanza si el veredicto recibido no es el de `verificar(seleccion, catalogo)`. Las firmas de abajo se comprobaron con `grep` en el código.

| Tipo | Firma pública | Qué hace |
|---|---|---|
| `Version` | `record Version(int mayor, int menor, int parche) implements Comparable<Version>`; `static Resultado<Version> parsear(String texto)`; `compareTo`; `toString` | Versión `X.Y.Z`; el orden natural es el de semver |
| `RangoVersion` | `record RangoVersion(Version minimo, Optional<Version> maximo, String texto)`; `static Resultado<RangoVersion> parsear(String texto)`; `boolean incluye(Version v)` | Las cuatro formas de R-V2 como intervalo `[minimo, maximo)` |
| `Descriptor` | `record Descriptor(String nombre, Version version, String core, RangoVersion coreCompatible, Map<String, String> etiquetas, Optional<String> descripcion)`; `static final Pattern NOMBRE` | El contenido validado de `synkra-plugin.yaml` |
| `LectorDescriptor` | `static Resultado<Descriptor> leer(Path jar)`; `static Resultado<Descriptor> desdeYaml(String yaml)`; `static String aYaml(Descriptor d)`; `static final String ARCHIVO = "synkra-plugin.yaml"` | Lee y valida R-V3 con snakeyaml-engine en modo seguro; devuelve todos los motivos de una vez |
| `Seleccion` | `record Seleccion(String core, Version versionCore, List<Descriptor> plugins)` | Lo que se quiere desplegar |
| `Veredicto` | `record Veredicto(Global global, List<VeredictoPlugin> porPlugin, List<Motivo> motivosGlobales)`; `enum Global { APROBADA, RECHAZADA }`; `static Veredicto de(List<VeredictoPlugin>, List<Motivo>)` | Resultado por plugin y global |
| `VeredictoPlugin` | `record VeredictoPlugin(Descriptor descriptor, boolean compatible, List<Motivo> motivos, List<Alternativa> alternativas)` | Una fila del veredicto; trae alternativas solo con `VERSION_INCOMPATIBLE` (C-22) |
| `Alternativa` | `record Alternativa(TipoAlternativa tipo, String nombre, Version version)`; `enum TipoAlternativa { PLUGIN, CORE }` | Versión del plugin o del core que sí sirve |
| `Catalogo` | `interface Catalogo { List<Version> versionesCore(String nombre); List<Version> versionesPlugin(String core, String plugin); Optional<Descriptor> descriptor(String core, String plugin, Version version); }` | Vista de solo lectura de lo subido; la implementa quien tenga los datos |
| `Verificador` | `static Veredicto verificar(Seleccion seleccion, Catalogo catalogo)`; `static Veredicto alternativas(Seleccion seleccion, Veredicto veredicto, Catalogo catalogo)` | `verificar` aplica R-V4 en orden; `alternativas` aplica R-V5 y devuelve un veredicto nuevo |
| `Resultado<T>`, `Motivo`, `CodigoMotivo` | `sealed interface Resultado<T>` con `Exito<T>(T valor)` y `Fallo<T>(List<Motivo> motivos)`; `record Motivo(CodigoMotivo codigo, Optional<String> plugin, String mensaje, Optional<String> campo)`; `enum CodigoMotivo` (10 códigos; el último, `DESCRIPTOR_INVALIDO`) | Errores como valores |

En `testFixtures` (se consume con `testFixtures(project(":verificador"))`):

| Tipo | Firma pública | Qué hace |
|---|---|---|
| `CatalogoEnMemoria` | `final class CatalogoEnMemoria implements Catalogo`; `CatalogoEnMemoria(Map<String, List<Version>> versionesCore, List<Descriptor> descriptores)` | `Catalogo` en memoria para pruebas de U02 y U07 (fuente: `unidades-y-tareas.md`, responsabilidad de U01, y `tareas/U01-T09-verificador.md`) |
| `SuiteCombinaciones` | `static CatalogoEnMemoria catalogo()`; `static List<Caso> casos()`; `static List<CoreDelCatalogo> contenido()`; `record Caso(String id, Grupo grupo, String descripcion, Entrada entrada, Esperado esperado)` | La suite S1-S12 del PRD: el catálogo fijo y cada caso con su resultado esperado exacto |

`Entrada` es un `sealed interface` con tres formas: `PorSeleccion` (se corre `Verificador`), `PorDescriptor` (se corre `LectorDescriptor.desdeYaml`) y `FueraDeU01` (S11 y S12: no se ejecutan en U01, remiten a otra unidad).

## Quién lo usa y cómo

Cada afirmación lleva su fuente entre paréntesis. Lo que ningún documento dice no está aquí.

- **U02 (core de ejemplo):** `ArranquePlugins` lee el descriptor, verifica con U01 y activa; `ConcordanciaSuiteTest` aplica los casos ejecutables de la suite de U01 al arranque (`unidades-y-tareas.md`, U02-T04 y U02-T05). S12 (plugin en el catálogo pero no elegido) es `FueraDeU01` y queda pendiente en U02 (`SuiteCombinaciones.java`, caso S12).
- **U04 (catálogo):** implementa su propio `Catalogo` sobre SQLite y lo prueba con los casos S1 a S7 de `SuiteCombinaciones` (`unidades-y-tareas.md`, U04-T09; `U04-catalogo-code-generation-plan.md`, línea 48). S11 (el hash del artefacto no coincide) es `FueraDeU01` y queda pendiente en U04 y U05 (`SuiteCombinaciones.java`, caso S11). La recepción del JAR usará `LectorDescriptor`: de ahí salen C-13, C-19 y C-20 (`CANDIDATAS.md`).
- **U07 (despliegue):** U07 depende de U01 y reutiliza la suite (`unidades-y-tareas.md`, responsabilidad de U01 y dependencias de U07). La verificación en vivo (U07-T03) prueba S4 con alternativas presentes, y la creación del plan (U07-T04) rechaza el plan inválido sin crear fila.
- **Límite de U01:** no lee el catálogo, no conoce Spring, Kubernetes ni la red; recibe un `Catalogo` de solo lectura (`unidades-y-tareas.md`, «Qué no hace» de U01).

## Tareas

| Tarea | Qué construyó | PR | Rondas de revisión |
|---|---|---|---|
| U01-T01 | Raíz Gradle multi-proyecto: toolchain Java 21, wrapper 9.9.2 con checksum, JVM del daemon fijada, `.gitignore` | #8 | 1 |
| U01-T02 | `verificador/build.gradle.kts`: `java-library`, `java-test-fixtures`, snakeyaml-engine, JUnit y jqwik | #9 | 1 |
| U01-T03 | `Version`, `Resultado`, `Motivo`, `CodigoMotivo` (9 códigos en esta tarea; U01-T06 añadió `DESCRIPTOR_INVALIDO`, hoy son 10) | #11 | 2 |
| U01-T04 | `RangoVersion` con las cuatro formas de R-V2 | #13 | 2 |
| U01-T05 | `RangoVersionPropiedades` (jqwik, PBT-02 y PBT-03) | #18 | 1 |
| U01-T06 | `Descriptor` y `LectorDescriptor` (`leer`, `desdeYaml`, `aYaml`) | #20 | 3 |
| U01-T07 | `DescriptorPropiedades` y `Generadores` compartidos (ida y vuelta; `desdeYaml` nunca lanza) | #24 | 2 |
| U01-T08 | `Seleccion`, `Alternativa`, `VeredictoPlugin`, `Veredicto` y `CodigoMotivoTest` contra `domain-entities.md` | #27 | 4 |
| U01-T09 | `Catalogo`, `Verificador.verificar` (R-V4) y `CatalogoEnMemoria` | #30 | 3 |
| U01-T10 | `Verificador.alternativas` (R-V5) y la invariante de C-22 | #31 | 2 |
| U01-T11 | `VerificadorPropiedades` (jqwik) | #32 | 3 |
| U01-T12 | `SuiteCombinaciones` y `SuiteCombinacionesTest` (S1 a S12; 10 casos ejecutables); cierra C-26 | #35 | 6 |
| U01-T13 | `verificarSuite` en `verificador/build.gradle.kts`, del que depende `check`, y `scripts/control-suite.sh` | #36 | 2 |
| U01-T14 | Este resumen | este PR (U01-T14) | (pendiente) |

Las rondas son los informes `revisiones/U01-T<NN>/ronda-N.md` del revisor (sin contar los `ronda-N-respuesta.md`). C-02 (ignorar `.jqwik-database`) se hizo como tarea propia en `m6r/C-02` y se fusionó con el PR #15.

### `verificarSuite` (U01-T13), como está en el código

`check` depende de `verificarSuite`. La tarea lee el XML de `SuiteCombinacionesTest` y falla, con un mensaje que nombra la causa, si: (1) el XML no existe; (2) `tests` es menor que 17 (los 10 casos S1-S10 más las 7 pruebas de forma); (3) `skipped` no es 0; (4) `failures` o `errors` no son 0; (5) la huella no existe o no coincide. La frescura no usa la hora del archivo: al terminar, `test` escribe una huella SHA-256 de contenido (rutas relativas ordenadas y bytes de las clases compiladas de main, testFixtures y test) en `test-results/test/huella-suite.txt`, y `verificarSuite` la recalcula. Un `test` UP-TO-DATE conserva XML y huella; recompilar con el mismo bytecode no los invalida; cambiar una clase sin volver a correr `test` sí.

### Suite S1-S12 (U01-T12): control de mutaciones

El criterio CA-3 de U01-T12 aplica mutaciones por separado sobre una copia y exige que la suite falle. Su punto (d), «`alternativas` toma la versión de core más alta», sobrevivía: con el catálogo fijo del diseño el mínimo y el máximo coinciden. El orquestador lo reemplazó por «`alternativas` no añade la alternativa de core», que sí hace fallar S4 y S5. La diferencia entre la versión más baja y la más alta la cubren `AlternativasTest.masAltaYMasBajaDeVerdadSinImportarElOrden` y `VerificadorPropiedades.minimoYMaximo`.

## Archivos

Raíz (U01-T01):

- `settings.gradle.kts`, `build.gradle.kts`, `.gitignore`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/gradle-daemon-jvm.properties`

Control (U01-T13): `scripts/control-suite.sh`.

Código (`verificador/`):

- `verificador/build.gradle.kts`
- `verificador/src/main/java/io/synkra/verificador/Alternativa.java`
- `verificador/src/main/java/io/synkra/verificador/Catalogo.java`
- `verificador/src/main/java/io/synkra/verificador/CodigoMotivo.java`
- `verificador/src/main/java/io/synkra/verificador/Descriptor.java`
- `verificador/src/main/java/io/synkra/verificador/LectorDescriptor.java`
- `verificador/src/main/java/io/synkra/verificador/Motivo.java`
- `verificador/src/main/java/io/synkra/verificador/RangoVersion.java`
- `verificador/src/main/java/io/synkra/verificador/Resultado.java`
- `verificador/src/main/java/io/synkra/verificador/Seleccion.java`
- `verificador/src/main/java/io/synkra/verificador/Veredicto.java`
- `verificador/src/main/java/io/synkra/verificador/VeredictoPlugin.java`
- `verificador/src/main/java/io/synkra/verificador/Verificador.java`
- `verificador/src/main/java/io/synkra/verificador/Version.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/CatalogoEnMemoria.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java`

Pruebas (`verificador/src/test/java/io/synkra/verificador/`):

- `AlternativasTest.java`, `CatalogoEnMemoriaTest.java`, `CodigoMotivoTest.java`, `LectorDescriptorTest.java`
- `RangoVersionTest.java`, `SuiteCombinacionesTest.java`, `VeredictoTest.java`, `VerificadorTest.java`, `VersionTest.java`
- jqwik: `DescriptorPropiedades.java`, `RangoVersionPropiedades.java`, `VerificadorPropiedades.java`
- apoyo: `Generadores.java` (generadores compartidos de `Version` y `RangoVersion`)

Diseño que la unidad implementa: `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md`, `business-rules.md` y `domain-entities.md`.

Artefactos de build (no se versionan; `.gitignore` ignora `build/`): `verificador/build/test-results/test/TEST-*.xml` y `verificador/build/test-results/test/huella-suite.txt`. Los conteos de abajo salen de esos XML.

## Cómo construir y probar

Desde la raíz del repositorio (JDK 25 en la máquina; el proyecto fija Java 21 con un toolchain de Gradle):

```bash
./gradlew :verificador:test --rerun :verificador:check --console=plain   # pruebas, verificarSuite y verificaciones
bash scripts/control-suite.sh                                            # control: alterar S4 o desactivar la suite hace fallar check
```

## Evidencia

### `:verificador:check` (corrida del 2026-11-27, 06:28 UTC)

Cola (`tail -6`) de `./gradlew :verificador:test --rerun :verificador:check --console=plain`, de `/tmp/u01-check.txt`, sin editar. Fecha y hora al terminar: 2026-11-27 06:28:38 UTC (rc=0):

```

For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 31s
7 actionable tasks: 7 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
```

La línea de `verificarSuite` de la misma corrida (`grep -n verificarSuite /tmp/u01-check.txt`; `tail -6` no la alcanza), sin editar:

```
19:> Task :verificador:verificarSuite
```

### Control de la suite (`bash scripts/control-suite.sh`, 2026-11-27, 06:28:41 a 06:30:44 UTC)

Salida completa, sin editar (el directorio temporal cambia en cada corrida):

```
temporal: /tmp/tmp.kf60u6AOts
escenario 1 (sin cambios): check rc=0 (esperado 0)
escenario 2 (S4 alterado): check rc=1 (esperado != 0)
escenario 3 (suite @Disabled): check rc=1 (esperado != 0)
causa 2: SuiteCombinacionesTest > casoEjecutable(String, Caso) > "S4" FAILED
causa 3: > verificarSuite: corrieron 8 pruebas de SuiteCombinacionesTest y se exigen al menos 17 (los 10 casos S1-S10 más las pruebas de forma)
rc=0
```

### Conteos de pruebas (XML de `verificador/build/test-results/test/`, de la corrida de arriba)

| Clase | tests | skipped | failures | errors | tries por propiedad |
|---|---|---|---|---|---|
| io.synkra.verificador.AlternativasTest | 15 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.CatalogoEnMemoriaTest | 5 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.CodigoMotivoTest | 14 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.DescriptorPropiedades | 3 | 0 | 0 | 0 | `idaYVuelta` 1000; `desdeYamlNuncaLanza` 1000 (`generadorCubreTextosAmbiguos` es una prueba normal) |
| io.synkra.verificador.LectorDescriptorTest | 29 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.RangoVersionPropiedades | 2 | 0 | 0 | 0 | `nXIncluyeSoloSuMayor` 1000; `parsearElTextoDaElMismoIntervalo` 1000 |
| io.synkra.verificador.RangoVersionTest | 36 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.SuiteCombinacionesTest | 17 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.VeredictoTest | 17 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.VerificadorPropiedades | 9 | 0 | 0 | 0 | `determinista`, `ordenYRepetidos`, `unMotivoPorPluginYGlobalEquivaleASinMotivos`, `alternativaAplicadaAprueba`, `completitud` y `minimoYMaximo`: 1000 cada una (las otras 3 son pruebas normales) |
| io.synkra.verificador.VerificadorTest | 15 | 0 | 0 | 0 | n/a |
| io.synkra.verificador.VersionTest | 16 | 0 | 0 | 0 | n/a |
| **Total (12 clases)** | **178** | **0** | **0** | **0** | 10 propiedades, 1000 intentos cada una |

Las 10 propiedades de jqwik muestran `tries = 1000` y `checks = 1000` en el XML (`system-out`). Ninguna clase tiene `skipped` distinto de 0.

## Candidatas

Salieron de una tarea de U01 (columna de origen con «U01»), tal como las lista `CANDIDATAS.md`. La lista sale de `awk -F'|' '$4 ~ /U01/ {print $2}' CANDIDATAS.md`: C-01, C-02, C-06, C-09, C-10, C-13, C-15, C-16, C-19, C-20, C-22, C-23, C-24, C-25, C-26, C-27 y C-28 (17). Este resumen solo las lista: no resuelve ninguna. El estado y el destino son el texto de `CANDIDATAS.md`; si ahí una candidata dice «se corrige con» una tarea que ya se fusionó, ahí sigue el texto original.

| Id | Qué | Origen | Estado y destino (según `CANDIDATAS.md`) |
|---|---|---|---|
| C-01 | Hacer más estricta la lectura de vuelta de la JVM del daemon: filtrar por el pid que reporta `./gradlew --info` en vez de leer todos los procesos `GradleDaemon` | Revisión de U01-T01, ronda 1 (F-01, AMARILLO) | Aceptada como candidata por el orquestador: el criterio se cumple y el revisor confirmó el daemon correcto con `--info`. Se aplica al escribir tareas nuevas que lean la JVM |
| C-02 | Ignorar `.jqwik-database` (o moverla a `build/` con `jqwik.database` en `junit-platform.properties`) | Codificador y revisión de U01-T02, ronda 1 (F-01, AMARILLO) | Aceptada como candidata por el orquestador: el `.gitignore` es de U01-T01 y quedó fuera del alcance de U01-T02. Aprobada por el responsable el 2026-11-25: se hace como tarea propia, `tareas/C-02-ignorar-jqwik-database.md` (rama `m6r/C-02`) |
| C-06 | Que `LectorDescriptor` asigne el campo (`version`, `coreCompatible`) del `Motivo` en vez de fijarlo `Version` y `RangoVersion` | Revisión de U01-T04, ronda 1 (F-03, AMARILLO) | Empujón aceptado por el orquestador: sigue un precedente ya aprobado. Cerrada en U01-T09 sin cambiar `LectorDescriptor`: `Version` y `RangoVersion` fijan `"version"` y `"coreCompatible"`, los nombres de los campos del descriptor; `LectorDescriptorTest.versionYCoreCompatibleInvalidosLlevanSuCampo` lo ata |
| C-09 | Alinear la fila PBT-08 de `business-logic-model.md` («Semilla fija registrada») con lo aplicado: la semilla cambia en cada ejecución y solo se registra en el reporte | Revisión de U01-T05, ronda 1 | Aceptada como candidata por el orquestador: es un artefacto de diseño, fuera del alcance de una tarea de pruebas. Se corrige con la siguiente tarea que toque PBT (U01-T07) |
| C-10 | Afinar los generadores de `RangoVersionPropiedades`: acotar el máximo de `>=A <B` a `0..50` (F-01) y generar el mayor como `n + desplazamiento` para que la reducción llegue a `0, 1` y la detección sea más densa (F-02) | Revisión de U01-T05, ronda 1 (F-01 y F-02, AMARILLO) | Aceptada como candidata por el orquestador: la ronda salió VERDE, CA-3 se cumple y los dos son mejoras de calidad de la prueba. Se retoma con U01-T07, que reutiliza los generadores |
| C-13 | Que los mensajes de error de `LectorDescriptor.leer` no expongan la ruta absoluta del sistema de archivos (`e.getMessage()`) | Revisión de U01-T06, ronda 1 | Aceptada como candidata por el orquestador: el lector todavía no se expone por la API. Se revisa en U04, cuando la recepción del JAR use el lector |
| C-15 | En U01-T07, el generador de descriptores incluye textos que YAML 1.2 Core resuelve como nulo, bool, int o float (`~`, `True`, `007`, `0x1F`, `.NaN`) en `descripcion` y en los valores de etiqueta, y claves de etiqueta que cumplen el patrón pero parecen otro tipo (`007`, `true`, `null`) | Revisión de U01-T06, rondas 2 (F-07) y 3 | Aceptada como candidata por el orquestador: la propiedad general de ida y vuelta es de U01-T07. Se incluye en esa tarea al escribirla |
| C-16 | Escribir en R-V3 que una etiqueta explícita se trata distinto en los campos (como escalar plano: `version: !!int 1` da `VERSION_INVALIDA`) y en las claves y valores de `etiquetas` (exige `str`: da `DESCRIPTOR_INVALIDO`) | Revisión de U01-T06, ronda 3 (F-10, AMARILLO) | Aceptada como candidata por el orquestador: la ronda salió VERDE y los dos casos son `Fallo`. Se documenta con U01-T07, que vuelve a tocar el descriptor |
| C-19 | Acotar o agrupar los motivos de `desdeYaml`: un descriptor de menos de 64 KB con 5000 claves desconocidas devuelve 5000 motivos (unos 360 KB) | Revisión de U01-T07, ronda 1 | Aceptada como candidata por el orquestador: U01-T07 no toca `src/main`. Se decide en U04, cuando la recepción del JAR devuelva esos motivos por la API |
| C-20 | Unificar la unidad de `MAX_BYTES`: `desdeYaml` la mide en caracteres y `leer` en bytes (20000 emojis, unos 80 KB en UTF-8, pasan `desdeYaml`) | Revisión de U01-T07, ronda 1 | Aceptada como candidata por el orquestador: fuera del alcance de una tarea de pruebas. Se corrige con la siguiente tarea que toque `LectorDescriptor` (U04, con C-13) |
| C-22 | Decidir si `VeredictoPlugin` con `compatible = true` puede traer alternativas: según R-V5 solo existen para `VERSION_INCOMPATIBLE` | Revisión de U01-T08, ronda 1 | Aceptada como candidata por el orquestador: calcular alternativas es de U01-T10, que fija la regla y la invariante. **Cerrada en U01-T10:** `VeredictoPlugin` lanza `IllegalArgumentException` si trae alternativas y ninguno de sus motivos es `VERSION_INCOMPATIBLE` (así `compatible = true` no las trae); regla en R-V5 |
| C-23 | `CodigoMotivoTest` no detecta HTML crudo en la sección «Códigos de motivo»: un `<!-- -->` que oculta la tabla, o una `<table>`, muestra 9 u 11 códigos y la prueba pasa. Arreglo barato: hacer fallar toda línea que empiece por `<` dentro de la sección y fuera de la tabla. También quedan los residuales de F-15: separador sin `-` y la sección duplicada | Revisión de U01-T08, ronda 4 (F-13 y F-15) | Por decisión del responsable, el HTML crudo queda fuera de alcance de U01-T08. El documento lo escribe el equipo y cambia por PR. Se retoma si el diseño pasa a generarse o a editarse fuera del equipo |
| C-24 | `AlternativasTest`: la prueba de nulos solo vigila `versionesCore` (sin `requireNonNull` en `versionesPlugin`, o con `sinNulo` que no comprueba, sigue en verde, porque el NPE implícito también dice «Optional»), y todas las versiones de la prueba tienen una sola cifra, así que comparar por texto no se distingue de comparar por versión | Revisión de U01-T10, ronda 2 (F-05 y F-06, AMARILLO) | Aceptada como candidata por el orquestador. La parte de las versiones de dos cifras se pidió a U01-T11, con su mutación. La de nulos queda para cuando se vuelva a tocar `AlternativasTest` |
| C-25 | jqwik con `after-failure = SAMPLE_FIRST` reutiliza la semilla de la corrida fallida en la siguiente, lo que choca con PBT-08 («la semilla cambia en cada ejecución»). Es configuración de todo el proyecto, junto a C-02 | Revisión de U01-T11, rondas 1 a 3 | Aceptada como candidata por el orquestador: se decide al cerrar U01 (U01-T13 o el resumen U01-T14) |
| C-26 | `VerificadorPropiedades` usa el catálogo de `AlternativasTest` como «catálogo de la suite»; cuando exista S1–S12 en `testFixtures` (U01-T12) debe usar ese. Además, «solo en otra carpeta» se cubre casi siempre con una carpeta cuyo core no está registrado (4,8 de 39,6 por cada 1000 con core registrado) | Revisión de U01-T11, rondas 1 y 3 (F-09) | Cerrada en U01-T12 (`SuiteCombinaciones.catalogo()` como catálogo de la suite; `core-b` registrado en la mitad de los catálogos de texto, y cada catálogo declara su core principal, que pesa más al elegir core, para que no se diluya) |
| C-27 | El umbral 6 de «core con texto distinto de número» en `VerificadorPropiedades.generadorCubreLosCodigos` (de U01-T11) queda con P(fallo) cerca de 10⁻⁶ o algo por encima (8,7e-7 a 2,3e-6, según la varianza por lote medida), por debajo del criterio que se aplicó a las puertas nuevas de U01-T12 | Revisión de U01-T12, rondas 3 a 6 | Aceptada como candidata por el orquestador: se decide junto a C-25 (semilla de jqwik), en U11 |
| C-28 | La huella de `verificarSuite` cubre solo las clases de main, testFixtures y test: no amarra el XML (un XML editado a mano pasa con `-x test`) ni los recursos ni el classpath de dependencias. Arreglo barato: incluir el SHA-256 del XML y usar `sourceSets[x].output` completo más el `runtimeClasspath` de test | Revisión de U01-T13, ronda 2 (F-01 y F-02, AMARILLO) | Aceptada como candidata por el orquestador: solo se puede explotar con `-x test`, que queda fuera de alcance. Se retoma con U11 (CI) |

**C-25, pendiente.** Recomendación del orquestador: decidir en U11 (CI) si se fija `jqwik.failures.after.default = RANDOM_SEED` en `junit-platform.properties`, junto con C-02. C-27 y C-28 también se retoman en U11.

## Lo que queda fuera

- Las candidatas de arriba: ninguna se resolvió en este resumen.
- La integridad del artefacto por hash (S11) y la prueba de plugin no activado (S12): son de U04, U05 y U02; en U01 son `FueraDeU01`.
- Todo lo que toca el clúster, la base de datos o la API REST: U01 no los usa.
- Casillas del plan de U01 y `aidlc-state.md`: las marca el orquestador al cerrar la unidad.
