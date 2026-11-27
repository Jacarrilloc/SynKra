# Ronda 5 — U01-T12

VEREDICTO: NO-VERDE

F-01 y F-02 de la ronda 4 quedan cerrados. Ejecuté yo mismo los 8 CA y todos pasan. Queda un NARANJA (F-01), sobre una puerta nueva que no es de U01-T11: con la varianza medida por lote, su probabilidad de fallo pasa el límite del orquestador, P(fallo) ≤ 10⁻⁶. Debí verlo en la ronda 4, donde di la razón media/umbral de esa puerta pero no calculé su P con la varianza medida. Es un fallo mío de barrido y lo corrijo ahora.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311, el esperado.
- **Ramas:** HEAD 863adef. El merge-base con origin/main es b4259d8.
- **Edad de la evidencia:** el código cambió en 969856b. 863adef solo trae la respuesta y la bitácora. Los 178 tests de la bitácora coinciden con mi CA-7.
- **Mutaciones:** todas en copias `git archive HEAD`, en el scratchpad (`u01t12r5/{pristine,head,head2}`).
- **Worktree al terminar:** `git status --short` sale vacío. Solo quedan cachés ignoradas (`.gradle/`, `build/`, `verificador/build/`, `verificador/.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: `tests="17" skipped="0" failures="0" errors="0"`. Hay 10 testcase `"S1"`–`"S10"` y 7 de forma. XML fresco (04:12:28) |
| 2 | Resultado exacto del diseño | el mismo, más `git diff eb567bb..HEAD` | pasa. Los datos de S1–S12 no cambiaron. En esta ronda solo se añade un assert de grupo null |
| 3 | Mutaciones (a)–(f), con (d) = «sin alternativa de core» | `scripts/rep.sh head2 <m> 1` | pasa: (a) falla en 3 de forma; (b) en S7; (b2) en 10; (c) en S9; (d) en S4 y S5; (e) y (f) en S8 y S9. Sin mutar: 17/17. `git diff --stat b4259d8...HEAD -- verificador/src/main` sale vacío |
| 4 | testFixtures usable | `testFixturesJar --rerun` + zipfile | pasa. El JAR trae `SuiteCombinaciones`, `SuiteCombinaciones$Caso` y `CatalogoEnMemoria` |
| 5 | C-26 sin romper U01-T11 | borré `.jqwik-database` y corrí `--tests '*VerificadorPropiedades'` + grep | pasa: BUILD SUCCESSFUL (35 s), 6 × `tries = 1000`, 6 × `checks = 1000`, 9/9, 0 saltadas. Fila C-26 cerrada. Ver F-01 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE ...` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL (37 s). 12 XML suman 178 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat b4259d8...HEAD` | pasa: 13 archivos, todos de la lista |

**Mutaciones de U01-T11:**
- `dp` hace fallar `minimoYMaximo`.
- `g2` hace fallar `determinista`.
- `j` hace fallar `generadorCubreLosCodigos`, `alternativaAplicadaAprueba` y S10.

## Comprobación especial 1: F-01 de la ronda 4, cerrado
- **Claves:** ahora se cuentan por el origen y además por el estado real de la carpeta (`VerificadorPropiedades.java:555-573`).
- **Prueba nueva:** `catalogosDeTextoRegistranCoreBSegunSuOrigen` (línea 580) exige que el origen y la presencia de `core-b` en `cores` coincidan, y que aparezcan las dos variantes.
- **Mutaciones de registro:** corrí cada una 3 veces sobre `generadorCubreLosCodigos` y la prueba nueva.

| mutación | resultado (3 de 3) |
|---|---|
| none | pasa |
| regnone, regdrop, regvacio, regotra | fallan: «faltan: [otra carpeta: texto con core-b registrado]» y falla la prueba nueva |
| regall | falla: faltan «sin registrar» y «NO registrado», y falla la prueba nueva |
| **regcarpeta** (nueva: los descriptores ajenos pasan a `core-c` y se registra `core-b`) | falla en las dos pruebas |
| **regcarpetab** (nueva: van a `core-c` solo cuando se registra `core-b`) | falla en las dos pruebas |
| **regswap** (nueva: etiqueta de origen invertida) | falla en las dos pruebas; faltan ambas claves |
| **regdesc** (nueva: rango ajeno `>=99.0.0`) | falla `generadorCubreLosCodigos` |
| **regver** (nueva: `core-b` registrado con 0.1.0) | **pasa**; ver F-02 |
| **reg13 / reg31** (registrado en 1 de cada 4, o en 3 de cada 4) | **pasa**; ver F-02 |

**Medición: 3 corridas de 100 lotes de 1000, con `.jqwik-database` borrada antes de cada una.** Hice una tercera corrida para fijar la varianza.

| puerta | umbral | medias | var/media por corrida | P(fallo) bin. neg., varianza agregada |
|---|---|---|---|---|
| otra carpeta: texto con core-b registrado | 1 | 16,10 / 16,84 / 16,59 | 1,44 / 1,29 / 1,02 | 3,9e-7: cumple (16,5x) |
| otra carpeta: texto con core-b sin registrar | 1 | 18,05 / 17,09 / 17,35 | 0,75 / 1,01 / 1,10 | 2,5e-8: cumple |
| suite en synkra-core | 20 | 70,19 / 69,73 / 70,73 | 0,80 / 1,16 / 1,00 | 4,3e-13: cumple (3,5x) |
| **suite en synkra-core con VERSION_INCOMPATIBLE** | 8 | 29,98 / 30,03 / 30,48 | 1,10 / 1,10 / 1,38 | **2,2e-6: no cumple**. Con Poisson da 4,6e-7 |
| U01-T11 (15 / 6 / 10) | — | 53–54 / 25 / 39 | ~1 | exentas |

## Comprobación especial 2: F-02, cerrado
`nogrupo` (quitar `requireNonNull(grupo)`) ahora hace fallar `esperadoYEntradaIncoherentesSeRechazan`: 16/17.

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:630 · La puerta «suite en synkra-core con VERSION_INCOMPATIBLE ≥ 8» tiene P(fallo) > 10⁻⁶ con la varianza medida
- **Qué es:** es una puerta de U01-T12, no estaba en b4259d8, así que le aplica la decisión del orquestador. Media/umbral = 3,8x, y eso cumple.
- **La varianza:** var/media da 1,10, 1,10 y 1,38 en mis tres corridas, y 1,0–1,2 en la ronda 4. Con 300 lotes, el agregado de 1,20 está unas 2,5 desviaciones estándar por encima de 1, así que hay sobredispersión. Con ella, la binomial negativa da P(<8) = 2,2e-6 (1,1e-6 incluso con var/media 1,10).
- **Lo que dice la bitácora:** solo da Poisson (4,5e-7), y por eso no se ve.
- **Arreglo:** umbral 7. Da 4,3x, con P de 5,7e-7 a var/media 1,20 y 1,0e-7 con Poisson. Actualizar la tabla de la bitácora con la P de la binomial negativa.

### F-02 · AMARILLO · VerificadorPropiedades.java:580-591 · La prueba nueva fija la presencia de core-b, no su forma ni su proporción
- **`regver`** (`core-b` registrado con 0.1.0, fuera de `>=1.0.0`) pasa 3/3. No rompe lo que pide C-26, porque el verificador solo consulta las versiones de `s.core()`. Lo que se pierde son las selecciones sobre `core-b` que podrían aprobar.
- **`reg13` y `reg31`** pasan 3/3. Contradicen la decisión «1 de cada 2» y bajan la media de «registrado» a ~8, con P(0) ≈ 3e-4. Hoy no ocurre.
- **Arreglo barato:** en la prueba de ejemplo, exigir que el número de `TEXTO_REGISTRADO` en las 300 muestras esté entre 100 y 200 (P binomial ≈ 1e-8 fuera de ese intervalo). Opcionalmente, exigir que las versiones registradas de `core-b` caigan en el rango de sus descriptores.

### F-03 (ronda 4) · AMARILLO · sigue en pie, aceptado como inherente
Sigue sin poder detectarse `cat6`. La justificación de la respuesta se sostiene.

## Tareas candidatas (defectos reales fuera de alcance)
- La de la ronda 3 sigue en pie: el umbral 6 de «core con texto» de U01-T11. Con la varianza medida, la binomial negativa da 1,4–2,3e-6. Puede ir junto a C-25.

## Rutas de transcripciones largas
- **Corridas de los CA:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r5/logs/cas.txt (también ca1.log, ca4.log, ca5.log y ca7.log en la misma carpeta)
- **Mutaciones de la suite y de U01-T11:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r5/logs/suite.txt
- **Mutaciones de registro (13 × 3):** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r5/logs/reg.txt, con un log por corrida en logs/rep-*.log
- **Mediciones:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r5/logs/medicion-head-1.txt, medicion-head-2.txt y medicion-head-3.txt
- **Scripts:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r5/scripts/mut.py, rep.sh, med.sh y MedicionTmp.java

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:630|la puerta «suite en synkra-core con VERSION_INCOMPATIBLE ≥ 8» da P(fallo)=2,2e-6 > 1e-6 con la varianza medida (var/media 1,20 en 300 lotes); umbral 7 la deja en 5,7e-7
INFORME: revisiones/U01-T12/ronda-5.md
```
