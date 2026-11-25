# Ronda 1 — U03-T03

VEREDICTO: NO-VERDE

Los 3 criterios pasan en mi propia ejecución. El 500 no filtra la traza, ni en la prueba ni en peticiones HTTP reales. Los extras (404 de ruta, 405, 415 y 413 de Spring) quedan dentro del alcance. Lo único que bloquea es un NARANJA por la invariante 8: códigos y mensajes visibles para el usuario puestos como literales en el `switch` de decisión, y además duplicados.

Contexto comprobado: el hash de la tarea es `607c3fb0…`, coincide. HEAD es `d0d1db7`, coincide con lo reportado. Diff revisado: `git diff m6r/U03-T02...m6r/U03-T03`, 7 archivos, +303. El worktree estaba limpio antes y después de la revisión.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Prueba del formato, sin caché | `./gradlew :backend:test --rerun --tests '*ErrorApiTest'` | pasa: `BUILD SUCCESSFUL in 4s`, reporte con timestamp `2026-11-25T20:40:53Z` (de esta misma corrida) |
| 2 | Lectura de vuelta del XML, ≥7 y nada saltado | `grep -ho 'tests=… ' backend/build/test-results/test/TEST-*ErrorApiTest.xml` | pasa: `tests="10" skipped="0" failures="0" errors="0"`. Hay un caso por estado: 400, 404, 409, 413, 422. También JSON mal formado da 400, `Exception` da 500 sin traza, y hay 3 extras: 413 de Spring, 404 de ruta y 405. Los tiempos van de 0,004 a 0,925 s y no son cero (el primer caso carga el contexto) |
| 3 | El resto del backend sigue pasando | `./gradlew :backend:check --rerun` | pasa: `BUILD SUCCESSFUL in 10s`. Reportes: BaseDeDatos 3, DialectoSqlite 6, ErrorApi 10, RepositorioSqlite 4, RutaDatosPorDefecto 1, SynkraApp 1. `skipped=0` en todos |

Las afirmaciones de la bitácora (3 de 3, CA-2 con tests=10 y el rojo primero registrado) cuadran con mi corrida. No encontré evidencia que no pudiera haber salido de este diff.

## Petición real (más allá de MockMvc)
Arranqué `io.synkra.SynkraApp` con el classpath de pruebas, así que `ControladorDePrueba` se registra con Tomcat real. El arranque fue en el puerto 18089, con `SYNKRA_DATA` apuntando al scratchpad y multipart limitado a 1KB. El classpath lo saqué con un init script de Gradle guardado en el scratchpad: no toqué el worktree. Resultados:
- `GET /prueba/500` da `500`, `Content-Type: application/json`, cuerpo `{"codigo":"ERROR_INTERNO","mensaje":"Error interno del servidor","detalles":[]}`.
  - Las cabeceras solo traen Content-Type, Transfer-Encoding, Date y Connection: nada de traza, clase ni mensaje de la excepción.
  - Probé también con `?trace=always`, `Accept: text/html` y `Accept: application/xml`: siempre el mismo cuerpo JSON y nunca la página de error de Spring.
  - La traza solo aparece en el log del servidor: `secreto interno` sale 4 veces en `app.log`, y eso es lo esperado.
- `GET /no/existe` da `404 NO_ENCONTRADO`.
- `POST /prueba/404` da `405`, con `Allow: GET` y `SOLICITUD_INVALIDA`.
- `POST text/plain` da `415` y `SOLICITUD_INVALIDA`.
- JSON mal formado da `400 SOLICITUD_INVALIDA`.
- `GET /prueba/422` da el 422 con 2 detalles `{campo, motivo}`.
- **Multipart real de 3000 bytes con límite de 1KB** da `413 DEMASIADO_GRANDE` en formato `ErrorApi`. Esto confirma que el 413 de Spring se traduce también fuera de la excepción lanzada a mano en la prueba.

## Alcance
Los extras están dentro del alcance. La nota de la tarea pide que el manejador traduzca «las excepciones propias de Spring (cuerpo ilegible, tamaño excedido)», y H-24 dice «cualquier error de la API… el mismo formato». Extender `ResponseEntityExceptionHandler` es el mecanismo natural para eso, y 404 de ruta, 405 y 415 caen ahí sin código extra. Lo que la tarea deja fuera no aparece en el diff:
- no se manejan 401 ni 403;
- no hay controladores en `src/main`;
- `backend` no depende de `:verificador`;
- no se toca OpenAPI, salud ni la base de datos.

## Hallazgos
### F-01 · NARANJA · backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:29-31, 47-67 · Códigos y mensajes visibles para el usuario como literales en el punto de decisión, y duplicados
- **El problema:** `codigoDe` y `mensajeDe` son el punto de decisión entre estado HTTP y lo que lee el usuario, y ahí todo va escrito como literal: `"NO_ENCONTRADO"`, `"CONFLICTO"`, `"DEMASIADO_GRANDE"`, `"ENTIDAD_NO_PROCESABLE"`, `"ERROR_INTERNO"`, `"SOLICITUD_INVALIDA"` y los 7 mensajes.
- **Duplicación:**
  - `"ERROR_INTERNO"` aparece 2 veces (en `imprevista` y en el `switch`).
  - `"Error interno del servidor"` aparece 3 veces (`imprevista`, `case INTERNAL_SERVER_ERROR` y `default`).
  - `"SOLICITUD_INVALIDA"`/`"Solicitud invalida"` se repite de nuevo en `ControladorDePrueba`.
- **Por qué importa:** los códigos son el contrato de la API que U04 en adelante deben reutilizar, según las notas de la tarea. Hoy cada módulo tendrá que volver a teclear `"NO_ENCONTRADO"` o `"CONFLICTO"` a mano, sin ninguna fuente única.
- **Arreglo sugerido:** constantes nombradas (por ejemplo, un tipo de códigos de plataforma) que usen tanto el manejador como los controladores, y un único sitio para el mensaje del 500.
- **Por qué es NARANJA:** la tarea fija qué valores existen, pero no dónde viven. La invariante 8 marca NARANJA como mínimo. Si el orquestador decide que los códigos pedidos por la tarea quedan exentos, el resto del hallazgo baja a AMARILLO por duplicación.

### F-02 · AMARILLO · backend/src/test/java/io/synkra/plataforma/ErrorApiTest.java:90-95 · Los extras traducidos no tienen todos una afirmación afilada
- `metodoNoPermitidoDa405ConElMismoFormato` solo afirma `$.codigo` con `isString()`, no el valor, y no revisa `mensaje`.
- El 415 está traducido (`mensajeDe` tiene su `case`) pero ninguna prueba lo cubre: solo lo verifiqué yo a mano.
- El 413 «de Spring» se prueba lanzando `MaxUploadSizeExceededException` desde el controlador, no con un multipart que supere el límite. Mi petición real muestra que sí funciona, pero ninguna prueba lo protege.
- Ninguno de estos casos es un criterio de la tarea, así que no bloquean.

### F-03 · AMARILLO · ManejadorErrores.java:60-64 y ControladorDePrueba · Mensajes visibles en español sin tildes ni eñe
`"El archivo supera el tamano permitido"`, `"Metodo no permitido"`, `"La solicitud es invalida…"`, `"Solicitud invalida"`. El cliente los lee tal cual. Jackson emite UTF-8 sin problema, así que no hay razón técnica para omitir los acentos.

### F-04 · AMARILLO · ManejadorErrores.java:38 y 47-55 · Detalles menores de la traducción de Spring
- `HttpStatus.valueOf(statusCode.value())` lanza `IllegalArgumentException` si llega un código no estándar. Spring no los produce hoy, pero `HttpStatus.resolve` con una alternativa a 500 sería más robusto.
- 405 y 415 se aplanan a `SOLICITUD_INVALIDA`. Es aceptable, pero un cliente no puede distinguirlos solo por `codigo`.

## Tareas candidatas (defectos reales fuera de alcance)
- Mapear `MethodArgumentNotValidException` y `HandlerMethodValidationException` (`@Valid`) a `detalles` con `campo` y `motivo` reales, en vez del `detalles: []` genérico. Hoy caen en `handleExceptionInternal` y pierden los campos. Conviene hacerlo cuando U04 introduzca DTO validados.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/app.log (servidor real; la traza del 500 solo está aquí)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/cp.gradle (init script de solo lectura para obtener el classpath)

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:29-31,47-67|Códigos y mensajes visibles para el usuario como literales en el punto de decisión, y duplicados
INFORME: revisiones/U03-T03/ronda-1.md
```
