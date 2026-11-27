# Ronda 4 — U01-T12

VEREDICTO: NO-VERDE

Los 8 CA pasan con mis propias corridas. Queda un hallazgo NARANJA, F-01: la regresión de C-26 sigue sin detectarse, ahora por otra vía. Aunque es la ronda final autorizada, no suavizo el veredicto; queda en manos del orquestador.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311, el esperado.
- **Ramas:** HEAD f42c341; merge-base y origin/main son b4259d8.
- **Edad de la evidencia:** el código cambió en 11d1db8 y f42c341 solo añade la bitácora y la respuesta. La evidencia de la bitácora pudo salir de este diff.
- **Mutaciones:** todas en copias `git archive HEAD`, en el scratchpad.
- **Worktree al terminar:** `git status --short` sale vacío. Solo quedan cachés ignoradas (`.gradle/`, `build/`, `verificador/build/`, `verificador/.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: `tests="17" skipped="0" failures="0" errors="0"`. Hay 10 testcase `"S1"`–`"S10"` y 7 de forma (las 6 de antes más `elCatalogoNoTieneMasQueElContenido`). XML fresco, de las 03:19:49 |
| 2 | Resultado exacto del diseño | el mismo, más `git diff 509051e..HEAD` | pasa. Los datos de S1–S12 no cambiaron: el diff de `SuiteCombinaciones` solo añade validaciones |
| 3 | Mutaciones (a)–(f), con (d) = «sin alternativa de core» | `scripts/run1.sh <m>` sobre una copia | pasa. Ver el detalle debajo de la tabla. `git diff --stat b4259d8...HEAD -- verificador/src/main` sale vacío |
| 4 | testFixtures usable | `testFixturesJar --rerun` + zipfile | pasa. El JAR trae `SuiteCombinaciones`, `$Caso` y `CatalogoEnMemoria`, entre otras |
| 5 | C-26 sin romper U01-T11 | borré `.jqwik-database` y corrí `--tests '*VerificadorPropiedades'` + grep | pasa: BUILD SUCCESSFUL (41 s), 6 × `tries = 1000`, 6 × `checks = 1000`, 8/8, 0 saltadas. Fila C-26 cerrada. Medición abajo. Ver F-01 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE ...` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL (47 s). 12 XML suman 177 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat b4259d8...HEAD` | pasa: 11 archivos, todos de la lista (CANDIDATAS, bitácora, 6 de `revisiones/U01-T12/` y los 3 .java) |

**Detalle de CA-3** (sin mutar: 17/17):

| mutación | qué falla |
|---|---|
| (a) | 3 pruebas de forma |
| (b) | S7 |
| (b2) | S1–S7, S10 y las 2 de catálogo, porque `CoreDelCatalogo` rechaza `KYC_XX` en `synkra-core` |
| (c) | S9 |
| (d) | S4 y S5 |
| (e) | S8 y S9 |
| (f) ARCHIVO | S8 y S9 |

**Mutaciones de U01-T11** (sobre `VerificadorPropiedades`):
- `dp` hace fallar `minimoYMaximo`.
- `g2` hace fallar `determinista`.
- `j` hace fallar `generadorCubreLosCodigos` y `alternativaAplicadaAprueba`, y además S10 en la suite.

**Medición: 2 corridas de 100 lotes de 1000, con `.jqwik-database` borrada antes de cada una.**

| contador | umbral | media 1 / 2 | mín 1 / 2 | var/media | P(fallo) Poisson / bin. neg. |
|---|---|---|---|---|---|
| otra carpeta: texto con core-b registrado | 1 | 15,80 / 16,42 | 7 / 4 | 1,12 / 1,00 | 1,4e-7 / 3,4e-7 |
| otra carpeta: texto con core-b sin registrar | 1 | 17,20 / 17,71 | 8 / 7 | 1,14 / 1,06 | 3,4e-8 / 9,9e-8 |
| compatible solo en otra carpeta ... registrado (solo presencia) | 1 | 20,71 / 21,54 | 8 / 6 | ~0,9 | < 1e-8 |
| ... NO registrado (solo presencia) | 1 | 17,20 / 17,71 | 8 / 7 | 1,14 / 1,06 | < 1e-7 |
| suite en synkra-core | 20 | 71,02 / 71,80 | 47 / 52 | 1,1–1,3 | 3,5x |
| suite en synkra-core con VERSION_INCOMPATIBLE | 8 | 30,78 / 30,62 | 20 / 18 | 1,0–1,2 | 3,8x |
| plugin / core con texto, solo en otra carpeta (U01-T11) | 15 / 6 / 10 | 52–53 / 25 / 38–39 | — | ~1 | sin cambios |

- **Las dos puertas nuevas** cumplen media ≥ 3x y P(fallo) ≤ 10⁻⁶ también con la varianza medida.
- **La sobredispersión 1,32 de la ronda 3** no se repite (1,06–1,14).

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:553 · La clave «otra carpeta: texto con core-b registrado» cuenta la etiqueta, no el registro: quitar el registro de core-b sigue pasando
- **Lo que pedí en la ronda 3, `regnone` y `regall`:** se cierra. Corrí 3 veces cada una y las 6 fallan de forma determinista. `regnone` falla con «faltan: [otra carpeta: texto con core-b registrado]». `regall` falla con «faltan: [... sin registrar, ... NO registrado]». Sin mutar pasa 3/3.
- **Por qué no basta:** la clave se cuenta con `!plugin && enOtraCarpeta && datos.origen().startsWith("texto")`, y `origen` sale del mismo booleano `core_b_registrado`. No dice nada de si `core-b` está de verdad en `cores`. Las mutaciones que rompen el registro sin tocar el booleano pasan, **3 de 3 cada una**:
  - `regdrop`: se quita la línea 197, `cores.put("core-b", ...)`;
  - `regvacio`: se registra `core-b` con `List.of()`;
  - `regotra`: se registra `core-c` en lugar de `core-b`.
- **Medición con `regdrop`** (100 lotes):
  - «texto con core-b registrado» sigue en **16,86**, sin cambio;
  - el genérico «... registrado» cae a **4,92** (mín 1), así que su puerta de presencia ≥ 1 tampoco salta;
  - «NO registrado» sube a 34,94.
- **Consecuencia:** es exactamente la regresión que C-26 cerró («`core-b` registrado en la mitad de los catálogos de texto»), y ninguna puerta la detecta. El comentario de la línea 550 («detectan que se pierda una de las dos variantes») no se cumple.
- **Arreglo:** contar cada clave por origen **y** por el estado real de la carpeta. «registrado» con `enOtraCarpetaConCoreRegistrado`, y «sin registrar» con `enOtraCarpetaSinCoreRegistrado`. Otra opción: una prueba de ejemplo que muestree `catalogosDeTexto()` y exija que `origen == TEXTO_REGISTRADO` ⇔ `cores.get("core-b")` no vacío, y que aparezcan las dos variantes. Las medias no cambian (~16 y ~17), así que la estabilidad se mantiene.

### F-02 · AMARILLO · SuiteCombinaciones.java:107 · `requireNonNull(grupo)` sin prueba
La mutación `nogrupo` (quitar esa línea) deja la suite en 17/17. `esperadoYEntradaIncoherentesSeRechazan` prueba `id` null pero no `grupo` null.

### F-03 · AMARILLO · SuiteCombinacionesTest.java:237 · `elCatalogoNoTieneMasQueElContenido` no ve nombres nuevos
- **`cat6`** (un `kyc-ar` extra en `catalogo()`) **pasa** 17/17. La prueba solo sondea los nombres que ya están en `contenido()`.
- **`cat6b`** (`kyc-pe 9.9.9` extra) y **`cat6c`** (versión de core 4.0.0 extra) sí fallan, por esta prueba y por la de «mismo contenido».
- **Por qué no se puede cerrar del todo:** la interfaz `Catalogo` no tiene forma de enumerar, así que el hueco es inherente. Hoy no ocurre, porque `catalogo()` se deriva de `contenido()`. No bloquea.

### Cerrados de la ronda 3
- **F-02 (umbrales genéricos):** las aserciones ≥ 3 y ≥ 2 desaparecieron. Los dos contadores quedan solo en `esperados`, es decir, presencia ≥ 1.
- **F-04:** el comentario de la suite en synkra-core está ahora encima de sus dos aserciones (líneas ~615–617).
- **F-03:**
  - `val3` ahora falla en `esperadoYEntradaIncoherentesSeRechazan`, igual que `val0`, `val1`, `val2` y `val4`.
  - `Contra` (recompilado contra las clases de HEAD) confirma que ahora se rechazan `CoreDelCatalogo` con un descriptor de otro core, `CoreDelCatalogo` con core null y `Caso` con id null.
  - `cat7` ya la detecta la suite que heredan U02 y U07, no solo src/test: fallan 10 casos y las 2 de catálogo.
- **Lo que no se hizo es aceptable:**
  - `Contra` sigue construyendo «Grupo INTEGRIDAD con PorSeleccion» y «LECTOR con PLUGIN_AUSENTE».
  - La justificación de la respuesta se sostiene: el diseño no fija una taxonomía de códigos por origen, y los 12 datos reales los fijan las pruebas de forma y de caso.
  - U02 y U07 consumen casos, no los construyen. No lo propongo como candidata.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Sigue en pie la observación de la ronda 3 sobre el umbral 6 de «core con texto», de U01-T11: hoy media 25,3 y var/media ~1,0, en el límite. Puede ir junto a C-25.

## Rutas de transcripciones largas
Todas bajo /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/:
- **Corridas de los CA:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/logs/ca1.log, ca4.log, ca5.log, ca7.log
- **Mutaciones:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/logs/mut-suite.txt, mut-cat.txt y mut-*.log con xml-*/. Las de registro (none, regnone, regall, regdrop, regvacio, regotra, × 3) están en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/tasks/bhem2mb9w.output
- **Mediciones:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/logs/medicion-head-1.txt, medicion-head-2.txt, medicion-regdrop.txt
- **Sonda Contra:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/logs/contra.txt, con la fuente en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/contra/src/otro/Contra.java
- **Scripts:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r4/scripts/{mut.py,run.sh,run1.sh,med.sh,medboth.sh,MedicionTmp.java}

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:553|la clave «otra carpeta: texto con core-b registrado» cuenta la etiqueta, no el registro: quitar cores.put("core-b") (regdrop), registrarlo vacío o registrar otro core pasa 3/3
INFORME: revisiones/U01-T12/ronda-4.md
```
