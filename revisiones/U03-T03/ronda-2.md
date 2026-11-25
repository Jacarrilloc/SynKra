# Ronda 2 — U03-T03

VEREDICTO: VERDE

Corrí los tres criterios yo mismo, sin caché, y los tres pasan. F-01, el único NARANJA de la ronda 1, está resuelto: no queda ningún literal de código ni de mensaje fuera de `CodigosError`. De F-02 a F-04 se corrigió lo pedido, salvo la prueba de multipart real. Ese empujón es aceptable, pero el razonamiento del codificador está a medias (ver F-01 de esta ronda). No queda ningún ROJO ni NARANJA en pie.

Contexto comprobado:
- El hash de la tarea es `607c3fb08013b42cae8e193716c1c2adbf531846`, coincide.
- HEAD es `10c59e9`, coincide con lo reportado.
- Diff revisado: `git diff m6r/U03-T02...m6r/U03-T03`, 10 archivos y +450 líneas (7 de código, más la bitácora, el informe de la ronda 1 y la respuesta).
- El worktree estaba limpio antes y después (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba del formato pasa, sin caché | `./gradlew :backend:test --rerun --tests '*ErrorApiTest'` | pasa: `BUILD SUCCESSFUL in 5s`. El reporte tiene timestamp `2026-11-25T20:45:58Z` y mi reloj marcaba `20:46:02Z`, así que sale de esta corrida |
| 2 | Lectura de vuelta del XML: al menos 7 casos y ninguno saltado | `grep -ho 'tests=…' backend/build/test-results/test/TEST-*ErrorApiTest.xml` | pasa: `tests="13" skipped="0" failures="0" errors="0"`. Hay un caso por cada estado (400, 404, 409, 413, 422). El JSON mal formado da 400 y la excepción no prevista da 500 sin traza (`secreto interno`, `IllegalStateException` y `at io.synkra` ausentes del cuerpo). Los tiempos van de 0,006 a 0,959 s: ninguno es cero y el primero carga el contexto |
| 3 | El resto del backend sigue pasando | `./gradlew :backend:check --rerun` | pasa: `BUILD SUCCESSFUL in 11s`. Reportes: BaseDeDatos 3, DialectoSqlite 6, ErrorApi 13, RepositorioSqlite 4, RutaDatosPorDefecto 1, SynkraApp 1. Todos con `skipped=0`, todos con timestamp de esta corrida |

La bitácora de la ronda 2 cuadra con mi corrida:
- Rojo primero: 4 de 13 pruebas fallan contra el manejador de la ronda 1. Es coherente: las pruebas nuevas afirman códigos y mensajes con tilde que antes no existían.
- Verde: 13 de 13.

No hay evidencia que no pudiera haber salido de este diff.

## Comprobación contra el servidor real (más allá de MockMvc)
Repetí la prueba de la ronda 1. Levanté `io.synkra.SynkraApp` con el classpath de pruebas, sobre Tomcat real en el puerto 18090, con `SYNKRA_DATA` y los logs en el scratchpad.

| Petición | Respuesta |
|---|---|
| `GET /prueba/500` | `500`, `application/json`, `ERROR_INTERNO` / «Error interno del servidor» |
| `GET /prueba/500?trace=always` con `Accept: text/html` | el mismo JSON; la traza solo aparece en el log (`secreto interno` sale 2 veces en `app-r2b.log`) |
| `GET /no/existe` | `404 NO_ENCONTRADO` |
| `POST /prueba/404` | `405`, con `Allow: GET`, `METODO_NO_PERMITIDO` / «Método no permitido» (el código nuevo funciona) |
| `POST text/plain` | `415 TIPO_NO_SOPORTADO` / «Tipo de contenido no soportado» (el código nuevo funciona) |
| JSON mal formado | `400 SOLICITUD_INVALIDA` / «La solicitud es inválida…», con las tildes bien en UTF-8 |
| `GET /prueba/422` | `422` con 2 `detalles {campo, motivo}` |
| Multipart real de 3000 bytes con límite de 1KB | `413 DEMASIADO_GRANDE` / «El archivo supera el tamaño permitido» |

Una nota para que no confunda a nadie: el primer intento de sondeo devolvió `400 text/html`. Fue culpa de mi script, no del código. Bajo zsh, `set -- $r` no separa las palabras, así que curl envió como método la cadena entera, y eso es una línea de petición inválida que rechaza Tomcat antes de llegar a Spring. Lo repetí con peticiones explícitas y salió la tabla de arriba.

## Alcance
- Los dos códigos nuevos, `METODO_NO_PERMITIDO` y `TIPO_NO_SOPORTADO`, están dentro del alcance. La nota de la tarea dice que «el manejador también traduce las excepciones propias de Spring», y H-24 pide el mismo formato para todo error. Además responden al AMARILLO F-04 de la ronda 1.
- No añaden estados nuevos al manejador: 405 y 415 ya se traducían en la ronda 1 y ahora solo tienen un código propio.
- Lo que la tarea deja fuera no aparece en el diff: ni 401/403, ni dependencia de `:verificador`, ni controladores en `src/main`, ni OpenAPI, salud o base de datos.
- C-07 (pasar `@Valid` a `detalles`) no se toca, como corresponde: está fuera de alcance.
- `grep` de literales en mayúsculas en `src/main/.../plataforma`, excluyendo `CodigosError.java`: cero resultados.

## Hallazgos
### F-01 · AMARILLO · ErrorApiTest.java:59-63 · El empujón del multipart real: la premisa es cierta, la conclusión no
- **Lo que tiene razón el codificador:** MockMvc construye un `MockMultipartHttpServletRequest` y no pasa por el análisis de partes de Tomcat. Por eso no puede reproducir el límite de `spring.servlet.multipart.max-file-size`.
- **Lo que no se sigue:** «solo se puede simular lanzando la excepción» no es cierto. Una prueba `@SpringBootTest(webEnvironment = RANDOM_PORT)` con un límite bajo y un cliente HTTP real lo cubre. Es justo lo que hice a mano, y responde `413 DEMASIADO_GRANDE`.
- **Lo que sí está cubierto:** `tamanoExcedidoDeSpringDa413ConElMismoFormato` recorre el camino de `ResponseEntityExceptionHandler` → `handleExceptionInternal` → `errorDe(PAYLOAD_TOO_LARGE)`. Lo que no protege ninguna prueba es el enlace entre el resolvedor de multipart del contenedor y esa excepción.
- **Por qué no bloquea:** ningún criterio lo pide, el plan fija `@WebMvcTest` y yo lo verifiqué en real dos rondas seguidas. Si se quiere esa protección, el sitio natural es una tarea posterior que suba archivos (U04 en adelante), no esta.

### F-02 · AMARILLO · ErrorApiTest.java:46,51,56 · Uso inconsistente de las constantes en las pruebas
- `estado404`, `estado409` y `estado413` afirman `"NO_ENCONTRADO"`, `"CONFLICTO"` y `"DEMASIADO_GRANDE"` como literales.
- `estado400` y el resto usan `CodigosError.*`.
- Es solo código de prueba y no es un punto de decisión, así que no bloquea.
- `losMensajesDePlataformaLlevanTildes` repite a propósito el literal «Método no permitido». Eso está bien: su objetivo es afirmar las tildes, independientemente de la constante.

## Tareas candidatas (defectos reales fuera de alcance)
- Una prueba de integración con contenedor real (`@SpringBootTest(RANDOM_PORT)` y un límite de multipart bajo) que fije que una subida demasiado grande da `413 DEMASIADO_GRANDE` en formato `ErrorApi`. Conviene hacerla con la primera tarea que suba compilados.
- C-07 (pasar `@Valid` a `detalles`) sigue vigente, sin cambios.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/app-r2.log (servidor real con límite de 1KB, multipart → 413)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/app-r2b.log (servidor real; la traza del 500 solo está aquí)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/cp.gradle (init script de solo lectura para obtener el classpath)

```
VEREDICTO: VERDE
AMARILLO|backend/src/test/java/io/synkra/plataforma/ErrorApiTest.java:59-63|Multipart real sin prueba: MockMvc no aplica el límite, pero @SpringBootTest(RANDOM_PORT) sí podría
INFORME: revisiones/U03-T03/ronda-2.md
```
