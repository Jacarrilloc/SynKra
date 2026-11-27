# Ronda 2 — U01-T12

VEREDICTO: NO-VERDE

Comprobé el hash de la tarea: `git hash-object tareas/U01-T12-suite-combinaciones.md` da 4dffdf5dd28d5f5ed0241de93a91e1f0fb5a0311, el esperado. Worktree en HEAD 28089da, base b4259d8. El código no cambia entre e44926a y HEAD: después solo se tocaron la bitácora, la respuesta y CANDIDATAS.md. La evidencia de la bitácora es posterior al commit del código, así que pudo salir de este diff. Al terminar, `git status --short` sale vacío. Solo quedan cachés ignoradas (`.gradle/`, `build/`, `verificador/build/`, `verificador/.jqwik-database`).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Suite verde, 10 parametrizadas con su id | `./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'` + grep del XML | pasa: `tests="15" skipped="0" failures="0" errors="0"`. Hay 10 testcase, de `"S1"` a `"S10"`, y 5 pruebas de forma |
| 2 | Resultado exacto del diseño | el mismo, más lectura del diff 9bca3f3..HEAD | pasa. Los datos de S1 a S12 no cambiaron. Se compara (código, plugin, campo) y un fragmento del mensaje |
| 3 | Mutaciones (a) a (e), con (d) según la decisión vigente («sin la alternativa de core»), en una copia `git archive HEAD` | `scripts/run.sh <m> '*SuiteCombinacionesTest'` | pasa. Control sin mutar: verde. (a): fallan 3 pruebas de forma. (b), selección de S7: falla S7 (`PLUGIN_AUSENTE`). (b2), catálogo: fallan S7 y «mismo contenido». (c): falla S9 («se esperaba un rechazo»). (d): fallan S4 y S5 por «alternativas exactas». (e): fallan S8 (NPE `coreCompatible`) y S9. `git diff --stat b4259d8...HEAD -- verificador/src/main` sale vacío |
| 4 | Suite usable desde otro proyecto | `testFixturesJar --rerun` + zipfile | pasa. El JAR trae `SuiteCombinaciones`, `$Caso`, `$Origen` y `CatalogoEnMemoria` |
| 5 | C-26 sin romper U01-T11 | `--tests '*VerificadorPropiedades'` + grep; además 2 corridas de 100 lotes de 1000 con `sampleStream` por árbol, borrando `.jqwik-database` antes de cada una | pasa: BUILD SUCCESSFUL (28,9 s), 6 × `tries = 1000`, 6 × `checks = 1000`. Mis medias: plugin 53,5 y 52,6 (umbral 15); core 25,6 y 24,4 (umbral 6); otra carpeta 39,2 y 39,2 (umbral 10); registrado 30,4 y 30,4 (umbral 8). Todas pasan de 3x. La fila C-26 dice que se cerró. Ver F-01 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE ...` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL. Suma de los XML: 175 pruebas, 0 saltadas, 0 fallos, 0 errores |
| 8 | Diff acotado | `git diff --stat b4259d8...HEAD` | pasa: CANDIDATAS.md, la bitácora, `revisiones/U01-T12/` (2 archivos), SuiteCombinaciones.java (testFixtures), SuiteCombinacionesTest.java y VerificadorPropiedades.java (test). Nada más |

**F-01 de la ronda 1 (cerrado).** Medición con mis propios contadores (`X suite: …`), independientes de los del codificador:

| por cada 1000 | base, corrida 1 / 2 | HEAD, corrida 1 / 2 |
|---|---|---|
| catálogo de la suite con core `synkra-core` | 44,5 (mín 23) / 28,5 (mín 16) | **71,3 (mín 53) / 70,5 (mín 47)** |
| lo mismo con `VERSION_INCOMPATIBLE` (región de S4 y S5) | 18,7 (mín 7) / 12,7 (mín 7) | **29,7 (mín 14) / 29,6 (mín 12)** |
| catálogo de la suite con cualquier core | 78,8 / 79,5 | 80,6 / 79,9 |

La base varía de una corrida a otra porque el orden de `Map.of` depende de la JVM. HEAD es estable porque usa un `principal` explícito. Las aserciones nuevas (≥ 20 y ≥ 8) están a 3,5x y 3,7x de su media. Mis contadores coinciden exactamente con los del codificador.

**F-03 (cerrado).** La mutación `ARCHIVO = "plugin.yaml"` en src/main ahora hace fallar S8 y S9 por `leer(jar)`, con `DESCRIPTOR_SIN_ARCHIVO`.

**Mutaciones de U01-T11 sobre HEAD, en una copia aparte:** `g2` hace fallar `determinista`, `j` hace fallar `alternativaAplicadaAprueba` y `generadorCubreLosCodigos`, y `dp` hace fallar `minimoYMaximo`. La suite conserva su potencia.

## Hallazgos

### F-01 · NARANJA · VerificadorPropiedades.java:186 y 608 · El caso «NO registrado» es una puerta intermitente unas 200 veces más frágil que cualquier otra
El caso entra en `esperados`, así que `containsAll` exige al menos 1 aparición en 1000. Su media es de 8,80 y 8,75 en mis corridas, y 8,74 en la bitácora; el mínimo es 2. Con semilla aleatoria en cada corrida y un modelo Poisson, P(0) = e^-8,75 ≈ 1,6·10⁻⁴, es decir, **1 corrida roja espuria de cada ~6 250**. Las demás puertas, con sus umbrales: registrado ≥ 8 da 1 de cada 2,2 millones; suite en VERSION_INCOMPATIBLE ≥ 8, 1 de cada 1,1 millones; core con texto ≥ 6, 1 de cada 1,2 millones; el resto está por debajo de 10⁻⁸. A lo largo de 1000 corridas de CI, la probabilidad de al menos un rojo falso es ~15 %. La tarea pide «mantener la estabilidad de U01-T11». Ese peso (1 catálogo de cada 4 sin `core-b`) no es aceptable para una aserción que bloquea. Opciones, a elegir por el orquestador:
- (i) Peso 1:1. Por la medición, NO registrado sube a ~17,5 (P(0) ≈ 2,5·10⁻⁸) y registrado baja a ~20. Su umbral tendría que pasar a 6 para conservar 3x.
- (ii) Sacar «NO registrado» de `esperados` y dejarlo solo como medición. La variante no era un CA; venía de F-02, que era AMARILLO.

**Las otras dos desviaciones las acepto:**
- El umbral de «registrado» baja de 10 a 8. Es un contador nuevo de esta tarea, no de U01-T11, y los tres umbrales de U01-T11 (15, 6, 10) siguen intactos. Queda a 3,8x de su media y su P(fallo) es 4,5·10⁻⁷. Con 10 quedaría a 3,0x y su P(fallo) sería 6·10⁻⁶.
- `core-b` queda registrado en 3 de cada 4 catálogos. Es coherente con C-26, salvo por la frecuencia de la variante sin registrar que acabo de describir.

### F-02 · AMARILLO · SuiteCombinaciones.java:73-92 · `origen` impide solo la mitad de las contradicciones
El javadoc es claro, y la regla «`global` presente ⇔ VEREDICTO» se hace cumplir. Lo comprobé con un consumidor externo (`contra/src/otro/Contra.java`): rechaza «VEREDICTO sin global» y «LECTOR con global». Pero estos casos se construyen sin error:
- FUERA_DE_U01 con motivos y alternativas;
- LECTOR con alternativas;
- LECTOR sin ningún motivo, un rechazo sin causa;
- APROBADA con motivos;
- `Caso` con `Entrada.FueraDeU01` y origen VEREDICTO;
- `Caso` con `PorDescriptor` y origen FUERA_DE_U01.

`origen` duplica la variante de `Entrada` y nadie cruza las dos. Hoy los datos son correctos y la prueba de forma los fija: la mutación `origen` en S8 hace fallar S8 y la prueba de forma. No bloquea, pero el contrato público que heredan U02 y U07 sigue admitiendo esperados incoherentes. Hay dos salidas: validar en `Caso` que la entrada y el origen concuerdan, o derivar `origen` de la entrada.

### F-03 · AMARILLO · VerificadorPropiedades.java:206-230 y 586-596 · La segunda mitad de F-05 de la ronda 1 sigue abierta, aunque la respuesta dice «Corregido»
`catalogoDeLaSuiteCubreTodoElCatalogo` compara contra un conjunto escrito a mano, no contra `SuiteCombinaciones.catalogo()`. Mutación `cat5`: añado `kyc-ar 1.0.0` en `synkra-core` al catálogo, sin nombrarlo en ninguna selección. Con eso, la prueba pasa (1/1) y la suite también (15/15). La entrada desaparece de `Datos` sin aviso, que es justo el hueco que se señaló. La primera mitad sí quedó cerrada: la mutación `nodet`, que cambia el rango de `kyc-pe 1.3.0` al azar entre llamadas, hace fallar «mismo contenido» en 2 de 3 corridas.

### F-04 · AMARILLO · VerificadorPropiedades.java:199-204 · Javadoc huérfano
El javadoc de `catalogoDeLaSuite()` quedó encima de `CORE_DE_LA_SUITE`, que ya tiene el suyo. Hay dos `/** */` seguidos, y el método se queda sin documentación.

### F-05 · AMARILLO · CANDIDATAS.md (fila C-26) · La fila dice «`core-b` con una versión registrada»
Ahora `core-b` está registrado solo en 3 de cada 4 catálogos. Además, en el catálogo de la suite, la elección de `otro-core` como core baja de 28-45 a 3,0-3,3 por cada 1000 (mín 0). Ningún caso de S1 a S10 elige `otro-core`, así que lo dejo como observación.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
Todas en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/:
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/logs/ca1.log, ca5.log, ca7.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/logs/mut-{none,a,b,b2,c,d,e,f,origen,cat5,cat5-suite,nodet1..3,g2-1,j-1,dp-1}.log y xml-*/
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/logs/medicion-{head,base}-{1,2}.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/scripts/{mut.py,run.sh,run2.sh}
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t12r2/contra/src/otro/Contra.java

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorPropiedades.java:186,608|«NO registrado» en esperados con media 8,75/1000: P(0) ≈ 1,6·10⁻⁴, prueba intermitente 1 de cada ~6 250 corridas
INFORME: revisiones/U01-T12/ronda-2.md
```
