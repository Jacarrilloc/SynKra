# Ronda 2 — U04-T09

VEREDICTO: VERDE

Corrí los cuatro criterios de aceptación yo mismo y pasan los cuatro. Los dos NARANJA de la ronda 1 están cerrados, y además los comprobé con mutaciones hechas en un clon aparte. Los tres AMARILLO también se atendieron. No queda ningún ROJO ni NARANJA. Dejo tres AMARILLO nuevos, que no bloquean.

**Lo que revisé**
- **Tarea:** hash `ada7bbd520afdbde9b0b6edec193d50d25d2aa51`, coincide.
- **Rama:** HEAD `d052dd7`, coincide con el sha reportado.
- **Diff:** `git diff origin/main...m6r/U04-T09`, 10 archivos, +419/−2.
- **Worktree:** limpio al terminar (`git status --short` sin salida). Las mutaciones las hice en un clon dentro de mi scratchpad, nunca en el worktree.
- **`revisiones/U04-T09/ronda-1.md`:** `git diff --quiet 94a5cf1 HEAD -- revisiones/U04-T09/ronda-1.md` sale con 0. Es idéntico al commit 94a5cf1, que es el único que toca ese archivo. Su contenido es el informe que entregué.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | S1 a S7 sobre SQLite, sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*CatalogoVerificadorTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="14" skipped="0" failures="0" errors="0"`. Lancé a las 12:10:41 y el XML es de las 12:10:45, así que es fresco. S1 a S7 aparecen como 7 testcase. |
| 2 | La interfaz pública sigue cerrada | `./gradlew :backend:test --rerun --tests '*InterfazCatalogoTest' --tests '*FronterasModulosTest'` | pasa: `BUILD SUCCESSFUL`, cada una con 3/0/0/0 |
| 3 | Todo backend y verificador pasan | `./gradlew :backend:test --rerun :backend:check :verificador:check` + barrido de XML | pasa: `BUILD SUCCESSFUL`. Los 20 XML de backend tienen skipped, failures y errors en 0, y ninguno es anterior a mi ejecución. `:verificador:test` salió UP-TO-DATE, así que lo repetí con `./gradlew :verificador:test --rerun`: `BUILD SUCCESSFUL`, 12 XML, todos en 0. |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: `build.gradle.kts` (solo la línea de testFixtures), `catalogo/**` en main y en test, la bitácora y `revisiones/U04-T09/` |

**Tiempos.** CA-1 tarda 4 s porque reutiliza el daemon. `:backend:test` sí se ejecutó: el primer caso tarda 0,666 s, que es el arranque del contexto. No es la señal de pruebas saltadas.

## Cierre de los hallazgos de la ronda 1
**F-01 (NARANJA), cerrado.** Para cada caso S1 a S7, la prueba ahora compara `Verificador.alternativas` sobre SQLite con el resultado en memoria y con `caso.esperado().alternativas()`. En S4 se esperan dos alternativas y en S5 una, así que la comparación no es trivialmente vacía (`SuiteCombinaciones.java:220-230`). Lo comprobé con dos mutaciones en el clon:
- `versionesCore` devolviendo `List.of()`: fallan S4, S5, `versionesDeCoreYPluginCoincidenConLaSuite` y la prueba de fila corrupta (4 de 14).
- `coreCompatible` fijo en `"2.x"`: fallan S4, la prueba de fila corrupta y `descriptorNoTraeLasEtiquetasDeSubida` (3 de 14).

Las transcripciones están en `mut1.log` y `mut2.log`.

**F-02 (NARANJA), cerrado.** `unaFilaNoParseableEsErrorInternalYNoSeOculta` siembra con JDBC una versión de core corrupta, un plugin con `core_compatible` corrupto y un plugin con versión corrupta. Afirma `IllegalStateException` en `versionesCore`, `versionesPlugin` y `descriptor`. Las dos mutaciones de arriba también la hacen fallar.

**F-03 (AMARILLO), cerrado.** Queda una sola constante, `SubidaPlugin.ORIGEN_DESCRIPTOR`, que pasa a package-private, y `CatalogoSqlite` la reutiliza.

**F-04 (AMARILLO), cerrado.** Se agregó `PluginRepositorio.findByCoreAndNombre`, que es una consulta derivada, y `versionesPlugin` la usa.

**F-05 (AMARILLO), cerrado.** Revisé lo que me pediste:
- **Transacciones.** Nada se pierde. El `CatalogoSqlite` de la ronda 1 era un `@Component` sin `@Transactional` (`git show 4b999e0:...CatalogoSqlite.java`), y cada consulta ya corría en la transacción de solo lectura del repositorio. Sigue igual. El `@Transactional` de `CatalogoServicio.java:60` es de otro método y no cambia.
- **Inyección.** `CatalogoServicio` (`@Service`) recibe por constructor los proxies de Spring Data `PluginRepositorio` y `EtiquetaRepositorio`, y se los pasa a `new CatalogoSqlite(...)`. El contexto arranca y las 14 pruebas consultan SQLite real a través de esos proxies.
- **Tipos públicos.** No aparece ninguno nuevo. Ninguna línea añadida en `backend/src/main` declara `public class|interface|record|enum`. `CatalogoSqlite` sigue package-private, solo se construye en `CatalogoServicio.java:51` y no hay otro bean `Catalogo` que pueda inyectarse. `InterfazCatalogoTest` pasa.

## Hallazgos
### F-06 · AMARILLO · CatalogoVerificadorTest.java:163-166 · Los INSERT corruptos son posicionales y comparten la base con las demás pruebas
`insert into cat_plugin values (...)` va sin lista de columnas, así que se rompe en silencio con cualquier cambio de orden o de número de columnas del esquema. Además, las filas `core-roto` quedan en la base compartida por todo el contexto. Hoy ninguna otra prueba consulta ese core, pero dependen del orden. La sugerencia es nombrar las columnas.

### F-07 · AMARILLO · CatalogoServicio.java:47,144 y CatalogoApi.java:41 · Nombre calificado en lugar de `import`
`io.synkra.verificador.Catalogo` aparece calificado entero en tres sitios. En la prueba pasa lo mismo con `assertThatThrownBy` y con `JdbcTemplate`. Es solo de estilo.

### F-08 · AMARILLO · CatalogoVerificadorTest.java:159 · Nombre de prueba mezclado
`ErrorInternal` mezcla español e inglés; lo coherente con el resto sería `ErrorInterno`.

## Tareas candidatas (defectos reales fuera de alcance)
- El N+1 de la interfaz de U01 (`alternativas` hace `versionesPlugin` y luego un `descriptor` por versión) sigue siendo una candidata del orquestador. No se pide aquí.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca1.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca2.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca3.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/ca3b.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/mut1.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2/mut2.log`

```
VEREDICTO: VERDE
AMARILLO|CatalogoVerificadorTest.java:163-166|Los INSERT corruptos son posicionales y comparten la base con las demás pruebas
AMARILLO|CatalogoServicio.java:47,144 y CatalogoApi.java:41|Nombre calificado en lugar de import
AMARILLO|CatalogoVerificadorTest.java:159|Nombre de prueba mezclado
INFORME: revisiones/U04-T09/ronda-2.md
```
