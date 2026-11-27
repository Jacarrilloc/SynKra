# Ronda 2 — U04-T03

VEREDICTO: NO-VERDE

Corrí yo los cuatro criterios y los cuatro pasan. F-01 a F-06 y F-09 de la ronda 1 quedan cerrados con mis propias corridas, y F-08 pasa a candidata por decisión del orquestador. Queda un hallazgo NARANJA nuevo, y lo introduce el propio arreglo de esta ronda: un tipo equivocado vuelve a recortar el 422 y rompe otra vez el «todos» de R-C1. Hay además cinco AMARILLO.

## Comprobaciones previas
- **Hash de la tarea:** `d7226e61e300ec8b80eac90eda189a1f7b5d1613`. Coincide.
- **HEAD y base:** HEAD es `15d8400` y la base de fusión es `a6b7bfe`.
- **Commits desde la ronda 1:** `cc3b964` (el informe de la ronda 1) y `15d8400` (el retrabajo).
- **Worktree:** `git status --short` salió vacío antes y después.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Rutas según el contrato | `./gradlew :backend:test --rerun --tests '*RutasCoresTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="11" skipped="0" failures="0" errors="0"`. La primera prueba tarda 0,84 s por el arranque del contexto y las demás entre 11 y 62 ms, con Tomcat real. |
| 2 | Los `curl` de H-01 contra el JAR en el 18080 | el bloque de CA-2 al pie de la letra (`r1/ca2.sh`) | pasa: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409`, `[('synkra-core', '2.1.0'), ('synkra-core', '3.0.0')]` |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 13 XML y 91 pruebas, todas con `skipped=0 failures=0 errors=0`. RutasCores 11, ValidacionErrores 7, ErrorApi 13, RegistrarCore 15, InterfazCatalogo 3, Fronteras 3, y el resto como antes. |
| 4 | Diff acotado | `git diff --stat a6b7bfe...HEAD` | pasa con desviaciones aceptadas: 15 archivos, 711 líneas añadidas y 8 quitadas. `CodigosError` y `ConfiguracionJson` caen fuera de la lista, pero los exige la orden del orquestador de que un tipo equivocado dé 422. Que `ConfiguracionJson` es necesaria lo prueba N01: sin ella, la suite cae. |

## Ronda 1, verificada
- **F-01 (`@NotNull` recortaba el 422): cerrado.** En el JAR, `{"nombre":"Mal Nombre","version":"x"}` da 422 con los tres detalles, y `{}` también da los tres. M14 (volver a poner `@NotNull` en `imagen` con `@Valid`) cae.
- **F-02 (la prueba de C-07 pasaba sin C-07): cerrado.** La prueba fija el motivo exacto (`MotivosCatalogo.IMAGEN_INVALIDA`), y C-07 se prueba ahora en `ValidacionErroresTest`.
- **F-03 (HMVE sin prueba): cerrado.** M11b, M12 y M24 ahora caen.
- **F-04 (un tipo equivocado no daba 422): cerrado** para un campo suelto.
  - En el JAR, un número, un bool, un decimal, un array o un objeto en un campo dan 422 con el campo real.
  - El texto `"123"` sigue registrándose con 201, y el JSON malformado sigue dando 400.
  - Lo que pasa al combinarlos va en F-10.
- **F-05 (literales sin fijar): cerrado.** M09 y M10 caen.
- **F-06 (la imagen de cada versión sin prueba): cerrado.** M23 y M26 caen.
- **F-07 (`CoreRegistrado`): cerrado a medias.**
  - Los imports quedaron bien.
  - El invariante entre versiones e imágenes no tiene prueba: N10 y N11 sobreviven, ver F-12.
  - Que `imagenes` sea inmutable tampoco tiene prueba: M17 sigue sobreviviendo. Es menor.
- **F-09 (la cifra de CA-4): cerrado.** La bitácora explica la diferencia de líneas.

## 1. El efecto global de `ConfiguracionJson`
Lo sondeé en el JAR y con `SondaJsonTest` en una copia (`sonda-json.log`).
- **No encontré regresiones en producción.**
  - La actuator responde bien: `/actuator/health` da 200 con `UP` y un POST da 405 `ErrorApi`.
  - El único cuerpo JSON de producción es el de `/api/cores`.
  - `verificador/` no usa Jackson (no aparece `ObjectMapper`), así que los descriptores no cambian.
  - Solo afecta a destinos de texto: `String` y `char[]`. `StringBuilder` y `Object` no cambian.
- **Casos legítimos rechazados: ninguno en producción.**
  - El fixture `/prueba/json` (`Map<String,String>`) pasó de aceptar `{"a":1}` a dar 422 `campo=a`. Ninguna prueba lo usa así.
  - En `SynKra` no hay ningún campo de texto donde un número sea legítimo: un nombre numérico entre comillas, `"123"`, sigue pasando.
- **La política se aplica solo en un sentido**, ver F-11. Un campo `Integer` acepta `"3"`, y `3.7` lo trunca a 3 sin avisar.

## 2. C-14
Los fixtures nuevos de `ControladorDePrueba` no reabren C-14: sigue abierta, nunca se cerró.
- Comprobé que no salen en el JAR: `unzip -l` da 0 entradas y `/prueba/valido` da 404 en el JAR.
- `ValidacionErroresTest` los carga con `@WebMvcTest(ControladorDePrueba.class)`, que es el patrón de cargarlos solo en las pruebas que los usan.
- Siguen entrando en todos los `@SpringBootTest` (por ejemplo, en `RutasCoresTest`), igual que los anteriores.
- El disparador de C-14 («cuando U04 traiga sus propios controladores») ya se cumplió con `RutasCores`. Ahora le toca al orquestador ponerle fecha. No es un defecto de esta tarea.

## 3. Mutaciones (30 mutantes, `./gradlew :backend:test --rerun`, 91 pruebas en cada corrida; `r2/mutaciones.txt`)
- **Caen:**
  - M09, M10, M11b, M12, M24, M14, M23 y M26.
  - N01, N02 y N03 (sin el bean, o sin la regla para enteros o para bool).
  - N06 (solo `InvalidFormatException`), N09 (motivo de tipo cambiado), N13 (campo del parámetro siempre el de respaldo), N15 (un JSON malformado da 422) y N16 (solo rutas anidadas).
- **Sobreviven:**
  - M13: poner `@Valid` sin restricciones es equivalente.
  - M21 y M22: volver a poner `@NotNull` en `nombre` o en `version`. Va en F-12.
  - M17: `imagenes` mutable.
  - N04: sin la regla para decimales.
  - N05: un `MismatchedInputException` sin ruta también da 422, y entonces sale con `campo=""`.
  - N07, N08 y N17: el índice, el separador y la ruta completa de un campo anidado.
  - N10 y N11: sin el invariante de `CoreRegistrado`.
  - N12: sin el texto de respaldo.
  - N14: la rama `ParameterErrors` vacía.

## Hallazgos
### F-10 · NARANJA · `ManejadorErrores.java:82-92` + `ConfiguracionJson.java` · un tipo equivocado vuelve a recortar el 422 (R-C1, «todos»)
Probado en el JAR (`r2/sonda.log`):
```
{"nombre":"Mal Nombre","version":"x","imagen":5}  -> 422, 1 detalle (imagen)
{"nombre":1,"version":2,"imagen":3}                -> 422, 1 detalle (nombre)
```
- En la ronda 1, `imagen:5` se convertía en texto y salían los tres detalles. Ahora Jackson se para en el primer error de tipo.
- Es la misma clase que F-01, y el orquestador ya decidió que R-C1 gana.
- Si el orquestador acepta que «el campo de Jackson» significa parar en el primer error, que lo deje escrito y esto baja a AMARILLO. Hasta entonces, bloquea.

### F-11 · AMARILLO · `ConfiguracionJson.java:17-20` · la política solo se aplica hacia el texto
- Un número va a dar error si el destino es texto, pero el texto sigue convirtiéndose a número. `"cantidad":"3"` da 200, y `3.7` se trunca a 3, también con 200.
- Hoy ningún campo de producción es numérico. Hay que decidirlo antes del primero.
- N04 (decimal) sobrevive: hoy el servicio lo rechaza igual por otro motivo.

### F-12 · AMARILLO · `RutasCoresTest`, `CoreRegistrado.java:16-18`, `ManejadorErrores.java:71,84-87,95` · código nuevo sin prueba
- M21 y M22 sobreviven: ninguna prueba HTTP manda `{}` ni deja fuera `nombre` o `version`. Bastaría un caso para atar F-01 en los tres campos.
- N10 y N11 sobreviven: el invariante de `CoreRegistrado` no tiene prueba.
- N05, N07, N08 y N17 sobreviven: un error de tipo en la raíz, que si se rompe el código saldría con `campo=""`, y las rutas anidadas (índice, separador, ruta completa) no tienen prueba. Las rutas anidadas son código especulativo, porque el DTO es plano.
- N12 (texto de respaldo) y N14 (rama `ParameterErrors`) sobreviven.

### F-13 · AMARILLO · `ManejadorErrores.java:85-87` · el `campo` devuelve la clave tal como llega
- En un cuerpo `Map`, `{"<script>":1}` vuelve con `campo:"<script>"`.
- Una clave de 100.000 caracteres la corta el límite de nombres de Jackson y da 400.
- Hoy solo afecta al fixture.

### F-14 · AMARILLO · `backend/build.gradle.kts` · `starter-validation` sin uso en `main`
- Ya no hay `jakarta.validation` en `backend/src/main`: solo lo usan los fixtures, y el JAR lo lleva igual.
- El codificador lo defiende para los DTO futuros, y es defendible. El orquestador decide si pasa a `testImplementation`.

## Tareas candidatas (defectos reales fuera de alcance)
- C-14 ya tiene su disparador cumplido y hay que ponerle fecha.
- Las que ya estaban en pie:
  - el tope del cuerpo JSON y el texto de C-34, que en `main` sigue apoyándose en U04-T03;
  - los 400 en HTML de Tomcat para `%2F` y `%00`;
  - el 406 mapeado a `SOLICITUD_INVALIDA`;
  - F-08: campos de más y claves repetidas aceptados sin aviso.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t03/r2/`:
- `ca1.log`, `ca2.log` y `ca3.log`
- `sonda.sh` y `sonda.log`
- `sonda-json.log`: la sonda está en `sonda/backend/src/test/java/io/synkra/sonda/SondaJsonTest.java`.
- `mutaciones.txt`, `mut-*.log`, `scripts/mutantes.py` y `scripts/correr.py`. Las copias están en `mut/`.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:82-92|un tipo equivocado recorta el 422 a un solo detalle; R-C1 «todos» roto otra vez
AMARILLO|backend/src/main/java/io/synkra/plataforma/ConfiguracionJson.java:17-20|política en un solo sentido: "3" y 3.7 se aceptan en un Integer; N04 sobrevive
AMARILLO|backend/src/test/java/io/synkra/catalogo/RutasCoresTest.java|código nuevo sin prueba: M21, M22, N05, N07, N08, N10, N11, N12, N14, N17 sobreviven
AMARILLO|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:85-87|campo devuelve la clave tal como llega en los cuerpos Map
AMARILLO|backend/build.gradle.kts|spring-boot-starter-validation sin uso en main; decidir si pasa a testImplementation
INFORME: revisiones/U04-T03/ronda-2.md
```
