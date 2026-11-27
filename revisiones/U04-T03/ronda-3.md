# Ronda 3 — U04-T03

VEREDICTO: VERDE

Corrí yo los cuatro criterios y los cuatro pasan. F-10 queda en AMARILLO por decisión del orquestador: comprobé que la decisión está escrita y probada. F-12 y F-14 quedan cerrados con evidencia propia. No queda ningún ROJO ni NARANJA; quedan tres AMARILLO que no bloquean. Fusiona el humano.

## Comprobaciones previas
- **Hash de la tarea:** `d7226e61e300ec8b80eac90eda189a1f7b5d1613`. Coincide.
- **HEAD y base:** HEAD es `7984cd5` y la base de fusión es `a6b7bfe`.
- **Commits desde la ronda 2:** `71c6c45` (el informe de la ronda 2) y `7984cd5` (el retrabajo).
- **Worktree:** `git status --short` salió vacío antes y después.
- **Copias:** las sondas y las mutaciones se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Rutas según el contrato | `./gradlew :backend:test --rerun --tests '*RutasCoresTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`. La primera prueba tarda 0,85 s por el arranque del contexto y las demás entre 9 y 49 ms, con Tomcat real. |
| 2 | Los `curl` de H-01 contra el JAR en el 18080 | el bloque de CA-2 al pie de la letra (`r1/ca2.sh`) | pasa: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409`, `[('synkra-core', '2.1.0'), ('synkra-core', '3.0.0')]` |
| 3 | Todo el backend y las fronteras | `./gradlew :backend:test --rerun :backend:check` + bucle sobre los XML | pasa: 14 XML y 100 pruebas, todas con `skipped=0 failures=0 errors=0`. RutasCores 15, ValidacionErrores 8, CoreRegistrado 4, ErrorApi 13, RegistrarCore 15, InterfazCatalogo 3, Fronteras 3, y el resto como antes. |
| 4 | Diff acotado | `git diff --stat a6b7bfe...HEAD` | pasa: 18 archivos, 952 líneas añadidas y 8 quitadas. Lo que queda fuera de la lista son desviaciones ya aceptadas en las rondas 1 y 2: `build.gradle.kts`, `CatalogoApi`, `CoreRegistrado`, `CatalogoServicio`, `CodigosError`, `ConfiguracionJson` y `ControladorDePrueba`. |

## Ronda 2, verificada
- **F-10 (un tipo equivocado recortaba el 422): baja a AMARILLO, aceptado.** La decisión está escrita en tres sitios:
  - en el Javadoc de `ManejadorErrores.handleHttpMessageNotReadable`;
  - en la bitácora;
  - en la prueba `unTipoEquivocadoNoSeCombinaConLasReglasDeR_C1`, que tumba a N09 y P07.

  El «todos» se exige con valores de texto o nulos. En el JAR dan tres detalles `{"nombre":"Mal Nombre","version":"x"}`, `{}` y los tres campos a `null`, y las pruebas `{}`, «sin imagen» y «tres inválidos» lo atan. El caso de F-10 da un detalle (`imagen`) en el JAR.
- **F-11 y F-13: siguen como candidatas.** N04 sobrevive, como se esperaba.
- **F-12 (código nuevo sin prueba): cerrado.**
  - **La raíz da 400, y es lo correcto.** Un error de tipo en la raíz no tiene campo que nombrar, se trata igual que un cuerpo ilegible y queda atado: N05 y P02 caen. En el JAR, `[]`, `[{"nombre":5}]` y `5` dan 400 `SOLICITUD_INVALIDA`.
  - **El código especulativo se quitó.** Ya no están las rutas anidadas, ni la rama `ParameterErrors`, ni `textoDe`, ni `MOTIVO_SIN_TEXTO`.
  - **No queda ningún caso sin manejar.** Lo sondeé en una copia con controladores de sonda (`sonda-r3.log`) y no salió ningún 500:
    - un campo anidado con tipo equivocado da 422 con `campo="hijo"`, el primer segmento;
    - un `Map` da 422 con `campo` igual a la clave;
    - un `Map<String,List<String>>` también da 422;
    - una lista en la raíz da 400;
    - un `@Valid List<Hijo>` da 422 con `campo` igual al nombre del parámetro (`l`).

    La imprecisión de estos dos últimos casos va en F-15.
  - El invariante de `CoreRegistrado` y su inmutabilidad quedan atados: N10, N11 y M17 caen.
- **F-14 (`starter-validation` sin uso en `main`): cerrado.**
  - El JAR no lleva ninguna clase de `validator` ni de `jakarta.validation` (`unzip -l` da 0).
  - Arranca: `/actuator/health` responde `UP` y CA-2 pasa.
  - `MethodArgumentNotValidException` y `HandlerMethodValidationException` vienen de `spring-web`, así que `main` compila sin la dependencia.
  - Las dos traducciones siguen probadas en el classpath de prueba: M07b, M08, M09, M11b, M12, M16, M24 y N13 caen.
  - En producción esas dos traducciones quedan latentes, porque no hay validador en tiempo de ejecución. Va en F-16.

## Mutaciones (41 mutantes, `./gradlew :backend:test --rerun`, 100 pruebas en cada corrida; `r3/mutaciones.txt`)
- **Caen 32 mutantes:**
  - De la ronda 1 siguen cayendo todas las que aún aplican: M00 a M06, M07b, M08 a M12, M16 a M20 y M23 a M26.
  - De la ronda 2: N01, N02, N03, N05, N06, N09, N10, N11, N13 y N15.
  - Nuevas: P02 (sin la comprobación de ruta vacía) y P07 (un tipo equivocado da 400).
- **No compilan:** M13 y M14 (volver a poner `@Valid` y `@NotNull`), y P05 (sin la dependencia en test). Que no compilen es en sí la protección.
- **Sobreviven:**
  - N04: es F-11, que es candidata.
  - P01 (sin la comprobación del índice) y P03 (último segmento en vez del primero): son equivalentes mientras los cuerpos sean objetos planos.
  - P04 (`validation` de vuelta en `main`): ninguna prueba ata F-14.
  - P06 (sin el nombre de respaldo): es equivalente, porque se compila con `-parameters`.

## Hallazgos
### F-10 · AMARILLO · `ManejadorErrores.java:72-90` · un tipo equivocado informa solo el primer campo
Es la decisión del orquestador: queda escrita y probada. No se combina con R-C1.

### F-15 · AMARILLO · `ManejadorErrores.java:63-90` · `campo` impreciso fuera de los cuerpos planos
- Un tipo equivocado en un campo anidado da `campo="hijo"`, mientras que `MethodArgumentNotValidException` da `hijo.nombre`: hay dos notaciones distintas.
- Un `@Valid` sobre una lista da `campo` igual al nombre del parámetro.
- P01 y P03 sobreviven.
- Hoy no se alcanza: el DTO es plano y no hay validador en `main`. Hay que decidirlo con el primer DTO anidado.

### F-16 · AMARILLO · `build.gradle.kts`, `CodigosError.java:24-28` · restos de C-07
- La traducción de C-07 está latente en producción, y ninguna prueba ata que `validation` quede solo en test: P04 sobrevive.
- `MOTIVO_OBLIGATORIO` es una constante de `main` que solo usa el fixture.
- El Javadoc de esas constantes quedó cortado: «se usan.».

## Tareas candidatas (defectos reales fuera de alcance)
- **Ya registradas en rondas anteriores:**
  - F-11: la política de coerción solo se aplica hacia el texto.
  - F-13: el `campo` devuelve la clave tal como llega en los cuerpos `Map`.
  - F-08: campos de más y claves repetidas se aceptan sin aviso.
- **Nueva:** la notación del `campo` en cuerpos anidados y en listas (F-15).
- **Siguen en pie:**
  - C-14, con su disparador ya cumplido;
  - el tope del cuerpo JSON y el texto de C-34;
  - los 400 en HTML de Tomcat;
  - el 406 mapeado a `SOLICITUD_INVALIDA`.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04t03/r3/`:
- `ca1.log`, `ca2.log` y `ca3.log`
- `sonda.sh` y `sonda.log`
- `sonda-r3.log`: la sonda está en `sonda/backend/src/test/java/io/synkra/sonda/`.
- `mutaciones.txt`, `mut-*.log`, `scripts/mutantes.py` y `scripts/correr.py`. Las copias están en `mut/`.

```
VEREDICTO: VERDE
AMARILLO|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:72-90|un tipo equivocado informa solo el primer campo (decisión del orquestador, escrita y probada)
AMARILLO|backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java:63-90|campo impreciso en cuerpos anidados y listas (P01 y P03 sobreviven); hoy no se alcanza
AMARILLO|backend/build.gradle.kts|C-07 latente en producción; validation solo en test sin atar (P04); MOTIVO_OBLIGATORIO solo lo usa el fixture; Javadoc cortado
INFORME: revisiones/U04-T03/ronda-3.md
```
