# Ronda 1 — U04-T09

VEREDICTO: NO-VERDE

Los cuatro criterios de aceptación pasan con mis propias ejecuciones. Quedan dos hallazgos NARANJA, ambos de cobertura, que impiden el VERDE.

Lo que revisé:
- **Tarea:** hash `ada7bbd520afdbde9b0b6edec193d50d25d2aa51`, coincide.
- **Rama:** HEAD `4b999e0`, coincide con el sha reportado.
- **Diff:** `git diff origin/main...m6r/U04-T09`, 6 archivos, +275/−1.
- **Entorno:** JDK 25.0.5.1 (Temurin), Gradle 9.9.2.
- **Puerto:** no arranqué el backend, así que el 18080 no se usó.
- **Worktree:** limpio al terminar (`git status --short` sin salida).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | S1 a S7 sobre SQLite, sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*CatalogoVerificadorTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="13" skipped="0" failures="0" errors="0"`. El XML se escribió a las 12:05:12, a 8 s de mi ejecución, así que es fresco. S1 a S7 aparecen como 7 testcase distintos. |
| 2 | La interfaz pública sigue cerrada | `./gradlew :backend:test --rerun --tests '*InterfazCatalogoTest' --tests '*FronterasModulosTest'` | pasa: `BUILD SUCCESSFUL`, 3/0/0/0 en cada una |
| 3 | Todo backend y verificador pasan | `./gradlew :backend:test --rerun :backend:check :verificador:check` + barrido de los XML | pasa: `BUILD SUCCESSFUL` y todos los XML de backend con skipped/failures/errors en 0. `:verificador:test` salió UP-TO-DATE, así que lo repetí con `./gradlew :verificador:test --rerun`: `BUILD SUCCESSFUL`, 12 XML, todos en 0. |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: solo `build.gradle.kts` (una línea de testFixtures), `catalogo/**` (main y la prueba nueva) y la bitácora |

**Tiempos de la suite.** CA-1 tardó 4 s porque reutiliza un daemon y los compilados. `:backend:test` sí se ejecutó, y el XML trae tiempos por caso, como los 0,646 s del primero, que arranca el contexto. No es la señal de pruebas saltadas.

## Juicio sobre la decisión del codificador
Puse la elección contra lo que pide la tarea y contra las fronteras del módulo. **Encaja.**
- **Ubicación.** `CatalogoSqlite` es package-private y está en `io.synkra.catalogo`. `CatalogoApi`, en `catalogo.api`, gana el método de fábrica `catalogoParaVerificar()`. Es la segunda forma que permite la Decisión 1 ("un tipo o un método de fábrica"). No aparece ningún tipo público nuevo en el paquete base, e `InterfazCatalogoTest` (`ningunTipoPublicoFueraDeApi`) pasa.
- **Fronteras.** El tipo que devuelve, `io.synkra.verificador.Catalogo`, es del proyecto Gradle `:verificador`, que es una biblioteca y no un módulo Modulith. `FronterasModulosTest` pasa.

**¿Corre de verdad contra SQLite?** Sí, no hay ningún doble:
- La prueba es `@SpringBootTest` con un `@TempDir` estático pasado a `synkra.data`.
- Carga la suite solo por `api.registrarCore` y `api.subirPlugin`, con JARs armados con `LectorDescriptor.aYaml`.
- Consulta `api.catalogoParaVerificar()`, que devuelve el bean `CatalogoSqlite`. Es la única implementación de `Catalogo` en `backend/src/main`.
- El "rojo primero" de la bitácora (8 de 13 fallan con el catálogo vacío) es coherente con eso.

**Aislamiento por core.** Las tres consultas llevan el core (`findByCore`, `findByCoreAndNombreAndVersion`, `findByCoreAndPluginAndVersion`). `unaCarpetaNoVeLosPluginsDeOtra` lo cubre en los dos sentidos.

**N+1.** No hay dentro de la implementación:
- `versionesCore`: 1 consulta.
- `versionesPlugin`: 1 consulta, aunque trae de más (ver F-04).
- `descriptor`: 2 consultas fijas.

Lo que sí existe es un N+1 que viene de la forma de la interfaz de U01: `Verificador.alternativas` llama a `descriptor` por cada versión. Con "sin caché" fuera de alcance, lo anoto como tarea candidata.

## Hallazgos

### F-01 · NARANJA · CatalogoVerificadorTest.java:85-92 · Para S4 y S5 no se compara el veredicto completo de la suite, solo `verificar`
La suite de U01 (`SuiteCombinaciones.java:220-232`) define S4 y S5 con alternativas esperadas. Son `PLUGIN kyc-pe 1.3.0` + `CORE 3.0.0`, y `CORE 2.1.0`. Así es como las ejecuta el propio U01 (`SuiteCombinacionesTest.java:54-64`: `verificar` y después `alternativas`).

La prueba nueva solo llama a `Verificador.verificar`. Según `Verificador.java:114-117`, `verificar` solo mira `catalogo.descriptor(...).isEmpty()`. Por eso S1 a S7 no ejercitan nada de lo que el catálogo realmente aporta:
- el `coreCompatible` rearmado desde SQLite;
- `versionesPlugin`;
- `versionesCore`.

Esos tres solo los usa `alternativas`. Una implementación que devolviera cualquier `Descriptor` no vacío para las tripletas existentes pasaría los siete casos. `descriptorNoTraeLasEtiquetasDeSubida` mitiga en parte, porque compara un descriptor entero, pero es uno de `core-etiquetas` y no de la suite.

Lo que se pide: para cada caso S1 a S7, comparar también `Verificador.alternativas(sel, verificar(sel, sqlite), sqlite)` con el resultado en memoria y con `caso.esperado().alternativas()`. Es justamente el uso que tendrá U07.

### F-02 · NARANJA · CatalogoSqlite.java:66-77 · El comportamiento de fila no parseable de la Decisión 2 no tiene prueba
La Decisión 2 exige que "una fila que no se puede parsear es un error interno (500) y no se oculta". El código lanza `IllegalStateException` desde `version(...)` y desde `rango(...)`, y `ManejadorErrores` la convierte en un 500 genérico (`ErrorApiTest`). Pero ninguna prueba siembra una fila corrupta en `cat_plugin` o `cat_version_core` para comprobar que `descriptor`, `versionesCore` y `versionesPlugin` lanzan en vez de devolver vacío.

Ya hay precedente de este tipo de prueba en el módulo: `SubirPluginTest.java:728-730`.

Lo que se pide: insertar con JDBC una fila con `core_compatible` y otra con `version` no parseables, y afirmar `IllegalStateException` en los tres métodos.

### F-03 · AMARILLO · CatalogoSqlite.java:21 · El literal de origen está duplicado
`ORIGEN_DESCRIPTOR = "descriptor"` ya existe como `private` en `SubidaPlugin.java:48`, y también `DESCRIPTOR` en la línea 46. Si cambia uno sin el otro, `descriptor()` devuelve el mapa de etiquetas vacío sin que nada falle.

La sugerencia es una única constante package-private compartida por las dos clases. No es visible para el usuario, así que no sube a NARANJA.

### F-04 · AMARILLO · CatalogoSqlite.java:41-45 · `versionesPlugin` trae todos los plugins del core para filtrar uno
Usa `plugins.findByCore(core)` y luego filtra por nombre en memoria. Es correcto y no es N+1, pero trae filas de más en cada llamada. Hace falta una consulta derivada `findByCoreAndNombre` en `PluginRepositorio`.

### F-05 · AMARILLO · CatalogoSqlite.java:18 · El bean se puede inyectar como `Catalogo` sin pasar por `catalogo.api`
`CatalogoSqlite` es un `@Component` cuyo tipo público es `io.synkra.verificador.Catalogo`. Un bean de otro módulo, como U07, podría hacer `@Autowired Catalogo` y saltarse `CatalogoApi`. Modulith no lo detectaría, porque la interfaz no pertenece a `catalogo`.

Vale anotarlo en la bitácora, o registrar el bean de forma que solo se obtenga por `CatalogoApi`. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- **N+1 de la interfaz U01.** En `Verificador.alternativas`, `versionesPlugin` va seguido de un `descriptor` por versión, que con `CatalogoSqlite` son 2 consultas cada una. Habría que evaluar un método de lote en `Catalogo` o una caché por petición, cuando la caché deje de estar fuera de alcance.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.log`

```
VEREDICTO: NO-VERDE
NARANJA|CatalogoVerificadorTest.java:85-92|Para S4 y S5 no se compara el veredicto completo de la suite, solo verificar
NARANJA|CatalogoSqlite.java:66-77|El comportamiento de fila no parseable de la Decisión 2 no tiene prueba
AMARILLO|CatalogoSqlite.java:21|El literal de origen está duplicado
AMARILLO|CatalogoSqlite.java:41-45|versionesPlugin trae todos los plugins del core para filtrar uno
AMARILLO|CatalogoSqlite.java:18|El bean se puede inyectar como Catalogo sin pasar por catalogo.api
INFORME: revisiones/U04-T09/ronda-1.md
```
