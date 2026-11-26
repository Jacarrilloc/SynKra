# Ronda 1 — U03-T05

VEREDICTO: NO-VERDE

Worktree `/home/user/wt-m6r-U03-T05`, rama `m6r/U03-T05`, HEAD `acfead5`, diff `b18d6e3...HEAD`, con 5 archivos: el test, 3 clases de la aplicación de prueba y la bitácora. El `git hash-object` de la tarea da `ad26b382…` y coincide. Al terminar, `git status --short` sale limpio: solo hay ignorados (`.gradle/`, `build/`). Hice todas las mutaciones en una copia hecha con `git archive` dentro de mi scratchpad, nunca en el worktree.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de fronteras pasa sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'` | pasa, `BUILD SUCCESSFUL`. El XML tiene timestamp 02:15:53Z y lo corrí a las 02:16Z |
| 2 | Lectura de vuelta del reporte | `grep -ho 'tests=…' backend/build/test-results/test/TEST-*FronterasModulosTest.xml` | pasa, `tests="3" skipped="0" failures="0" errors="0"`. Tiempos 1.442 s, 0.04 s y 0.096 s: no es una suite de 0,0 s |
| 3 | La documentación de módulos se genera | antes borré `backend/build/spring-modulith-docs`, luego `ls` y `grep` | pasa. Se regeneran `components.puml` y `module-plataforma.adoc`; el `.adoc` nombra `io.synkra.plataforma` |
| 4 | Modulith no llega a runtime | `./gradlew :backend:dependencies --configuration runtimeClasspath \| grep -ci modulith` | pasa, `0` |
| 5 | Todo el backend pasa sin caché | `./gradlew :backend:check --rerun` | pasa, `BUILD SUCCESSFUL`. Son 7 XML con 31 tests y skipped=0 en todos |

## Comprobaciones que pidió el orquestador

**El caso real de `verify()` sí analiza `src/main`.** En la copia agregué `io.synkra.alfa.interno.Secreto` y `io.synkra.beta.Beta`, donde `Beta` tiene un campo de tipo `Secreto`. Resultado: `elBackendRealRespetaSusFronteras() FAILED` con `Violations: - Module 'beta' depends on non-exposed type io.synkra.alfa.interno.Secreto within module 'alfa'!`. La prueba es sensible a violaciones reales y no pasa en vacío.

**La aplicación de prueba no se cuela en el análisis de Modulith.** Una sonda que recorre `ApplicationModules.of(SynkraApp.class)` lista solo `io.synkra.alfa`, `io.synkra.plataforma` e `io.synkra.beta`. No aparece ningún módulo `fronteras`, ni `sonda` (paquete de test). El `DoNotIncludeTests` por defecto funciona, así que no hacía falta filtrar. Lo que dice la bitácora sobre esto es correcto.

**El caso negativo no se excluye por accidente.** En la copia dejé `Uno` sin la importación ni el campo. Resultado: `verifyFallaCuandoUnModuloImportaUnPaqueteInternoDeOtro() FAILED – AssertionError at FronterasModulosTest.java:40`, con 3 tests y 1 fallo. El caso negativo depende de verdad de la importación prohibida.

**La aplicación de prueba sí se cuela en el contexto de Spring.** Es el hallazgo F-01.

## Hallazgos

### F-01 · NARANJA · backend/src/test/java/io/synkra/fronteras/AppFronteras.java:6 · La aplicación de prueba se registra como bean en todos los `@SpringBootTest` del backend real
`AppFronteras` lleva `@SpringBootApplication`, y eso implica `@Configuration`, `@EnableAutoConfiguration` y `@ComponentScan`. Está en `io.synkra.fronteras`, que cae dentro del escaneo de componentes de `SynkraApp`, que escanea `io.synkra`, y en las pruebas el classpath incluye las clases de test. Ni `TypeExcludeFilter` ni `AutoConfigurationExcludeFilter` la excluyen.

Lo demostré en la copia con una sonda `@SpringBootTest(classes = SynkraApp.class)` que recorre `getBeanDefinitionNames()`:
```
SONDA-BEAN appFronteras -> class io.synkra.fronteras.AppFronteras$$SpringCGLIB$$0
```
Por eso `SynkraAppTest`, `BaseDeDatosTest` y `RepositorioSqliteTest` cargan un segundo `@EnableAutoConfiguration` y un `@ComponentScan` de `io.synkra.fronteras`. Hoy no rompe nada porque `Uno` y `Algo` no son componentes. Pero el plan de pruebas pide que la app de prueba "no se mezcle con los módulos reales", y lo hace en el contexto de ejecución de las pruebas. Basta con que alguien anote una clase del fixture para que entre al contexto real.

Correcciones posibles (las decide el codificador): anotar el fixture con `@org.springframework.modulith.Modulithic`, que está en el `testCompileClasspath` vía `spring-modulith-api` y no es `@Configuration`, o usar `ApplicationModules.of("io.synkra.fronteras", ImportOption)` sin tipo raíz. En los dos casos hay que releer los beans, no solo ver que la prueba pasa en verde.

### F-02 · AMARILLO · bitacoras/U03-T05.md:18 · La mutación descrita no compila tal como está escrita
La bitácora dice "quitando la importación de `dos.interno.Algo` en `Uno.java`" y pega `3 tests completed, 1 failed`. Quitar solo el `import` produce `error: cannot find symbol` en `Uno.java:6`; lo comprobé con `compileTestJava` en la copia. El resultado que reporta corresponde a quitar la importación y también el campo, que fue lo que reproduje. La conclusión es cierta, pero la descripción no reproduce. Además, la salida del "rojo primero" no incluye el mensaje de la aserción que falló, solo la línea `FAILED`.

### F-03 · AMARILLO · backend/src/test/java/io/synkra/FronterasModulosTest.java:37-39 · Comentario partido dentro de la expresión
El comentario sobre `DoNotIncludeTests` quedó entre `var modulos =` y su inicializador, probablemente por el formateador. Conviene ponerlo encima de la declaración.

### F-04 · AMARILLO · FronterasModulosTest.java:31 · Ruta de salida de Documenter incrustada
`Path.of("build/spring-modulith-docs/module-plataforma.adoc")` depende de que el directorio de trabajo sea `backend/` y de la carpeta por defecto de Documenter. No lo ve ningún usuario, así que no llega a NARANJA. Se podría derivar de `DocumentationOptions` o documentar la dependencia.

Desborde de alcance: ninguno. No hay cambios en `src/main` ni en `build.gradle.kts`, Modulith sigue solo en `testImplementation` y no se declaró `plataforma` como módulo abierto.

## Tareas candidatas (defectos reales fuera de alcance)
- `components.puml` sale vacío de componentes (`title SynkraApp` sin elementos), porque hoy hay un solo módulo sin dependencias. Cuando llegue U04 convendría afirmar sobre el `.puml` que el diagrama contiene los módulos.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/ca5.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/mutante.txt (violación real en src/main detectada)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/fuga.txt (bean `appFronteras` en el contexto real)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/mut-neg.txt (caso negativo sin la importación prohibida)
- Copia mutada: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u03t05/copia

No tengo herramientas de escritura, así que no creé `revisiones/U03-T05/ronda-1.md`. Este mensaje es el contenido del informe para que el orquestador lo guarde en esa ruta.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/test/java/io/synkra/fronteras/AppFronteras.java:6|La aplicación de prueba (@SpringBootApplication) se registra como bean en todos los @SpringBootTest del backend real
INFORME: revisiones/U03-T05/ronda-1.md
```
