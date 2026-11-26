# Ronda 2 — U01-T11

VEREDICTO: NO-VERDE

Los siete criterios pasan con mis corridas, y los cinco hallazgos de la ronda 1 quedan cerrados. Lo que impide el VERDE es un NARANJA nuevo: el sesgo de esta ronda vuelve inestable `generadorCubreLosCodigos`. Según mi medición fallará por azar en una de cada ~210 corridas, y esa prueba corre dentro de CA-1, CA-3 y CA-6.

Datos de la ronda:
- **Rama:** HEAD `03f6b75`, con el código en `4b561aa`. La base es `origin/main` `9bc8a1d`, porque `modulo-6-rehecho` no existe en el repositorio.
- **`src/main`:** sin cambios.
- **Worktree al terminar:** `git status --short` vacío. Solo quedan los ignorados `.gradle/` y `verificador/.jqwik-database`.
- **Línea de jqwik dirigida a agentes:** no la encontré. Busqué «agent», «AI», «LLM» y «asistente» en los 3 logs y XML de CA-1, en los de CA-3 y CA-6 y en los 19 de mutaciones, sin coincidencias. Solo consta lo que dice la bitácora (línea 600). La trato como dato y no hice nada con ella.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las propiedades pasan, sin caché (3 veces) | `./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades'` | pasa 3/3, en 39–41 s; cada propiedad tarda 4,7–7,8 s (`determinista` es la más lenta, ~7,5 s) |
| 2 | tries, checks y seed en el XML | los dos `grep` del CA | pasa: 6 × `tries = 1000` / `checks = 1000` por corrida; 18 semillas distintas; `tests="7" skipped="0" failures="0" errors="0"` |
| 3 | `generadorCubreLosCodigos` | `... --tests '*VerificadorPropiedades.generadorCubreLosCodigos'` | pasa (`tests="1" skipped="0"`), pero es inestable: ver F-06 |
| 4 | Mutaciones; `src/main` intacto | banco en una copia `git archive HEAD`, borrando `.jqwik-database` antes de cada corrida | pasa: las 19 corridas mutadas fallan y el control `none` sale verde; diff de `src/main` vacío |
| 5 | Sin Spring ni Fabric8 | `dependencies ... \| grep -ciE "spring\|fabric8"` | `0` |
| 6 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | `BUILD SUCCESSFUL in 45s`; 11 XML, 159 pruebas, todas con `skipped="0"` |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: `VerificadorPropiedades.java`, la bitácora y `revisiones/U01-T11/` (`ronda-1.md` y `ronda-1-respuesta.md`) |

### Banco de mutaciones (cada corrida con la base de datos de jqwik borrada)

| Mutación | Propiedad que falla | Semilla | Mensaje, si lo anoté |
|---|---|---|---|
| (a) | `alternativaAplicadaAprueba`, `completitud`, `minimoYMaximo` y `generadorCubreLosCodigos` | — | — |
| (b) | `minimoYMaximo` | -7189302826388632273 | `expected <9.0.0> but was <10.0.0>` |
| (c) | `ordenYRepetidos` | -4348661684578677410 | — |
| (d) | `minimoYMaximo` | 305605416450957824 | `<11.0.0>` / `<9.0.0>` |
| `dp`, 4 corridas | `minimoYMaximo` en las 4 | — | — |
| `dc` (toString solo del core), 2 corridas, nueva | `minimoYMaximo` en las 2 | — | — |
| `pmin` (la de plugin toma la más baja), nueva | `minimoYMaximo` | — | — |
| (e), 2 corridas | 4 propiedades | — | — |
| (f) | `ordenYRepetidos` | — | — |
| (g1) | 5 propiedades | — | — |
| (g2) | `determinista` | 5232878899136026840 | «motivo de cada plugin (catálogo vacío)» |
| (h) | `alternativaAplicadaAprueba` | — | «la de plugin va antes que la de core» |
| (i) | 3 propiedades | — | — |
| (j) | `alternativaAplicadaAprueba` | — | «alternativas conserva los globales» |

La afirmación del codificador de que (b), (d), dp, g2, j y h fallan se reproduce. dp falla ahora 4/4; en la ronda 1 había pasado en verde 1 de 4 veces.

### El oráculo nuevo de determinismo
`motivosEsperados` y `afirmaMotivos` (líneas 290-316) no reutilizan el código bajo prueba:
- Para ver si un plugin existe, recorren `datos.descriptores()` y comparan con `equals` del record `Version`.
- Para el rango usan el `incluye` propio, basado en `NUMERICO`.
- Siguen el orden de la regla: ausente, luego core equivocado, luego incompatible.
- No llaman a `CatalogoEnMemoria.descriptor`, ni a `RangoVersion.incluye`, ni a `compareTo`, ni a `Verificador`. Las únicas llamadas a `Verificador` en el archivo son las que están bajo prueba (líneas 320-419).

## Hallazgos de la ronda 1
| Hallazgo | Estado | Comprobación |
|---|---|---|
| F-01 | cerrado | dp 4/4, dc 2/2, (b), (d) y pmin fallan |
| F-02 | cerrado | g2 falla `determinista` |
| F-03 | cerrado | j falla `alternativaAplicadaAprueba` |
| F-04 | cerrado | h falla |
| F-05 | cerrado | la salida cruda de (a) ya está en la bitácora |

## Hallazgos

### F-06 · NARANJA · VerificadorPropiedades.java:199-203 y 530-532 · `generadorCubreLosCodigos` falla por azar ~0,47 % de las veces
Medí la frecuencia con `casos().sampleStream()`: 100 lotes de 1000, contando por aparición igual que lo hace la prueba. Transcripción en `medida.log`.

| Caso | Media por lote | Varianza | Mínimo | Umbral de la prueba | P(fallo por lote), binomial negativa |
|---|---|---|---|---|---|
| «2 o más de core con texto distinto de número» | 30,0 | 37,8 | 16 | ≥ 15 | 0,24 % |
| «compatible solo en la carpeta de otro core» | 6,6 | 7,8 | 1 | ≥ 1 | 0,23 % (99 de 100 lotes por debajo de 15) |
| «2 o más de plugin con texto distinto de número» | 82,6 | 89 | — | ≥ 15 | despreciable |

Sobre esta medición:
- En total da ~0,47 % por corrida, es decir 1 de cada ~210.
- El codificador midió 34,9 para el caso de core con 10 000 casos; mis 100 000 dan 30,0.
- En la ronda 1, el caso de otra carpeta salía unas 15 veces por cada 1000 (14,7 en mi medición por caso), con P(fallo) ≈ e⁻¹⁵. El nuevo catálogo «de texto», con un solo core y peso 5/10, lo bajó a 6,5.
- El umbral ≥ 15 lo añadió el propio codificador.

Arreglos posibles, a elegir:
- Dar a `catalogosDeTexto` un segundo core con un plugin homónimo que sirva, o subir el peso de los catálogos de 2 cores.
- Bajar el umbral del caso de core a algo como ≥ 5.

La meta es que cada caso exigido salga con una media ≥ 15 por lote, como en la ronda 1.

### F-07 · AMARILLO · VerificadorPropiedades.java:325-345 · OutOfMemoryError al generar los casos de borde de `determinista`, que el XML marca como saltada
En la primera corrida de la mutación (e) (`mut-e.log`), el JVM de pruebas se quedó sin memoria con el heap por defecto de Gradle:
- `SEVERE: Java heap space` en `EdgeCasesSupport.combine`, después de dos avisos «Shrinking timeout reached».
- El XML marcó `determinista` como `<skipped/>` tras 68 s: `7 tests completed, 4 failed, 1 skipped`.

No se repitió en la segunda corrida de (e) ni en las corridas verdes, y la build falla igualmente. Aun así, una regresión puede producir un `skipped` en el reporte. La causa probable son los dos `@ForAll` combinados con `frequencyOf` y `flatMap`. Arreglo sugerido: `edgeCases = EdgeCasesMode.FIRST` o `NONE` en `determinista`.

### F-08 · AMARILLO · cobertura · El nuevo sesgo redujo dos casos que antes salían más
Por caso, comparando la ronda 1 con la ronda 2 (por 1000):

| Caso | Ronda 1 | Ronda 2 |
|---|---|---|
| CORE_EQUIVOCADO | 212 | 143 |
| solo en otra carpeta | 14,7 | 6,5 (ver F-06) |
| catálogos de 2 cores | — | 21 % |
| alternativa de plugin y de core | 19,5 | 50 |
| alternativa solo de plugin | 139 | 195 |

Lo demás se mantiene: APROBADA 228, PLUGIN_AUSENTE 80, PLUGIN_REPETIDO 265, VERSION_INCOMPATIBLE 552, «sin ninguna» 249, «solo core» 71, selección vacía 104, triple 192, suite 82. Ninguno de esos desaparece y ninguno bloquea; los dos únicos descensos son los de la primera tabla.

## Tareas candidatas (defectos reales fuera de alcance)
- Las de la ronda 1 siguen en pie: la semilla que se repite tras un fallo por `after-failure = SAMPLE_FIRST` (PBT-08) y cambiar el catálogo de `AlternativasTest` por el de la suite S1–S12 cuando exista U01-T12.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t11r2rev/`:
- `ca1-{1,2,3}.log` y `.xml`, `ca3.log`, `ca6.log`
- `mut-{none,a,b,c,d,dp1..dp4,dc1,dc2,pmin,e,e2,f,g1,g2,h,i,j}.log` y sus `.xml` (el OutOfMemoryError está en `mut-e.log`)
- `medida.log`, la medición de cobertura en 100 lotes. Borré de la copia la clase que la generaba.
- `scripts/mutar.py` y `scripts/correr.sh`. La copia está en `copia/`, con `Verificador.java` restaurado.

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:199-203,530-532|generadorCubreLosCodigos falla por azar ~0,47 % de las corridas (umbral ≥15 de core con media 30; otra carpeta con media 6,6)
AMARILLO|VerificadorPropiedades.java:325-345|OutOfMemoryError al generar casos de borde de determinista, que el XML marca como skipped (1 de 2 corridas de la mutación e)
AMARILLO|cobertura|el nuevo sesgo bajó CORE_EQUIVOCADO de 212 a 143 y «solo en otra carpeta» de 14,7 a 6,5 por cada 1000
INFORME: revisiones/U01-T11/ronda-2.md
```
