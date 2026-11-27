# Ronda 3 — U01-T12

VEREDICTO: NO-VERDE

La tarea queda sin aprobar por un solo hallazgo NARANJA, F-01. Los 8 CA pasan con mis propias corridas.

Comprobaciones previas:
- Hash de la tarea: `git hash-object` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311, el esperado.
- Worktree en HEAD 509051e; origin/main es b4259d8.
- El código cambió en 31e367d. 509051e solo añade la bitácora y la respuesta (`git diff --stat 31e367d..509051e`). La evidencia de la bitácora pudo salir de este diff.
- Al terminar, `git status --short` sale vacío. Solo quedan cachés ignoradas (`.gradle/`, `build/`, `verificador/build/`, `verificador/.jqwik-database`).
- Todas las mutaciones se hicieron en copias `git archive 509051e`, en el scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: `tests="16" skipped="0" failures="0" errors="0"`. Hay 10 testcase, de `"S1"` a `"S10"`, y 6 de forma (5 de antes más `esperadoYEntradaIncoherentesSeRechazan`) |
| 2 | Resultado exacto del diseño | el mismo, más el diff dd5f138..HEAD | pasa. Los datos de S1 a S12 no cambiaron; solo cambió de dónde sale `catalogo()` |
| 3 | Mutaciones (a) a (f), con (d) = «sin alternativa de core» | `scripts/run.sh <m>` sobre una copia | pasa. Sin mutar: 16/16. (a): fallan 3 de forma. (b): falla S7 (`PLUGIN_AUSENTE` en vez de `CORE_EQUIVOCADO`). (b2): fallan S7 y «mismo contenido». (c): falla S9. (d): fallan S4 (le falta `CORE synkra-core 3.0.0`) y S5 (`[]`). (e): fallan S8 y S9. (f) ARCHIVO: fallan S8 y S9 por `leer(jar)`, con `DESCRIPTOR_SIN_ARCHIVO`. `git diff --stat b4259d8...HEAD -- verificador/src/main` sale vacío |
| 4 | testFixtures usable | `testFixturesJar --rerun` + zipfile | pasa. El JAR trae `SuiteCombinaciones`, `$Caso`, `$CoreDelCatalogo` y `CatalogoEnMemoria`, entre otras |
| 5 | C-26 sin romper U01-T11 | borré `.jqwik-database` y corrí `--tests '*VerificadorPropiedades'` + grep | pasa: BUILD SUCCESSFUL (30 s), 6 × `tries = 1000`, 6 × `checks = 1000`, 8/8, 0 saltadas. Las medias pasan de 3x (tabla abajo). La fila C-26 está actualizada. Ver F-01 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE ...` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL (32 s). Suma de los XML: 176 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 9 archivos, todos de la lista (CANDIDATAS, bitácora, 4 de `revisiones/U01-T12/` y los 3 .java previstos) |

**Medición, 2 corridas de 100 lotes de 1000, borrando `.jqwik-database` antes de cada una.** Añadí la varianza por lote.

| contador | umbral | media 1 / 2 | mín 1 / 2 | var/media | media/umbral |
|---|---|---|---|---|---|
| plugin con texto distinto | 15 | 52,60 / 53,93 | 34 / 39 | 1,0 | 3,6x |
| core con texto distinto | 6 | 26,05 / 26,24 | 14 / 16 | 1,1 | 4,4x |
| solo en otra carpeta | 10 | 39,26 / 39,55 | 26 / 21 | 1,1 | 3,9x |
| ... registrado | 3 | 22,14 / 21,77 | 10 / 9 | 1,0 | 7,3x |
| ... NO registrado | 2 | 17,12 / 17,78 | 6 / 8 | **1,32** | 8,7x |
| suite en synkra-core | 20 | 70,48 / 71,08 | 50 / 53 | 0,9 | 3,5x |
| suite en synkra-core con VERSION_INCOMPATIBLE | 8 | 29,96 / 31,03 | 20 / 13 | 1,0 | 3,8x |

- **Tabla Poisson del codificador:** la recalculé y cuadra (1,8e-7, 3,6e-7, 4,3e-7…). Sus umbrales 3 y 2 son los máximos que cumplen ≤ 10⁻⁶ bajo Poisson: con 4 y 3 la probabilidad sale 1,3e-6 y 3,4e-6.
- **synkra-core en el catálogo de la suite y su región VERSION_INCOMPATIBLE:** siguen en ~71 y ~30 por cada 1000, sin cambio frente a la ronda 2.

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:605 · La puerta de «registrado» no detecta que se pierda el registro de `core-b`, que es la regresión de C-26
- **Mutación `regnone`** (`Arbitraries.of(false)`, es decir, `core-b` nunca registrado): `generadorCubreLosCodigos` **pasa** (1/1).
- **Por qué pasa:** lo medí con 100 lotes bajo esa mutación. «registrado» sigue en **4,47 de media** (mín 1, máx 9), alimentado por los otros generadores de catálogos. Es el mismo 4,8 de antes de C-26 que cita la fila de CANDIDATAS. Con umbral 3, la mutación pasa en el 82 % de las corridas (P(Poisson(4,47) ≥ 3)).
- **Ningún umbral sobre este contador sirve:** el máximo estable es 4 (5,9e-7 con mi media de 21,95), y aun con 4 la mutación pasa el 65 % de las veces.
- **La mutación contraria sí se detecta:** `regall` (`core-b` siempre registrado) falla de forma determinista con «faltan: [… NO registrado]».
- **Los pesos intermedios no se detectan:** 3:1 y 1:3 pasan, 3 de 3 corridas cada uno. Es inherente a una puerta estable con medias de ~20; no lo exijo.
- **Arreglo que cumple a la vez potencia y estabilidad:** contar la variante por su origen, con el registro de `core-b` en `catalogosDeTexto` y no cualquier core registrado. Por ejemplo, una clave «… registrado (core-b)», o una prueba de ejemplo que muestree `catalogosDeTexto()` y exija las dos variantes. Así el umbral protege lo que C-26 cerró.

### F-02 · AMARILLO · VerificadorPropiedades.java:607 · «NO registrado» sale sobredispersa y su P(fallo) real podría pasar de 10⁻⁶
- **Lo medido:** en mis dos corridas, la varianza por lote es 23,16 y 22,90 para medias de 17,12 y 17,78 (var/media ≈ 1,32). El resto de contadores sale en ~1,0.
- **Lo que no lo explica:** el histograma por caso (100 000 casos) da {0: 98 331, 1: 1 629, 2: 40}, así que no viene de que un mismo caso cuente dos veces.
- **Probabilidad con ese dato:** con una binomial negativa ajustada a mi media y varianza, P(< 2) ≈ 3,8e-6, por encima del criterio. Con umbral 1, que es lo que ya exige `containsAll`, queda en 2,7e-7.
- **Grado de certeza:** es una estimación con 200 lotes, no una prueba, y por eso no bloquea. Bajar a ≥ 1 cuesta poco.

### F-03 · AMARILLO · SuiteCombinaciones.java y SuiteCombinacionesTest.java:203 · Huecos menores del contrato público
- **Validación sin prueba:** quitar «RECHAZADA lleva al menos un motivo» (`val3`) deja la suite en 16/16. La respuesta la cuenta como validación, pero `esperadoYEntradaIncoherentesSeRechazan` no la ejercita. Las otras cuatro validaciones (`val0`, `val1`, `val2`, `val4`) sí se detectan, cada una por esa prueba.
- **Lo que aún se construye, según `contra/src/otro/Contra.java`:**
  - `CoreDelCatalogo("synkra-core", …)` con un descriptor de `otro-core`;
  - `CoreDelCatalogo` con core null;
  - `Caso` con id null;
  - `Caso` con `Grupo.INTEGRIDAD` y `PorSeleccion`;
  - LECTOR con un código del verificador.

  La mutación `cat7` (`KYC_XX` en la carpeta de `synkra-core`) solo la detecta `catalogoDeLaSuiteCubreTodoElCatalogo`, que está en src/test y que U02 y U07 no heredan.
- **Dirección sin comprobar:** `cat6` (`catalogo()` con una entrada que no está en `contenido()`) pasa todo. Solo se comprueba contenido ⊆ catálogo, no la inversa. Hoy no ocurre porque `catalogo()` se deriva de `contenido()`.

### F-04 · AMARILLO · VerificadorPropiedades.java:606 · Comentario desplazado
El comentario «El catálogo de la suite en synkra-core … la base daba ~29 y ~12,8» quedó encima de la aserción de «NO registrado» y no de las dos que describe.

### Cerrados de la ronda 2
- **F-01 (opción i):** hecha, 1:1 medido. Sigue el problema de potencia de la puerta, ahora como F-01.
- **F-02:** las 8 combinaciones de la ronda 2 se rechazan ahora, y también otras 4 nuevas (RECHAZADA sin motivos, APROBADA con alternativas, PorSeleccion con LECTOR…).
- **F-03:**
  - `contenido()` es inmutable: `add`, `plugins().clear()` y `versiones().add` lanzan `UnsupportedOperationException`. Hace copia defensiva, y es un objeto nuevo e igual en cada llamada.
  - `catalogo()` y `catalogoDeLaSuite()` salen de `contenido()`.
  - `cat5`: con una sonda propia, `kyc-ar` aparece en `Datos` y en `catalogo()`; ya no se pierde.
  - `nodet`: 3 de 3 rojas.
- **F-04 y F-05:** corregidos.

**Mutaciones de U01-T11 sobre HEAD:** `dp` hace fallar `minimoYMaximo`, `g2` hace fallar `determinista`, y `j` hace fallar `alternativaAplicadaAprueba` y `generadorCubreLosCodigos`.

**Observación fuera de este diff:** el umbral 6 de «core con texto», de U01-T11, da 5,5e-7 bajo Poisson y 1,3e-6 con la varianza medida. Es el límite y no es de esta tarea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La observación sobre el umbral de U01-T11 puede ir junto a C-25 cuando se cierre U01.

## Rutas de transcripciones largas
Todas en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/:
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/logs/ca1.log, ca4.log, ca5.log, ca7.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/logs/medicion-head-1.txt, medicion-head-2.txt, medicion-regnone.txt, casos-head.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/logs/mut-*.log y xml-*/ (none, a, b, b2, c, d, e, f, val0..val4, cat5, cat6-cat, cat7-cat, nodet-cat{,2,3}, regall-1, regnone-2, reg31-{3,4,5}, reg13-{6,7,8}, dp, g2, j)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/scripts/{mut.py,run.sh,run1.sh,med.sh,MedicionTmp.java,MedicionCasos.java}
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r3/contra/src/otro/Contra.java

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:605|la puerta «registrado ≥ 3» no detecta core-b nunca registrado (sigue en 4,47/1000 por otros generadores; la mutación pasa el 82 % de las corridas)
INFORME: revisiones/U01-T12/ronda-3.md
```
