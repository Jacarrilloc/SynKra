# Ronda 1 — U01-T05

VEREDICTO: VERDE

Contexto: worktree /home/user/wt-m6r-U01-T05, HEAD db25dfb. Diff `3bf6e7d...HEAD`: 2 archivos, `bitacoras/U01-T05.md` y `verificador/src/test/java/io/synkra/verificador/RangoVersionPropiedades.java` (nuevo, 81 líneas). Hash de la tarea `b2185593c5bc1467b00d995b4a2cd3f18719fd73`, coincide con el del despacho.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las propiedades pasan sin caché | `./gradlew :verificador:test --rerun --tests '*RangoVersionPropiedades'` | pasa. `BUILD SUCCESSFUL`. XML: tests=2, skipped=0, failures=0, errors=0, time=0.403 s |
| 2 | 1000 intentos y semilla en el reporte | `grep -o -E 'tries = ...\|checks = ...\|seed = ...' verificador/build/test-results/test/TEST-*RangoVersionPropiedades.xml` | pasa. 2× `tries = 1000`, 2× `checks = 1000`, `seed = -113901649662785468` y `seed = 5386313770973280657`. Son semillas distintas de las de la bitácora, así que no hay semilla fija. `generation = RANDOMIZED` |
| 3 | La mutación N+2 se detecta y se revierte | Mutación `Math.incrementExact(n)` → `Math.addExact(n, 2)` hecha en una **copia** (`git archive HEAD`) en el scratchpad, no en el worktree. Después corrí CA-1 en esa copia y `git diff --stat -- verificador/src/main` en el worktree | pasa. Las dos propiedades FAILED y `BUILD FAILED`. Shrunk Sample de N.x: `arg0: 2, arg1: 3`. Shrunk Sample de ida y vuelta: `RangoVersion[minimo=0.0.0, maximo=Optional[1.0.0], texto=0.x]`. `git diff --stat` en el worktree: vacío. La bitácora trae el par falla-pasa |
| 4 | Todo el verificador pasa sin caché | `./gradlew :verificador:check --rerun` | pasa. `BUILD SUCCESSFUL`. XML: RangoVersionPropiedades 2/0 skip, RangoVersionTest 36/0 skip, VersionTest 16/0 skip, sin fallos |

## Comprobaciones de alcance y del plan
- No hay cambios en `Version`, `RangoVersion`, `Resultado`, `Motivo`, `CodigoMotivo` ni `build.gradle.kts`, ni en `testFixtures`. No hay `seed =` en `@Property`. No hay `Assume` ni filtros (`checks = tries`). No hay desborde.
- Los generadores cubren las cuatro formas de R-V2: `N.x`, `N.M.x`, exacta, `>=A` y `>=A <B`. En `>=A <B`, `A < B` se cumple por construcción. Los espacios U+0020 van de 0 a 3 alrededor de `>=` y `<`.
- Valores incrustados: ninguno nuevo visible para el usuario. Solo hay literales de prueba.
- El worktree está limpio al terminar. Solo quedan rutas ignoradas: `.gradle/`, `build/`, `verificador/build/`, `verificador/.jqwik-database`.

## Hallazgos
### F-01 · AMARILLO · RangoVersionPropiedades.java:31-39 (`mayorQue`) · El máximo `B` puede salirse de `0..50`
`a.parche() + d`, `a.menor() + d` y `a.mayor() + d` con `d` en `1..50` dan componentes de hasta 100. El plan (PBT-07) pide componentes en `0..50`. La propiedad no es incorrecta, pero se aparta del rango declarado del generador. Arreglo posible: acotar `d` a `1..(50 - componente)` o documentar la desviación.

### F-02 · AMARILLO · RangoVersionPropiedades.java:63-71 (`nXIncluyeSoloSuMayor`) · Reducción débil del contraejemplo
`n` y `mayorAzar` se generan por separado. Contra un fallo de uno más, el shrinker no conserva la relación `mayorAzar == n+1` y se queda en `2, 3` en mi corrida (`3, 4` en la bitácora), cuando el mínimo sería `0, 1`. Además, la detección de N+2 depende de que coincidan `mayorAzar == n+1` y `mismoMayor == false`. Eso pasa en torno al 1 % de los intentos: unos 10 casos por cada 1000, con probabilidad de que no salga ninguno de unos 5e-5. Generar el mayor como `n + desplazamiento` (desplazamiento en `-n..50-n`) haría la reducción canónica y la detección más densa. No bloquea, porque CA-3 se cumple.

## Tareas candidatas (defectos reales fuera de alcance)
- `business-logic-model.md`, fila PBT-08: dice «Semilla fija registrada», y eso contradice la tarea («la semilla cambia en cada ejecución y solo se registra»). Conviene alinear el artefacto.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3-rojo.log (salida de CA-1 con la mutación N+2)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3/ (copia mutada, fuera del worktree)

Nota: no tengo herramientas de escritura, así que no creé `revisiones/U01-T05/ronda-1.md`. El texto de arriba es el informe para que lo archive el orquestador.

```
VEREDICTO: VERDE
AMARILLO|RangoVersionPropiedades.java:31-39|mayorQue genera componentes de B fuera de 0..50 (hasta 100)
AMARILLO|RangoVersionPropiedades.java:63-71|Reducción débil del contraejemplo N.x (2,3 en vez de 0,1) por generar n y mayorAzar por separado
INFORME: revisiones/U01-T05/ronda-1.md
```
