# Ronda 1 — U01-T12

VEREDICTO: NO-VERDE

Hash de la tarea comprobado: `git hash-object tareas/U01-T12-suite-combinaciones.md` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311 y coincide. Worktree en HEAD 82df3cd, base b4259d8. Al terminar, `git status --short` sale vacío: solo quedan las cachés ignoradas (`.gradle/`, `build/`, `verificador/.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas con su id | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: `tests="15" skipped="0" failures="0" errors="0"`. Hay 10 testcase, de `"S1"` a `"S10"`, y 5 pruebas de forma |
| 2 | Resultado exacto del diseño | el mismo, más lectura del código | pasa. Los datos coinciden con business-logic-model.md: nombres, versiones, rangos y cores. Se compara la lista de (código, plugin, campo) y se busca un fragmento en el mensaje |
| 3 | Mutaciones (a)–(e) en una copia `git archive HEAD` | `scripts/run.sh` en el scratchpad (ver rutas) | pasa. (a) fallan 3 pruebas de forma. (b), solo en la selección de S7: falla S7 (`PLUGIN_AUSENTE` en vez de `CORE_EQUIVOCADO`). (b2), sobre el catálogo: fallan S7 y la prueba del catálogo. (c) falla S9. (d), la nueva: fallan S4 y S5 con «alternativas exactas». (e) fallan S8 y S9. El control sin mutar sale verde. `git diff --stat b4259d8...HEAD -- verificador/src/main` sale vacío |
| 4 | testFixtures usable desde otro proyecto | `testFixturesJar --rerun` + zipfile; además compilé y corrí con javac un consumidor en otro paquete (`otro.Consumidor`) | pasa. El JAR trae `SuiteCombinaciones`, `$Caso` y `CatalogoEnMemoria`. El consumidor recorre los 12 casos y cuenta 10 ejecutables. `testFixturesRuntimeClasspath` solo tiene `:verificador` y snakeyaml-engine, sin dependencias de test |
| 5 | C-26 sin romper U01-T11 | `--tests '*VerificadorPropiedades'` + grep; 2 corridas de 100 lotes × 1000 con `sampleStream`, borrando `.jqwik-database` antes de cada una | pasa en lo que se pide: 6×`tries = 1000` y 6×`checks = 1000`. Medias frente a umbral: plugin 53,18/15 (3,5x, mín 37); core 24,54/6 (4,1x, mín 10); otra carpeta 38,22/10 (3,8x, mín 24); con core registrado 38,22/10. La fila C-26 dice que se cerró. Pero ver F-01 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE ...` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL. Suma de los XML: 174 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat b4259d8...HEAD` | pasa: solo los 5 archivos previstos |

Mutaciones de U01-T11 repetidas sobre HEAD: `dp` falla `minimoYMaximo` en 4 de 4 corridas; `g2` falla `determinista` en 3 de 3; `j` falla `alternativaAplicadaAprueba` en 2 de 2. La potencia frente a esas mutaciones se mantiene.

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:198-224 y 251-256 · El nuevo peso deja casi sin muestras el catálogo de la suite en `synkra-core`
`catalogoDeLaSuite()` arma los cores con un `TreeSet`, así que `otro-core` va antes que `synkra-core`. `selecciones()` da ahora peso 8 a `nombresDeCores.get(0)`. Para el catálogo de la suite, ese primer core es `otro-core`, que solo tiene `kyc-xx`; S1 a S10 viven en `synkra-core`. El comentario del código («el primer core (el de las versiones de texto en catalogosDeTexto)») solo vale para `catalogosDeTexto`.

Medición propia, 100 lotes de 1000, dos corridas por árbol:

| métrica por 1000 | base b4259d8 | HEAD |
|---|---|---|
| catálogo de la suite con core `synkra-core` | 29,3 (mín 19) / 29,0 (mín 16) | **3,3 (mín 0)** / 3,3 (mín 0) |
| el mismo caso con `VERSION_INCOMPATIBLE` (la región de S4 y S5) | 12,8 (mín 6) / 12,9 (mín 3) | **1,4 (mín 0)** / 1,2 (mín 0) |
| catálogo de la suite con cualquier core | 79,3 | 80,4 |

El diseño (PBT-03) pide «selecciones aleatorias sobre el catálogo de la suite», y el sentido de C-26 era usar ese catálogo. Con este diff, la carpeta de `synkra-core` cae en unos 3 de cada 1000 casos, y hay lotes de 1000 en los que no aparece nunca. La bitácora no midió esto: su tabla solo trae los cuatro contadores exigidos. Esta desviación declarada (el cambio de pesos) no la acepto tal como está. Hay que cubrir el catálogo de la suite en `synkra-core` al menos como en la base, y medirlo.

### F-02 · AMARILLO · VerificadorPropiedades.java:554-561 · El contador nuevo repite al viejo y se pierde la variante con core no registrado
Ahora toda carpeta ajena tiene core registrado. Por eso «compatible solo en la carpeta de otro core registrado» da exactamente lo mismo que el contador viejo (38,22 = 38,22; en la bitácora, 37,43 = 37,43), y su aserción no añade nada. La variante con core no registrado baja de 103,7 por cada 1000 en la base a 0 en todas las corridas. La tarea dice que el caso salga «también» con core registrado. La variante con core registrado es la más fuerte, así que no bloquea, pero el orquestador debería decidir si se conserva la otra.

### F-03 · AMARILLO · SuiteCombinacionesTest.java:83 · El JAR usa `LectorDescriptor.ARCHIVO` en vez del nombre literal
La mutación `ARCHIVO = "plugin.yaml"` en src/main deja la suite en verde (15 de 15). La detectan `LectorDescriptorTest.leeDesdeJar` y `anidacionProfundaNoLanza`. La decisión 2 pide un JAR «que lleva ese YAML como `synkra-plugin.yaml`». La suite, que es el contrato que heredará U02, no fija ese nombre.

### F-04 · AMARILLO · SuiteCombinaciones.java:59-63 y 105-108 · `global` vacío significa dos cosas
Según el javadoc, «`global` vacío significa que rechaza el lector». Pero S11 y S12 también llevan `Optional.empty()`. Un consumidor que filtre por `global().isEmpty()` mezclaría S8 y S9 con S11 y S12. Además, `remiteA` nombra las unidades pero no la prueba que lo cubre (decisión 1: «con la unidad y la prueba que lo cubren»). Hoy esas pruebas no existen, así que acepto la forma; conviene dejarlo como pendiente para U02, U04 y U05.

### F-05 · AMARILLO · SuiteCombinacionesTest.java:153-174 y VerificadorPropiedades.java:198-224 · Dos huecos menores en los datos
- «Mismo contenido» compara `versionesCore` y `versionesPlugin`, pero no `descriptor(...)`. Un rango distinto en el catálogo no lo detecta esta prueba (sí S4 y S5, de forma indirecta).
- `catalogoDeLaSuite()` reconstruye el catálogo a partir de los nombres que aparecen en las selecciones. Una entrada futura del catálogo que ninguna selección nombre desaparecería sin aviso.

La desviación del origen del catálogo es aceptable: `Datos` necesita los datos en crudo para el oráculo, y hoy el contenido es idéntico a la base (4 descriptores, mismas versiones).

Otras desviaciones que juzgué: S10 en `Grupo.REPETIDO` cumple la decisión 2 del orquestador y la prueba de forma afirma los 7 grupos del PRD más `REPETIDO`. La forma de S11 y S12 es aceptable, salvo lo de F-04.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. F-03 queda cubierto hoy por `LectorDescriptorTest`. Si se mantiene, puede ir como candidata para cuando U02 consuma la suite.

## Rutas de transcripciones largas
Todas en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r1/:
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r1/logs/mut-{none,a,b,b2,c,d,e,f,f-full,g,h,dp1..dp4,g2-1..g2-3,j1,j2}.log y xml-*/
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r1/logs/medicion-{head,base}-{1,2}.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r1/scripts/{mut.py,run.sh,MedicionTmp.java}
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r1/consumidor/src/otro/Consumidor.java

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:198-224,251-256|el peso 8 al primer core (otro-core en el catálogo de la suite) baja synkra-core de 29 a 3,3 por 1000 (mín 0)
INFORME: revisiones/U01-T12/ronda-1.md
```
