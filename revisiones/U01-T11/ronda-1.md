# Ronda 1 — U01-T11

VEREDICTO: NO-VERDE

Corrí los siete criterios yo mismo y todos pasan. El veredicto no es verde por dos hallazgos NARANJA: dos mutaciones razonables que pediste sobreviven, una de ellas en 1 de cada 4 corridas.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object tareas/U01-T11-propiedades-verificador.md` da `c25f16fa4c1244bcd1f79635a367bd49471ff998`, coincide.
- **HEAD y base:** HEAD es `c11ebc2` y la base `origin/main` es `9bc8a1d`.
- **Rama base:** `modulo-6-rehecho` no existe en el repo (`git rev-parse` falla), así que usé `origin/main`, como indicó el despacho.
- **Edad de la evidencia:** los tiempos de la bitácora (18:57Z a 19:05Z) son posteriores al commit `65d90f3` (18:57:09Z). El último commit solo añade la bitácora.
- **Worktree:** `git status --short` queda vacío al terminar. Solo hay ignorados (`.gradle/` y `verificador/.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las propiedades pasan, sin caché (3 veces) | `./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades'` | pasa 3/3; 6,0 a 7,8 s por propiedad (< 30 s) |
| 2 | tries, checks y seed en el XML | los dos `grep` del CA sobre `TEST-*VerificadorPropiedades.xml` | pasa: 6 × `tries = 1000` / `checks = 1000`, 6 semillas distintas en cada corrida (18 en total); `tests="7" skipped="0" failures="0" errors="0"`; `generation = RANDOMIZED` |
| 3 | `generadorCubreLosCodigos` | `... --tests '*VerificadorPropiedades.generadorCubreLosCodigos'` | pasa, `tests="1" skipped="0"` |
| 4 | Mutaciones (a) a (d) fallan y vuelven a verde; `src/main` sin cambios | copia `git archive HEAD` en el scratchpad, con `.jqwik-database` borrada antes de cada corrida; `git diff --stat origin/main...HEAD -- verificador/src/main` | pasa: (a), (b), (c) y (d) fallan; el control `none` sale verde; el diff de `src/main` sale vacío |
| 5 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | `0` |
| 6 | Todo el verificador pasa, sin caché | `./gradlew :verificador:test --rerun :verificador:check` | `BUILD SUCCESSFUL in 48s`; 11 XML, 159 pruebas, todas con `skipped="0"` |
| 7 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: solo `VerificadorPropiedades.java` y `bitacoras/U01-T11.md` |

Mutaciones de CA-4 (mis corridas):

| Mutación | Propiedad que falla | Intentos | Semilla | Mensaje |
|---|---|---|---|---|
| (a) | `alternativaAplicadaAprueba`, `completitud`, `minimoYMaximo` y `generadorCubreLosCodigos` | 1 a 8 | — | ver XML |
| (b), corrida limpia | `minimoYMaximo` | — | 8948502402122164740 | `expected <0.1.0> but was <0.1.1>` |
| (c) | `ordenYRepetidos` | 7 | 4768072624123235420 | — |
| (d) | `minimoYMaximo` | 321 | -5631054732747975877 | `expected <2.0.0> but was <10.0.0>` |

En la primera tanda, (b) falló con una semilla que venía de la falla de (a), por `after-failure = SAMPLE_FIRST`. Por eso la repetí con la base de datos de jqwik borrada; esa es la «corrida limpia».

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:56-81, 91-100 · El generador casi nunca llega a los casos que separan máximo y mínimo, y una mutación de toString sobrevive
Medí la cobertura con `casos().sampleStream()` sobre 20 000 casos, con una clase auxiliar dentro de la copia. Lo que separa a una implementación correcta de una mutada es raro:

| Caso que la delata | Casos de 20 000 | Por 1000 intentos | Prob. de no verlo en una corrida |
|---|---|---|---|
| Alguna versión de core da distinto el mínimo y el máximo, es decir (b) | 134 | 6,7 | ~0,1 % |
| Plugin con más alta ≠ más baja (alternativa de plugin que toma la mínima) | 133 | 6,7 | ~0,1 % |
| Comparar por texto ≠ por número, cualquiera de los dos lados, es decir (d) | 69 | 3,5 | ~3 % |
| Lo mismo, solo del lado del plugin | 33 | 1,65 | ~19 % |

La prueba empírica fue la mutación «solo `altaDelPlugin` compara por `Version::toString`». La corrí 4 veces: falló en `dp1`, `dp3` y `dp4`, y **pasó en verde en `dp2`** (`tests="7" failures="0"`). Hay dos causas:
- Cada nombre tiene de 1 a 3 versiones por carpeta, cada una con un rango independiente, así que es raro que dos versiones sirvan para la misma `versionCore`.
- Las versiones de core salen de 9³ combinaciones, así que es raro que dos caigan en el mismo rango.

CA-3 no mide estos casos. Un posible arreglo:
- Sesgar el generador para que las versiones de un mismo plugin compartan rango a menudo, y las versiones de core compartan `mayor` o `menor`.
- Añadir a `generadorCubreLosCodigos` el caso «≥2 candidatas que sirven, con orden de texto ≠ orden numérico».

### F-02 · NARANJA · VerificadorPropiedades.java:230-240 · El determinismo no detecta estado estático que ignora el catálogo
La mutación (g2) mete en `verificar` una caché estática `Map<Seleccion, Veredicto>` que ignora el catálogo. Resultado: `BUILD SUCCESSFUL`, `tests="7" failures="0"`. Las 7 pruebas pasan.

La propiedad solo intercala llamadas sobre el **mismo** catálogo. El alcance dice «no depende de otras llamadas», y el despacho pide expresamente probar «determinismo roto con estado estático». Mi otra mutación de estado estático, (g1) con un contador estático en el mensaje, sí la detecta `determinista`.

Arreglo sugerido: verificar `s` (y `otra`) contra un catálogo distinto entre medias (otro `Datos` generado, o uno vacío) y volver a comparar.

### F-03 · AMARILLO · VerificadorPropiedades.java:279-348 · `alternativas` puede reordenar `porPlugin` o perder los globales sin que falle ninguna propiedad
La mutación (j) hace que `alternativas` invierta `porPlugin` y devuelva los globales vacíos. Ninguna de las 6 propiedades falla; solo cae `generadorCubreLosCodigos` («faltan: [PLUGIN_REPETIDO]»), y de rebote.

business-rules.md:75 exige que `alternativas` no cambie el orden ni los globales, pero CA-1 no lo pide literalmente. Bastaría una aserción en `alternativaAplicadaAprueba`: mismos descriptores en el mismo orden y los mismos globales que `verificar`.

### F-04 · AMARILLO · CA-1 · Ninguna propiedad fija el orden de las alternativas (plugin antes que core)
La mutación (h), que pone la de core antes que la de plugin, pasa en verde. R-V5 no especifica ese orden y `AlternativasTest` lo fija en `claves(...)`, así que no bloquea.

### F-05 · AMARILLO · bitacoras/U01-T11.md · La mutación (a) no tiene salida cruda
Para (a) solo hay un resumen; el registro pegado empieza en `none` y sigue con (b), (c) y (d). Lo reproduje yo y falla igual, en 4 pruebas.

### Las desviaciones declaradas
1. **Componentes {0..5, 9, 10, 11}.** Aceptada: es el ajuste del orquestador y hace posible distinguir (d). Pero ver F-01: (d) se distingue con poca frecuencia.
2. **Rangos propios pequeños, con peso 9/10.** Aceptada. `Generadores.rangos()` usa componentes hasta 50 y casi no acertaría; cubre las cuatro formas de R-V2 y no toca `Generadores.java`.
3. **Catálogo de `AlternativasTest` en vez del de la suite.** Aceptada, porque la suite es U01-T12. Entra en el 7,7 % de los casos (1534 de 20 000). Habrá que cambiarlo cuando exista la suite (ver candidatas).
4. **Completitud como «si y solo si».** Aceptada: es más fuerte que lo pedido. El oráculo recorre `datos.descriptores()` y `datos.cores()` con un comparador propio, `NUMERICO`, y un `incluye` propio. No llama a `alternativas`, ni a `Version.compareTo`, ni a `RangoVersion.incluye`. Revisé las líneas 188-206 y 339-346.
5. **Sin `ronda-1-respuesta.md`.** Correcto: es la ronda 1 y no había hallazgos que responder.

### Búsqueda adversarial sin hallazgo
- **`filter` y `Assume`:** no hay `filter` de jqwik ni `Assume`. Todos los `.filter` son de streams de Java, y `checks = tries` en las 18 propiedades × corrida.
- **Propiedades vacías:** VERSION_INCOMPATIBLE aparece en el 54 % de los casos, así que `completitud` y `minimoYMaximo` no son vacías.
- **Mutaciones que sí se detectan:**
  - (e) «la alternativa de plugin ignora la carpeta» falla en 4 pruebas.
  - (f) «un PLUGIN_REPETIDO por aparición extra» falla `ordenYRepetidos`.
  - (i) «la alternativa de core usa el rango del catálogo» falla en 3 pruebas.
- **Flakiness de CA-3:** el caso más raro de CA-3, «solo en otra carpeta», sale unas 15 veces por cada 1000. La probabilidad de que falte en 1000 muestras es de ~e⁻¹⁵, despreciable.

## Tareas candidatas (defectos reales fuera de alcance)
- **Semilla tras un fallo:** con `after-failure = SAMPLE_FIRST`, después de un fallo jqwik reutiliza la semilla anterior. Lo vi en las corridas de mutación: la misma semilla de `minimoYMaximo` en (a), (b) y (c). Eso choca con PBT-08 («la semilla cambia en cada ejecución»). Es configuración de todo el proyecto, relacionada con C-02.
- **Catálogo de la suite:** cuando exista la suite S1–S12 (U01-T12), sustituir `catalogoDeLaSuite()` por el catálogo de `testFixtures`.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t11r1/`:
- `ca1-1.log` a `ca1-3.log` y `ca1-1.xml` a `ca1-3.xml`
- `ca3.log`, `ca6.log`
- `mut-{none,a,b,c,d,e,f,g1,g2,h,i,dp1..dp4,j}.log` y sus `.xml`
- `scripts/mutar.py` (todas las mutaciones) y `scripts/correr.sh`
- La copia `git archive HEAD` está en `copia/`, con `Verificador.java` restaurado y la clase auxiliar de medición borrada.

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:56-100|el generador casi nunca separa máximo y mínimo; la mutación toString solo del plugin pasó 1 de 4 corridas
NARANJA|VerificadorPropiedades.java:230-240|el determinismo no detecta una caché estática que ignora el catálogo (g2 en verde)
AMARILLO|VerificadorPropiedades.java:279-348|alternativas puede reordenar porPlugin o perder los globales sin que falle ninguna propiedad
AMARILLO|CA-1|ninguna propiedad fija el orden de las alternativas
AMARILLO|bitacoras/U01-T11.md|la mutación (a) no tiene salida cruda
INFORME: revisiones/U01-T11/ronda-1.md
```
