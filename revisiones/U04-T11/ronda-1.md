# Ronda 1 — U04-T11

VEREDICTO: NO-VERDE

El código pasa los cuatro criterios en mis propias ejecuciones y las tres mutaciones de CA-2 se ponen en rojo en mi copia. Lo único que bloquea es la bitácora. La evidencia de CA-3 trae un conteo que este diff no puede producir, y CA-4 no tiene salida. El arreglo es pegar la salida real.

Tarea `tareas/U04-T11-fronteras.md`: hash 5582e7a5d8e42032ac4130c7d60fc5b92f8cbe50, coincide. HEAD dc7e76e, coincide con el reportado. El diff es un solo commit sobre origin/main.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las fronteras pasan sin caché | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*InterfazCatalogoTest'` + grep de los XML | pasa. `BUILD SUCCESSFUL`. FronterasModulosTest da tests=5 y InterfazCatalogoTest tests=6, con skipped=0, failures=0 y errors=0. Hay una prueba por cada regla de la decisión 1 y una para C-36 |
| 2 | Las reglas detectan las tres mutaciones | Copia hecha con `git archive dc7e76e` en el scratchpad (sin tocar el worktree) y la misma orden de CA-1 tras cada mutación | pasa (detalle abajo) |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de XML | pasa. `BUILD SUCCESSFUL in 15s`, 22 XML, 306 pruebas, 0 saltos, 0 fallos y 0 errores |
| 4 | El diff queda acotado | `git diff --stat origin/main...m6r/U04-T11` | pasa. 12 archivos: las pruebas de fronteras, los 3 fixtures renombrados, las 3 pruebas que los cargan, `SynkraAppTest`, `SynkraApp.java` (lo exige la decisión 2), `CANDIDATAS.md` y la bitácora |

Las mutaciones de CA-2, que corrí yo:
- **M1**, `RutasCores` pasa a `public`: fallan `ningunTipoPublicoFueraDeApi` y `laInterfazSinNombreNoExponeTipos` (2/11).
- **M2**, `CodigosCatalogo` pasa a public y `CatalogoApi` declara `Class<?> CODIGOS = io.synkra.catalogo.CodigosCatalogo.class`: fallan además `catalogoApiNoDependeDeTiposInternosDeCatalogo` (3/11).
- **M3**, el mismo cambio pero desde `plataforma.ExcepcionApi`: fallan además `plataformaNoDependeDeCatalogo` y `elBackendRealRespetaSusFronteras` (4/11).
- **M3b** es mía, para aislar la regla. `plataforma` usa solo `catalogo.api.CatalogoApi`, algo que Modulith permite salvo por el ciclo. Fallan `plataformaNoDependeDeCatalogo` y `verify` (2/11), así que la regla de ArchUnit no depende de que haya un tipo público.
- **M4**, sin `@Modulithic`: falla `verificadorEsModuloCompartido`.
- **M5**, los fixtures vuelven a `io.synkra.plataforma.prueba`: falla `losFixturesDePruebaDePlataformaNoEntranAlContexto`.
- **M6**, `.gitignore` sin `build/`: falla `laDocumentacionGeneradaNoSeVersiona`.

M1, M2 y M3 coinciden línea por línea con la bitácora.

### Las decisiones que pediste juzgar
- **C-14, fixtures en `fixtures.plataforma`.** Cumple la decisión 3. La propia decisión prevé mover los fixtures («Si mover los fixtures rompe…»). Salir de `io.synkra` es lo que los saca del escaneo de componentes y de Spring Data. Cada prueba que los usa los carga explícitamente: `ErrorApiTest` y `ValidacionErroresTest` con `@Import`, y `RepositorioSqliteTest` con `@TestConfiguration` + `@EnableJdbcRepositories`. `SynkraAppTest` afirma que ni `ControladorDePrueba` ni `ElementoRepositorio` están en el contexto, y M5 demuestra que esa afirmación sí detecta la regresión. **El comportamiento no cambia**: corrí origin/main en otra copia y comparé los conteos de las 22 clases. Solo cambian InterfazCatalogoTest (de 3 a 6), FronterasModulosTest (de 3 a 5) y SynkraAppTest (de 2 a 3). ErrorApiTest, ValidacionErroresTest y RepositorioSqliteTest tienen los mismos conteos y pasan. En `main/` solo cambia `SynkraApp.java`.
- **C-36, `@Modulithic(sharedModules = "verificador")`.** Es una de las dos vías que la decisión 2 permite, y no toca `verificador/`. Es correcto. El matiz va en F-03.
- **La documentación de Modulith.** Se genera en `backend/build/spring-modulith-docs/` (`components.puml` y `module-{catalogo,plataforma,verificador}.adoc`). `git check-ignore -v` la atribuye a `.gitignore:3:build/` y `git ls-files | grep -i modulith` sale vacío, así que no está versionada.

## Hallazgos
### F-01 · ROJO · bitacoras/U04-T11.md, CA-3 y CA-4 · Evidencia que no sale del diff actual y CA-4 sin salida
Para CA-3 la bitácora no pega la salida del bucle. La resume como «25 XML, 0 fallos». Este diff produce **22** XML, igual que origin/main: 22 clases de prueba, comprobado en las dos ramas. Tampoco puede venir de XML viejos que quedaran en el directorio. Lo comprobé en la copia: dejé un `TEST-io.synkra.Fantasma.xml` y Gradle lo borró en la siguiente ejecución. Ese «25» no pudo salir de `:backend:test --rerun :backend:check` sobre dc7e76e.

Del mismo tipo, en el mismo archivo: CA-4 dice «Ver `git diff --stat` … en el commit final» y no trae ni comando con salida ni la lista.

El código pasa las dos cosas en mis ejecuciones. Lo que falla es el registro, y basta con pegar la salida real de CA-3 (el conteo de los XML con su resultado) y de CA-4.

### F-02 · AMARILLO · InterfazCatalogoTest · Las reglas de ArchUnit no ven constantes en línea
Mi primera versión de M2 y M3 hacía referencia a `CodigosCatalogo.RUTA_NO_PERMITIDA`, que es `static final String`. Con eso, `catalogoApiNoDependeDeTiposInternosDeCatalogo` y `plataformaNoDependeDeCatalogo` quedaron **en verde**, porque javac copia la constante en el bytecode y la dependencia desaparece. Es un límite de ArchUnit, no del diff. Conviene dejarlo escrito en un comentario de la prueba.

### F-03 · AMARILLO · SynkraApp.java / CANDIDATAS.md C-36 · `verificador` sigue apareciendo como módulo
`sharedModules` hace que `verificador` entre siempre al arranque de los `@ApplicationModuleTest`, pero sigue siendo un módulo: `module-verificador.adoc` todavía se genera. La nota de cierre de C-36 es exacta. Solo falta aclarar que el síntoma original («aparece como módulo») sigue, porque es lo esperado.

### F-04 · AMARILLO · FronterasModulosTest.laDocumentacionGeneradaNoSeVersiona · Depende del binario `git` y de `..`
La prueba lanza `git check-ignore` desde `Path.of("..")`. En una compilación sin `.git` (un tarball, una imagen de compilación) falla con 128. Hoy no hay ninguna compilación así: el `Dockerfile` solo empaqueta un JAR ya hecho.

### F-05 · AMARILLO · RepositorioSqliteTest:30 · `basePackages = "io.synkra"` en literal
Repite a mano la raíz de escaneo de la app. Escrito como `basePackageClasses = {SynkraApp.class, ElementoRepositorio.class}` no se desalinea si la raíz cambia.

### F-06 · AMARILLO · InterfazCatalogoTest · Solo se cubre el paquete base como «interno»
`ningunTipoPublicoFueraDeApi` y `catalogoApiNoDependeDeTiposInternosDeCatalogo` solo miran el paquete base `io.synkra.catalogo`. Si mañana aparece `catalogo.persistencia`, ninguna de las dos lo cubre. Hoy solo existen el paquete base y `api`.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva que bloquee. F-02, F-04 y F-06 pueden ir como candidata si el orquestador quiere endurecer las reglas cuando llegue U06 o U07.

## Estado del worktree
Limpio después de la revisión (`git status --short` vacío). Solo quedan cachés ignoradas (`build/`, `backend/.jqwik-database`). Todas las mutaciones se hicieron en la copia del scratchpad. El `stash@{0}` de U04-T07 no lo toqué.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/`:
- `ca1.log`: CA-1
- `ca3.log`: CA-3
- `mut-M0.log` a `mut-M6.log`, más `mut-M3b.log`: mutaciones. `mut-M2.log` y `mut-M3.log` quedaron sobrescritos con la versión `.class`.
- `base.log` y `base.cnt`: línea base de origin/main
- `rama.cnt`: conteos de la rama

```
VEREDICTO: NO-VERDE
ROJO|bitacoras/U04-T11.md CA-3/CA-4|Evidencia de CA-3 con 25 XML imposibles en este diff (son 22) y CA-4 sin salida
INFORME: revisiones/U04-T11/ronda-1.md
```
