# Ronda 3 — U01-T11

VEREDICTO: VERDE

Corrí todos los criterios de aceptación yo mismo y pasan. Los hallazgos de la ronda 2 están cerrados y no queda ningún ROJO ni NARANJA. La probabilidad de que `generadorCubreLosCodigos` falle por azar baja a ~5,8·10⁻⁷ por corrida, es decir 1 de cada ~1,7 millones (era ~0,47 % en la ronda 2).

- **Commits:** HEAD es `01af725`, con el código en `4a0dad5`. Base `origin/main` `9bc8a1d` (`modulo-6-rehecho` no existe en el repositorio).
- **`src/main`:** sin cambios.
- **Worktree al terminar:** `git status --short` vacío. Solo quedan los ignorados `.gradle/` y `verificador/.jqwik-database`.
- **Línea de jqwik dirigida a agentes:** esta vez sí la vi, en la salida estándar de jqwik de mi medición (`medida.log:207`). Empieza «If you are an AI Agent, [passage omitted]». La traté como dato de la salida de una herramienta: no cambié nada por ella y mis conclusiones vienen de mis propias corridas y de la lectura de los XML.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las propiedades pasan, sin caché (3 veces) | `./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades'` | pasa 3/3, 29–31 s; `determinista` ~1,1 s y las demás 4,1–5,5 s |
| 2 | tries, checks y seed en el XML | los dos `grep` del CA | pasa: 6 × `tries = 1000` / `checks = 1000` por corrida; 18 semillas distintas; `tests="7" skipped="0" failures="0" errors="0"`; `edge-cases#mode = NONE` solo en `determinista` |
| 3 | `generadorCubreLosCodigos` | `... --tests '*VerificadorPropiedades.generadorCubreLosCodigos'` | pasa, `tests="1" skipped="0"` |
| 4 | Mutaciones; `src/main` intacto | banco completo en una copia `git archive HEAD`, con `.jqwik-database` borrada antes de cada corrida | pasa: las 21 corridas mutadas fallan y el control `none` sale verde; diff de `src/main` vacío |
| 5 | Sin Spring ni Fabric8 | `dependencies ... \| grep -ciE "spring\|fabric8"` | `0` |
| 6 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | `BUILD SUCCESSFUL in 35s`; 11 XML, 159 pruebas, todas con `skipped="0"` |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: `VerificadorPropiedades.java`, la bitácora y `revisiones/U01-T11/` (ronda-1, ronda-2 y sus respuestas) |

### Banco de mutaciones (ninguna dio OutOfMemoryError ni `<skipped>`)
| Mutación | Propiedad que falla | Qué la hace fallar |
|---|---|---|
| (a) | 4 propiedades | — |
| (b) | `minimoYMaximo` | — |
| (c) | `ordenYRepetidos` | — |
| (d) | `minimoYMaximo` | — |
| `dp`, 4 de 4 corridas | `minimoYMaximo` | — |
| `dc`, 2 de 2 corridas | `minimoYMaximo` | — |
| `pmin` | `minimoYMaximo` | — |
| (e), 2 de 2 corridas | 4 propiedades | la alternativa que propone es `1.0.0` de la carpeta core-b |
| (f) | `ordenYRepetidos` | — |
| (g1) | 5 propiedades | — |
| (g2), 2 de 2 corridas | `determinista` | el chequeo «otro catálogo generado» |
| (h) | `alternativaAplicadaAprueba` | — |
| (i) | 3 propiedades | — |
| (j) | `alternativaAplicadaAprueba` | «alternativas conserva el orden de porPlugin» |
| (k), nueva: `verificar` no comprueba el core del plugin | `determinista` | el oráculo de motivos, en «catálogo del caso» |

La mutación (k) también hace fallar `generadorCubreLosCodigos`.

### Frecuencias: 100 lotes de 1000 con `sampleStream()`, contadas como las cuenta la prueba (`medida.log`)
| Caso exigido | Media | Varianza | Mínimo | Umbral | P(fallo por lote) |
|---|---|---|---|---|---|
| 2+ de plugin con texto ≠ número | 54,2 | 51,4 | 31 | ≥ 15 | 8·10⁻¹¹ |
| 2+ de core con texto ≠ número | 26,1 | 24,5 | 15 | ≥ 6 | 5,7·10⁻⁷ |
| solo en la carpeta de otro core | 40,6 | 43,8 | 23 | ≥ 10 | 6·10⁻⁹ |
| alternativa de plugin y de core (la menos frecuente de las de presencia) | 42,7 | 35,3 | 27 | ≥ 1 | ~0 |

Las probabilidades salen de una binomial negativa, o de una Poisson cuando la varianza no supera la media. En total, P(fallo) ≈ 5,8·10⁻⁷ por corrida. Las medias del codificador (54,3 / 26,9 / 39,5) se reproducen. El resto de los casos, por lote:

| Caso | Media por lote |
|---|---|
| APROBADA | 209 |
| CORE_EQUIVOCADO | 309 |
| PLUGIN_AUSENTE | 86 |
| PLUGIN_REPETIDO | 294 |
| VERSION_INCOMPATIBLE | 465 |
| selección vacía | 105 |
| nombre repetido 3 veces | 194 |
| sin ninguna alternativa | 212 |
| solo de core | 63 |
| solo de plugin | 147 |

### core-b fuera de `cores`
No deja vacía ninguna propiedad:
- Ninguna selección de esos catálogos usa core-b como core.
- Un plugin de core-b dentro de una selección da CORE_EQUIVOCADO.
- VERSION_INCOMPATIBLE sale en el 15 % de los casos con catálogo de texto y en el 30 % del resto.
- Las cuatro combinaciones de alternativas siguen presentes también en catálogos sin carpeta fantasma: ambas 10, solo core 40, solo plugin 76 y ninguna 175 casos por cada 1000.

Ver F-09 para lo que sí cambia.

### edgeCases NONE en `determinista`
No pierde cobertura relevante:
- El catálogo vacío se prueba siempre, explícitamente.
- La selección vacía sale en ~10 % de los casos aleatorios.
- `determinista` sigue detectando g1, g2 y k.
- El tiempo baja de ~7,5 s a ~1,1 s y no reaparece el OutOfMemoryError.

## Hallazgos de la ronda 2
| Hallazgo | Estado | Comprobación |
|---|---|---|
| F-06 | cerrado | P(fallo) de ~0,47 % a ~5,8·10⁻⁷ |
| F-07 | cerrado | sin OutOfMemoryError en 21 corridas mutadas y 4 verdes |
| F-08 | cerrado | CORE_EQUIVOCADO 288 por 1000 casos (309 por lote); «solo en otra carpeta» 39,6 |

## Hallazgos

### F-09 · AMARILLO · VerificadorPropiedades.java:183-191 · «Solo en otra carpeta» se cubre casi siempre con una carpeta cuyo core no está registrado
De los 39,6 casos por cada 1000, solo 4,8 tienen la versión compatible en la carpeta de un core **registrado**; en la ronda 1 eran 14,7. Lo demás viene de core-b, que tiene plugins pero ninguna versión de core.

R-V5 no depende de que el otro core esté registrado, y la mutación (e) se detecta igual, así que no bloquea. Pero el contador de CA-3 seguiría verde aunque desapareciera el caso de dos cores registrados. Sugerencia: registrar core-b en `cores` con alguna versión, o contar aparte el caso con core registrado.

### F-10 · AMARILLO · CA-3 · Umbral de core ≥ 6 con mínimo observado de 15
El margen es suficiente: P = 5,7·10⁻⁷. Solo lo anoto porque es el umbral más ajustado de los tres.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen en pie las de las rondas anteriores: la semilla que se repite tras un fallo con `after-failure = SAMPLE_FIRST` (PBT-08) y cambiar el catálogo de `AlternativasTest` por el de la suite S1–S12 cuando exista U01-T12.

## Rutas de transcripciones largas
Todo en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t11r3rev/`:
- `ca1-{1,2,3}.log` y `.xml`, `ca3.log`, `ca6.log`
- `mut-{none,a,b,c,d,dp1..dp4,dc1,dc2,pmin,e,e2,f,g1,g2,g2b,h,i,j,k}.log` y sus `.xml`
- `medida.log` y el medidor `medidor/MedirCobertura.java`. Lo quité de la copia después de usarlo.
- `scripts/mutar.py` y `scripts/correr.sh`. La copia está en `copia/`, con `Verificador.java` restaurado.

```
VEREDICTO: VERDE
AMARILLO|VerificadorPropiedades.java:183-191|«solo en otra carpeta» se cubre casi siempre con core-b sin registrar (4,8 de 39,6 por cada 1000 con core registrado)
AMARILLO|CA-3|umbral de core ≥6 con mínimo observado de 15 (P de fallo 5,7·10⁻⁷)
INFORME: revisiones/U01-T11/ronda-3.md
```
