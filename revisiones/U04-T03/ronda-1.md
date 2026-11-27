# Ronda 1 — U04-T03

VEREDICTO: NO-VERDE

Los cuatro criterios pasan en mis propias corridas. El veredicto es NO-VERDE por seis NARANJA. Ninguno rompe un criterio tal como está escrito. Todos son de la misma familia: hay partes de C-07 que no se pueden distinguir con las pruebas, o que se comportan peor que el servicio que tienen detrás.

## Comprobaciones previas
- **Hash de la tarea:** `git hash-object` da `d7226e61e300ec8b80eac90eda189a1f7b5d1613`. Coincide.
- **HEAD y base:** HEAD es `ff6edb6` y la base de fusión es `a6b7bfe`.
- **Worktree:** `git status --short` salió vacío antes y después.
- **Fetch:** hice un `git fetch`. Solo movió la referencia `origin/main` a `4092fa0` y no tocó ningún fuente. Revisé el diff contra `a6b7bfe`, que sale igual que `origin/main...HEAD` antes del fetch.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Rutas según el contrato | `./gradlew :backend:test --rerun --tests '*RutasCoresTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="8" skipped="0" failures="0" errors="0"`. La primera prueba tarda 0,79 s por el arranque del contexto y las demás entre 11 y 44 ms, con Tomcat real. |
| 2 | Los `curl` de H-01 contra el JAR en el 18080 | el bloque de CA-2 al pie de la letra (`ca2.sh`) | pasa: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409`, `[('synkra-core', '2.1.0'), ('synkra-core', '3.0.0')]` |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 12 XML y 81 pruebas, todas con `skipped=0 failures=0 errors=0`. Fronteras 3, InterfazCatalogo 3, RutasCores 8, RegistrarCore 15, ErrorApi 13, y el resto como en `main`. |
| 4 | Diff acotado | `git diff --stat a6b7bfe...HEAD` | pasa con las desviaciones aceptadas: 9 archivos, 344 líneas añadidas y 8 quitadas. |

## Contrato, sondeado contra el JAR (`sonda.log`, `sonda2.log`)
- **Lo que está bien:**
  - `Location: /api/cores/{nombre}` es relativo, como pide el contrato.
  - El cuerpo tiene exactamente la forma `{"nombre","versiones":[{"version","imagen"}]}`.
  - Las versiones se ordenan como `Version`: 2.1.0, 2.10.0, 10.0.0.
  - La lista sale ordenada por nombre: a-core, beta, synkra-core.
  - Los códigos 409, 422 (con tres detalles), 404 y 400 (JSON malformado o cuerpo vacío) traen cuerpo `ErrorApi` y `Content-Type: application/json`.
  - Sin `Content-Type`, o con `text/plain`, responde 415 `TIPO_NO_SOPORTADO`.
  - DELETE y PUT dan 405 con la cabecera `Allow`.
  - Con barra final dan 404 `ErrorApi` `/api/cores/`, `…/synkra-core/` y `…/plugins/`.
  - `%20`, `ñ` y las mayúsculas dan 404 `ErrorApi`.
- **Lo que es anterior a este diff:**
  - `%2F`, `%00` y `..%2F` los corta Tomcat con un 400 en HTML, no en `ErrorApi`. Va como candidata.
  - `;x=1` se ignora y la ruta devuelve 200.

## Juicio de las cinco desviaciones
1. `listarCores()`: aceptada. La autorizó el orquestador y está bien atada: M05 y M25 caen.
2. `Map<Version,String> imagenes`: la acepto, porque el contrato necesita la imagen de cada versión y no cambia ninguna regla de U04-T02. Tiene dos defectos menores (F-07): la lista y el mapa son dos fuentes que pueden no coincidir, y nada lo comprueba.
3. `spring-boot-starter-validation`: aceptada. Es necesaria: M15 no compila sin ella.
4. Controlador y DTO package-private: aceptada y atada. M19 (controlador público) y M20 (DTO anidado público) los tumban `ningunTipoPublicoFueraDeApi` y `laInterfazSinNombreNoExponeTipos`.
5. `HandlerMethodValidationException` sin prueba: **rechazada**, ver F-03.

## C-34 (un POST de 3 MB en `version` vuelve como un 422 de 3 MB)
Lo reproduje: 3.000.040 bytes de entrada dan un 422 de 3.000.332 bytes. Además, un cuerpo de 30 MB con un campo de relleno de más se lee entero y vuelve como un 422.

**No es un defecto de esta tarea.** El `Dentro` y los CA del archivo fijado por el hash no piden ningún tope de cuerpo. Ponerlo tocaría la configuración de plataforma, que no está en la lista de CA-4, y eso sería desborde.

Hay que corregir el texto de C-34 en `main`. Dice «El tope de cuerpo de las rutas (U04-T03) lo acota por ahora», y esa frase sale de mi F-09 de U04-T02. Con este diff es falsa. El orquestador tiene dos opciones: añadir el tope como criterio de U04-T03, o reescribir C-34 y crear una tarea para el tope del cuerpo JSON. Sigue siendo candidata.

## Mutaciones (29 mutantes, `./gradlew :backend:test --rerun`, 81 pruebas en cada corrida)
- **Caen:**
  - M00: sin el controlador caen las 8 pruebas de `RutasCoresTest`. Esto confirma que el rojo primero era posible.
  - M01 (`Location`), M02 (200 sin `Location`), M03 (imagen nula), M04 (orden de versiones como texto, lo tumba `RegistrarCoreTest`), M05 (lista sin ordenar), M06 (plugins sin 404) y M07b (sin traducir MANVE).
  - M08 (campo falso), M16 (MANVE con 400), M18 (clave `versiones` renombrada), M19, M20 y M25.
- **Sobreviven:**
  - M09 (motivo de MANVE falso) y M10 (texto de `OBLIGATORIO` cambiado).
  - M11b (sin el manejador de HMVE), M12 (HMVE devuelve 400 sin detalles) y M24 (HMVE con campo y motivo falsos).
  - **M13 (sin `@Valid`)**, M14, M21 y M22 (sin `@NotNull` en imagen, nombre y version).
  - M17 (sin `Map.copyOf`), M23 (imagen fija en lugar de la guardada) y M26 (la imagen de la primera versión para todas).
- **No compilan:** M07, M11 y M15. M07 y M11 se rehicieron como M07b y M11b, que sí compilan.

## Hallazgos
### F-01 · NARANJA · `RutasCores.java:24-27` · `@NotNull` recorta el 422 y rompe el «todos» de R-C1
- Envié `{"nombre":"Mal Nombre","version":"x"}` y recibí un 422 con **un solo** detalle (`imagen`). El nombre y la versión inválidos no aparecen.
- La regla 1 de U04-T02 dice que se devuelven **todos** los detalles en un solo 422. El servicio ya trata los `null` de los tres campos, así que sin `@NotNull` saldrían los tres.
- La bitácora dice «Ninguna regla de U04-T02 chocó con la ruta». Sí chocó, y la tarea pedía registrarlo y parar.

### F-02 · NARANJA · `RutasCoresTest.java:114-123` · la prueba de C-07 pasa sin C-07
- M13 (sin `@Valid`), M14, M21 y M22 sobreviven. El servicio responde lo mismo, con `campo=imagen`, y la prueba solo exige `motivo` no vacío, así que M09 y M10 también sobreviven.
- El punto de CA-1 «sin imagen → 422 `campo=imagen` (C-07)» se cumple aunque se quite el código de C-07. Lo único distinto que se ve por HTTP es el motivo, y ninguna prueba lo fija.

### F-03 · NARANJA · `ManejadorErrores.java:60-75` · HMVE traducida sin prueba y C-07 cerrada igual
- M11b, M12 y M24 sobreviven. Ese código no lo ejercita nada.
- El argumento de que ninguna ruta valida parámetros no se sostiene: `plataforma/prueba/ControladorDePrueba.java` es el sitio previsto para provocar cada error, y las pruebas nuevas están en la lista de CA-4.
- `CANDIDATAS.md:14` marca C-07 como «Cerrada» con la mitad sin probar.

### F-04 · NARANJA · `RutasCores.java:24` + `ManejadorErrores` · un tipo equivocado no da 422 con su `campo`
- `"imagen":["a"]` y `"imagen":{"a":1}` dan 400 `SOLICITUD_INVALIDA` con `detalles:[]`.
- `"nombre":123` se convierte en texto y se registra un core `"123"` con **201**.
- `"imagen":true` y `"version":1` se convierten en texto y caen en el 422 solo por suerte.
- Ninguno de estos casos tiene prueba.

### F-05 · NARANJA · `RutasCores.java:22`, `ManejadorErrores.java:71` · literales visibles para el usuario fuera de `MotivosCatalogo` y sin fijar
- `"El campo es obligatorio"`: M10 sobrevive.
- El campo de respaldo `"parametro"`: M24 sobrevive.
- U04-T02 (F-04) dejó como precedente que los motivos viven en `MotivosCatalogo` y que una prueba fija su texto.

### F-06 · NARANJA · `RutasCoresTest.java:28` · la imagen de cada versión no se prueba
- Todas las pruebas usan la misma `IMAGEN`.
- M23 (una imagen fija en lugar de la guardada) y M26 (la imagen de la primera versión repetida en todas) sobreviven. El campo `imagen` del contrato no tiene prueba que lo distinga.

### F-07 · AMARILLO · `CoreRegistrado.java:11-15`, `CatalogoServicio.java:97`, `CatalogoApi.java:13`
- `versiones` e `imagenes` pueden no coincidir, y el constructor no lo comprueba.
- Que `imagenes` sea inmutable no lo comprueba ninguna prueba: M17 sobrevive.
- Se escriben con el paquete completo `java.util.HashMap` y `java.util.List`, en lugar de importarlos.

### F-08 · AMARILLO · `RutasCores.java:24` · los campos de más y las claves repetidas se aceptan sin aviso
- `"admin":true` da 201.
- Con `"nombre":"dup","nombre":"otro"` se registra `otro`.

### F-09 · AMARILLO · `bitacoras/U04-T03.md` · la cifra de CA-4 no coincide
La bitácora da 328 líneas y mi corrida da 344. La diferencia son las 16 líneas que añade la propia bitácora, pero no lo dice.

## Tareas candidatas (defectos reales fuera de alcance)
- **Tope del cuerpo JSON:** 30 MB se leen enteros. C-34 en `main` supone que U04-T03 lo acota, y no lo acota.
- **Errores de Tomcat:** `%2F`, `%00` y `..%2F` devuelven un 400 en HTML de Tomcat, no en `ErrorApi`. Es de plataforma y anterior a este diff.
- **406:** se mapea a `SOLICITUD_INVALIDA`, que no tiene un código propio.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t03/r1/`:
- `ca1.log`, `ca2.log`, `ca2.sh` y `ca3.log`
- `sonda.sh`, `sonda.log`, `sonda2.sh` y `sonda2.log`
- `mutaciones.txt`, `mut-M*.log`, `scripts/mutantes.py` y `scripts/correr.py`. Las copias están en `mut/`.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/catalogo/RutasCores.java:24-27|@NotNull recorta el 422: con un campo null se pierden los demás detalles (rompe el «todos» de R-C1)
NARANJA|backend/src/test/java/io/synkra/catalogo/RutasCoresTest.java:114-123|la prueba de C-07 pasa sin @Valid ni @NotNull (M13, M14, M21, M22, M09, M10 sobreviven)
NARANJA|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:60-75|HandlerMethodValidationException sin prueba (M11b, M12, M24) y C-07 marcada cerrada
NARANJA|backend/src/main/java/io/synkra/catalogo/RutasCores.java:24|tipo equivocado: un array o un objeto dan 400 sin campo; "nombre":123 registra el core "123" con 201
NARANJA|backend/src/main/java/io/synkra/catalogo/RutasCores.java:22|literales "El campo es obligatorio" y "parametro" fuera de MotivosCatalogo y sin fijar (M10, M24)
NARANJA|backend/src/test/java/io/synkra/catalogo/RutasCoresTest.java:28|la imagen de cada versión no se prueba (M23, M26 sobreviven)
AMARILLO|backend/src/main/java/io/synkra/catalogo/api/CoreRegistrado.java:11-15|versiones e imagenes pueden no coincidir; la inmutabilidad no tiene prueba (M17); java.util sin importar
AMARILLO|backend/src/main/java/io/synkra/catalogo/RutasCores.java:24|campos de más y claves repetidas aceptados sin aviso
AMARILLO|bitacoras/U04-T03.md|la cifra de CA-4 (328) no coincide con la mía (344) y no se explica
INFORME: revisiones/U04-T03/ronda-1.md
```
