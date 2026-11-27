# Ronda 6 — U01-T12

VEREDICTO: VERDE

Los dos hallazgos de la ronda 5 quedan cerrados. Corrí yo mismo los 8 CA y todos pasan. Las mutaciones que pediste fallan donde deben y el control sin mutar pasa. Con la varianza agregada de todas mis mediciones (700 lotes), la puerta nueva con umbral 7 cumple el límite del orquestador. No queda ningún ROJO ni NARANJA en pie.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object tareas/U01-T12-suite-combinaciones.md` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311, el esperado.
- **Ramas:** HEAD 9bcffc9. El merge-base con origin/main es b4259d8, igual a origin/main.
- **Edad de la evidencia:** el código cambió en 21aacab: 10 líneas añadidas y 1 quitada, todas en `VerificadorPropiedades.java`. 9bcffc9 solo trae la respuesta y la bitácora. Los 178 tests de la bitácora coinciden con mi CA-7.
- **Mutaciones:** todas en copias `git archive HEAD` en el scratchpad (`u01t12r6/{pristine,head,head2,head3,head4,head5}`). El worktree no se tocó.
- **Worktree al terminar:** `git status --short` sale vacío. Solo quedan cachés ignoradas (`build/`, `.gradle/`, `.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="17" skipped="0" failures="0" errors="0"`. Hay 10 testcase `"S1"`–`"S10"` y 7 de forma. XML fresco (05:02:08) |
| 2 | Resultado exacto del diseño | el mismo, más `git diff c7e83ff..21aacab --stat` | pasa. `SuiteCombinaciones*` no cambió desde la ronda 5. Las mutaciones (b)–(f) siguen haciendo fallar el caso exacto |
| 3 | Mutaciones (a)–(f), con (d) = «sin alternativa de core» | `scripts/rep.sh head4 <m> 1 '*SuiteCombinacionesTest'` | pasa: (a) falla en 3 de forma (16 tests); (b) en S7; (b2) en 10; (c) en S9; (d) en S4 y S5; (e) y (f) en S8 y S9. Sin mutar: 17/17 |
| 3' | Sin cambios en `src/main` | `git diff --stat b4259d8...HEAD -- verificador/src/main` | vacío |
| 4 | testFixtures usable | `./gradlew :verificador:testFixturesJar --rerun` + zipfile | pasa. El JAR trae `SuiteCombinaciones`, `SuiteCombinaciones$Caso` y `CatalogoEnMemoria` |
| 5 | C-26 sin romper U01-T11 | borré `.jqwik-database` y corrí `--tests '*VerificadorPropiedades'` + grep | pasa: BUILD SUCCESSFUL (58 s), 6 × `tries = 1000`, 6 × `checks = 1000`, 9/9, 0 saltadas. Fila C-26 «Cerrada en U01-T12» |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL (1 min). 12 XML suman 178 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat b4259d8...HEAD` | pasa: 15 archivos, todos de la lista: `CANDIDATAS.md`, la bitácora, `revisiones/U01-T12/*` (10), `SuiteCombinaciones.java`, `SuiteCombinacionesTest.java` y `VerificadorPropiedades.java` |

**Mutaciones de U01-T11**, con `--tests '*VerificadorPropiedades' --tests '*SuiteCombinacionesTest'`:
- `dp` hace fallar `minimoYMaximo` (`expected: <10.0.0> but was: <2.0.0>`).
- `g2` hace fallar `determinista`.
- `j` hace fallar `generadorCubreLosCodigos` («faltan: [PLUGIN_REPETIDO]»), `alternativaAplicadaAprueba` y S10.

## Comprobación 1: F-01 de la ronda 5, cerrado
**Cambio:** el umbral pasa de 8 a 7 en `VerificadorPropiedades.java:643`.

**Medición:** repetí 2 × 100 lotes de 1000 con `sampleStream()`, borrando `.jqwik-database` antes de cada corrida. Las agregué con mis 2 corridas de la ronda 4 y mis 3 de la ronda 5: 7 corridas, 700 lotes. La varianza agregada sale de las sumas de cuadrados dentro de cada corrida más las de entre corridas.

| puerta (nueva en U01-T12) | umbral | media agr. | var/media agr. | mín. visto | media/umbral | P bin. neg. (agr.) | P Poisson |
|---|---|---|---|---|---|---|---|
| suite en synkra-core con VERSION_INCOMPATIBLE | 7 | 30,43 | 1,081 | 15 | 4,35x | **1,8e-7: cumple** | 8,3e-8 |
| suite en synkra-core | 20 | 70,67 | 1,036 | 42 | 3,53x | 6,0e-13 | 3,1e-13 |
| otra carpeta: texto con core-b registrado | 1 | 16,42 | 1,085 | 4 | 16,4x | 1,4e-7 | 7,4e-8 |
| otra carpeta: texto con core-b sin registrar | 1 | 17,48 | 1,039 | 7 | 17,5x | 3,6e-8 | 2,6e-8 |

- **Por corrida:** en esta ronda, var/media de la puerta VERSION_INCOMPATIBLE da 0,86 y 1,01, con medias 30,64 y 30,45.
- **Sobredispersión:** con 700 lotes, el agregado de 1,08 está a z = 1,5 de 1. Es compatible con Poisson. La ronda 5 había estimado 1,20 con 300 lotes.
- **Corrida más dispersa:** es solo informativo, porque la regla del orquestador es la varianza agregada. Con var/media 1,38 el umbral 7 daría 1,65e-6, y la bitácora lo declara (1,5e-6).
- **U01-T11:** sus umbrales están fuera de alcance y no los evalúo como hallazgo.

## Comprobación 2: F-02 de la ronda 5, cerrado
**Cambios en `catalogosDeTextoRegistranCoreBSegunSuOrigen`:**
- La línea 596 exige entre 100 y 200 catálogos registrados de 300.
- La línea 591 exige que cada versión registrada de `core-b` caiga en el rango de todos los descriptores de `core-b`.

**Mutaciones, 3 corridas cada una (`scripts/rep.sh head3 <m> 3 '*VerificadorPropiedades'`):**

| mutación | resultado (3 de 3) |
|---|---|
| reg13 (`of(true,false,false,false)`) | falla `catalogosDeTexto…` en la línea 596 (proporción) |
| reg31 (`of(true,true,true,false)`) | falla `catalogosDeTexto…` en la línea 596 |
| regver (`core-b` con 0.1.0) | falla `catalogosDeTexto…` en la línea 591 (rango) |
| regnone, regdrop | fallan `generadorCubreLosCodigos` («faltan: [otra carpeta: texto con core-b registrado]») y `catalogosDeTexto…` |
| regswap | falla en las dos pruebas; faltan ambas claves |
| none | BUILD SUCCESSFUL 3/3 |

**P de que la proporción falle por azar:**
- **Teórica:** con X ~ Bin(300, ½), la exacta da P(X ∉ [100, 200]) = 3,9e-9.
- **Medición empírica:** prueba temporal en la copia `head5`, 2000 repeticiones de `catalogosDeTexto().sampleStream().limit(300)`. Da media 150,03, var 77,9 (var/media 1,04 respecto de la binomial, que daría 75), mín. 115 y máx. 175.
- **Normal con la varianza medida:** 1,05e-8.
- **Conclusión:** cumple con holgura el límite de 1e-6. La comprobación de rango es determinista: `core-b` 1.0.0 contra `>=1.0.0`, sin azar.

## Hallazgos
No quedan ROJO ni NARANJA.

### F-03 (ronda 4) · AMARILLO · sigue en pie, aceptado como inherente
Sigue sin poder detectarse `cat6`. La justificación de la respuesta de la ronda 4 se sostiene. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Sigue en pie la de la ronda 3: el umbral 6 de «core con texto distinto de número» de U01-T11. Con la varianza de las corridas más dispersas, su P queda cerca de 1e-6 o por encima (la bitácora da 8,7e-7 con las de esta ronda). Puede ir junto a C-25.

## Rutas de transcripciones largas
- **CA:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/logs/ca1.log (también ca4.log, ca5.log y ca7.log en la misma carpeta)
- **Mutaciones de la suite y de U01-T11:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/logs/suite.txt
- **Mutaciones de registro (7 × 3):** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/logs/reg.txt, con un log por corrida en logs/rep-*.log
- **Mediciones:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/logs/medicion-head-1.txt y medicion-head-2.txt
- **Agregado de 700 lotes:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/logs/agregado.txt
- **Proporción empírica:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/head5/verificador/proporcion.txt
- **Scripts:** /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r6/scripts/mut.py, rep.sh, med.sh, agg.py, MedicionTmp.java y ProporcionTmp.java

```
VEREDICTO: VERDE
AMARILLO|F-03 (ronda 4)|cat6 indetectable, aceptado como inherente; sin ROJO ni NARANJA en pie
INFORME: revisiones/U01-T12/ronda-6.md
```
