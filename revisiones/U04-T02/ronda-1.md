# Ronda 1 — U04-T02

VEREDICTO: NO-VERDE

Los cuatro criterios de aceptación pasan cuando los corro yo. El veredicto es NO-VERDE por cuatro NARANJA: la expresión de `imagen` deja pasar entradas hostiles, faltan pruebas que aten R-C1 y la atomicidad, Modulith sigue exponiendo el paquete base de `catalogo` y los `motivo` son literales incrustados.

## Comprobaciones previas
- Hash de la tarea: `git hash-object tareas/U04-T02-registrar-core.md` = `3fc7ec316c7c3e4668a753f80ad3d87ce7809b09`. Coincide.
- HEAD = `5dc8b61`. La base de fusión con `origin/main` es `c4a033f`. Hay un solo commit.
- `git status --short` salió vacío antes y después de la revisión. Todas las sondas y mutaciones se hicieron en copias `git archive HEAD` dentro del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde la raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | R-C1 y R-C2 sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*RegistrarCoreTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="10" skipped="0" failures="0" errors="0"`. La corrida empezó a las 11:20:18 y el XML es de las 11:20:20. La primera prueba tarda 0,57 s por el contexto y las demás entre 5 y 18 ms, con SQLite real. Las 10 pruebas cubren los 9 puntos del criterio. |
| 2 | La validación usa U01 | los dos `grep` del criterio | pasa: `Descriptor.NOMBRE` en :44, `Version.parsear` en :48 y :87, y se imprime `sin copia`. Hay una sola expresión compilada (:27). |
| 3 | Fronteras y todo el backend | `./gradlew :backend:test --rerun :backend:check :verificador:check` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Los 11 XML del backend salen con `skipped=0 failures=0 errors=0`: Fronteras 3, SynkraApp 2, CatalogoRepositorios 19, InterfazCatalogo 1, RegistrarCore 10, BaseDeDatos 4, Dialecto 6, ErrorApi 13, RepositorioSqlite 4, RutaDatos 1, Salud 3. Los 12 XML de verificador también salen en 0. Que `api` sea interfaz con nombre lo afirma `InterfazCatalogoTest`; ver F-03. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa, con la salvedad de la desviación 1: 8 archivos, todos dentro de la lista. |

## Desviaciones declaradas
1. **`spring-modulith-api` en `implementation`.** La acepto. La quité en una copia y `:backend:compileJava` falla con `package org.springframework.modulith does not exist` (ver `dev1.log`). Bastaría con `compileOnly`, porque la anotación solo se lee como bytecode en las pruebas, pero eso es opcional.
2. **`InterfazCatalogoTest`.** La acepto como «prueba nueva». La mutación M18 (quitar `@NamedInterface`) solo la atrapa esta prueba. Sin embargo, su aserción `contains("api")` es demasiado débil; ver F-03.
3. **R-C2 por lectura previa y C-31 abierta.** La acepto. La sonda `carrera` lanzó 40 pares de altas simultáneas del mismo core y la misma versión. Resultado: `{ok=40, 409=40}`, 40 filas en `cat_core`, 40 en `cat_version_core` y ningún 500. El motivo es `BaseDeDatos.java:48` (`setMaximumPoolSize(1)`), que serializa las transacciones dentro del proceso. El 500 que describe la bitácora solo aparece con dos procesos sobre el mismo archivo o con un pool mayor que 1; eso es lo que cubre C-31.

## R-C2 y la transacción, leídas de vuelta
- Repetido: 409 con el detalle `version`. La imagen original y `count=1` se leen de vuelta (`RegistrarCoreTest`).
- Fallo del insert de la versión después de crear el core: lo forcé con un trigger `raise(abort)` sobre `cat_version_core`. Sale `DbActionExecutionException`, y después quedan `cat_core(nuevo)=0` y `versiones=0`. La fila del core se crea y se revierte dentro de la misma transacción.
- Ninguna prueba ata esa atomicidad: ver M06 en F-02.

## Hallazgos

### F-01 · NARANJA · `backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:26-27` · la expresión de `imagen` acepta mucho más que `registro/repo[:tag]@sha256:<hex>`
La expresión es `[^\s@/]+(/[^\s@/]+)+@sha256:[0-9a-f]{64}`, con `matches()`.

Lo que rechaza bien:
- espacio o `\n`/`\r\n` al final y espacio al principio (`matches` ancla a toda la entrada, así que el problema de `$` frente a `\z` no aplica);
- 65 hex, `sha512`, `SHA256`, `md5` y la falta de `:`;
- sin registro, nulo, vacío, `http://` y `@@`.

Lo que acepta con 200 (`sonda.log`):
- `registro/repo:tag:otro@…`: dos tags.
- `registro:1:2/repo@…`: varios dos puntos en el registro.
- `registro/repo:tag/sub@…`: tag en un segmento intermedio.
- `registro/repo:@…`: tag vacío.
- `../../etc/passwd@…`.
- `REGISTRO/REPO@…`: OCI no admite mayúsculas en la ruta del repo.
- `registro/rëpo@…`: unicode.
- `registro/re<NBSP>po`, `re<U+2028>po`, `re<U+0085>po` y `re<NUL>po`: en Java `\s` solo cubre espacios ASCII, así que caracteres de control y espacios unicode entran a la base y de ahí al manifiesto futuro.
- una imagen de 5000 caracteres.

Puerto con tag (`registro.local:5000/synkra/core:2.0.0@…`): se acepta, y es correcto.

**Lo que se exige.** Que cada segmento quede restringido a un juego ASCII explícito (`[a-z0-9._-]`, más `:puerto` en el registro), que haya como mucho un `:tag` y solo en el último segmento, y que haya pruebas para estos casos.

### F-02 · NARANJA · `backend/src/test/java/io/synkra/catalogo/RegistrarCoreTest.java` · mutaciones que sobreviven a toda la suite (barrido de clase)
Corrí `./gradlew :backend:test --rerun` sobre 20 mutantes (`mutaciones.txt`, script `mutar.sh`). Sobreviven estas:

| Mutante | Cambio | Qué deja sin atar |
|---|---|---|
| M02 | `{64}` → `{64,}` | se aceptan 65 hex o más |
| M03 | `matches()` → `find()` en la imagen | se acepta basura antes y después |
| M04 | `.+@sha256` | cualquier prefijo vale |
| M19 | `(/…)+` → `(/…)*` | el registro deja de ser obligatorio |
| M06 | quitar `@Transactional` de `registrarCore` | la creación del core y su primera versión dejan de ser atómicas; ninguna prueba lo detecta |
| M14 | quitar `nombre == null` | un `nombre` nulo da NPE y 500; ninguna prueba pasa un nombre ni una versión nulos |
| M13 | `motivo` en inglés | nada exige que estén en español |
| M20 | `CatalogoServicio` público | ver F-03 |
| M17 | guardar la `version` cruda | mutante equivalente, porque `Version.parsear` ya rechaza las formas no canónicas; no lo cuento |

Las atrapa `RegistrarCoreTest`: M01 (hex en mayúsculas), M05 (`find` en el nombre), M07 (sin la lectura previa), M08 y M09 (orden), M10 (core siempre insertado), M11 (404), M12 (campo del 409), M15 (detalles acumulados) y M16 (estado del 409).

**Lo que se exige.** Pruebas para:
- 65 hex y basura antes o después de la imagen;
- la imagen sin registro y los casos de F-01;
- `nombre` y `version` nulos, y los tres nulos a la vez;
- la atomicidad, con el trigger de mi sonda: falla la versión, entonces `cat_core` queda vacía;
- los `motivo` exactos.

### F-03 · NARANJA · `backend/src/main/java/io/synkra/catalogo/api/package-info.java:2` y `InterfazCatalogoTest.java:16` · Modulith sigue exponiendo el paquete base de `catalogo`
`getNamedInterfaces()` de `catalogo` devuelve `[<<UNNAMED>>, api]`, no solo `api`.

Lo probé en una copia con un `public class Publica` en `io.synkra.catalogo` y un módulo `io.synkra.sonda` que la usa. `FronterasModulosTest` pasa 3/3, así que `verify()` no lo detecta (`mod-base.log`).

Hoy la decisión 5 se cumple solo porque todos los tipos del paquete base son package-private, y nada lo ata: M20 sobrevive. `contains("api")` tampoco lo distingue.

**Lo que se exige.** Una prueba que falle si la interfaz sin nombre de `catalogo` expone algún tipo, o si existe un tipo público en `io.synkra.catalogo` (por ejemplo con ArchUnit).

### F-04 · NARANJA · `CatalogoServicio.java:45, 51, 54-55, 66` · cuatro `motivo` visibles para el usuario, incrustados como literales (barrido de clase)
Los textos de `nombre`, `version`, `imagen` y el del 409 son literales dentro de las ramas de validación. En cambio, `CodigosError` se declara como «un solo sitio» para esos textos.

Además, el de `version` duplica el texto de U01 y descarta el `Motivo` que ya trae el `Resultado.Fallo`, que es más preciso porque incluye el valor recibido. El de `imagen` repite a mano la forma de la expresión.

**Lo que se exige.** Que los textos vivan en constantes con nombre, que se reutilice el `motivo` de U01 para `version` y que una prueba fije los textos (cierra M13).

### F-05 · AMARILLO · `CatalogoServicio.java:87` · cast sin comprobar
`(Resultado.Exito<Version>) Version.parsear(...)` lanza `ClassCastException` (500) si en la base aparece una versión no canónica. Hoy solo escribe este servicio, pero un `switch` o un `orElseThrow` con un error claro sería más seguro.

### F-06 · AMARILLO · `bitacoras/U04-T02.md` (CA-4) · evidencia ausente
CA-4 dice «ver `git diff --stat` en el commit», sin comando ni salida. Es la misma clase que F-04 de U04-T01. La corrí yo y pasa.

## Tareas candidatas (defectos reales fuera de alcance)
- `Descriptor.NOMBRE` de U01 no limita la longitud: un nombre de 300 caracteres se registra. Kubernetes limita los nombres a 63, y la tarea prohíbe tocar `verificador/`.
- C-31, el traductor de `SQLITE_CONSTRAINT_*`, sigue abierta para el caso de varios procesos o un pool mayor que 1.
- `verificador` aparece ahora como un módulo de Modulith (`module-verificador.adoc`), porque `io.synkra.verificador` cuelga de `io.synkra`. Conviene decidir si se declara como módulo compartido o abierto antes de que crezca.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t02/`:
- `ca1.log` y `ca3.log`
- `dev1.log`: compilación sin `spring-modulith-api`
- `sonda.log`: entradas hostiles, rollback y carrera. La sonda está en `sonda/backend/src/test/java/io/synkra/catalogo/SondaHostilTest.java`.
- `mod-api.log` y `mod-base.log`: fronteras. Las copias son `mod-api/` y `mod-base/`.
- `mutaciones.txt`, `mut-M*.log` y `mutar.sh`. Las copias están en `mut/`.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:26-27|la expresión de imagen acepta varios tags, tag intermedio o vacío, ../, mayúsculas, unicode, NBSP/LS/NEL/NUL
NARANJA|backend/src/test/java/io/synkra/catalogo/RegistrarCoreTest.java|sobreviven M02, M03, M04, M19 (imagen), M06 (sin @Transactional), M14 (nombre nulo), M13 (motivos) y M20 (público)
NARANJA|backend/src/main/java/io/synkra/catalogo/api/package-info.java:2|catalogo expone <<UNNAMED>> además de api; verify() acepta un tipo público del paquete base
NARANJA|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:45,51,54,66|cuatro motivos incrustados como literales; el de version duplica U01
AMARILLO|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:87|cast sin comprobar a Resultado.Exito en leer()
AMARILLO|bitacoras/U04-T02.md|CA-4 sin comando ni salida
INFORME: revisiones/U04-T02/ronda-1.md
```
