# Ronda 1 — U01-T10

VEREDICTO: NO-VERDE

Todos los criterios de aceptación pasan con mi propia ejecución, y las cuatro mutaciones de CA-3 fallan donde deben. Lo que bloquea es un solo hallazgo NARANJA. Las pruebas no detectan una implementación que tome la primera versión compatible de la lista en vez de la más alta o la más baja. El contrato de `Catalogo` dice «sin orden garantizado», así que esa implementación es incorrecta, y la prueba «más alta y más baja de verdad» no la descubre.

Datos de la revisión:
- **Hash de la tarea:** `d04fa61825eedd3d436a8f82c580735f2ad8fd37`. Coincide en el worktree y en `origin/main`.
- **Revisión:** HEAD `795e23a`, base `1dee7b4`.
- **Worktree al terminar:** limpio. Solo queda `verificador/.jqwik-database`, que está ignorado por C-02.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas de alternativas | `./gradlew :verificador:test --rerun --tests '*AlternativasTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="14" skipped="0" failures="0" errors="0"`. Están todos los casos mínimos (S4, S5, más alta/más baja, carpeta, ninguna, aplicada aprueba S4 y S5, S1/S6/S7/S10, dos incompatibles, entrada mala) |
| 2 | Invariante C-22 | `./gradlew :verificador:test --rerun --tests '*VeredictoTest'` | pasa: 17/0/0/0. Están los 3 casos nuevos (compatible → IAE; `PLUGIN_AUSENTE` → IAE; `VERSION_INCOMPATIBLE` → válido). La fila C-22 de `CANDIDATAS.md` dice que se cerró y cómo |
| 3 | Mutaciones (a) a (d) en copia `git archive HEAD` | `bin/run.sh` en mi scratchpad (`--rerun`, AlternativasTest + VeredictoTest) | pasa: (a) y (b) fallan en `masAltaYMasBajaDeVerdad` (línea 83); (c) hace fallar 5 pruebas, entre ellas `s4AplicadaApruebaConPlugin` y `s5SoloCore`; (d) falla en `veredictoAjenoOConAlternativasSeRechaza` (línea 223). La copia sin mutar da `BUILD SUCCESSFUL`. `git diff --stat origin/main...HEAD -- verificador/src/main` muestra solo `Verificador.java` y `VeredictoPlugin.java` |
| 4 | R-V5 escribe las decisiones 1, 3 y 5 | `sed -n '/## R-V5/,/## R-V6/p' …business-rules.md` | pasa: rango del plugin **pedido**, entrada = `verificar(seleccion, catalogo)` y la invariante C-22 |
| 5 | Sin Spring ni Fabric8 | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0` |
| 6 | Verificador entero | `./gradlew :verificador:test --rerun :verificador:check` | pasa: `BUILD SUCCESSFUL`, 10 XML, todos con `skipped="0" failures="0" errors="0"` (151 pruebas) |
| 7 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 7 archivos, los permitidos. `verificar` no cambia: el diff de `Verificador.java` no tiene líneas borradas |

Sobre los tiempos (invariante 7): las suites tardan alrededor de 1 s, pero son JUnit puro sobre `CatalogoEnMemoria`, sin estado externo. Gradle reporta `1 executed` y los XML traen los conteos completos, así que no se saltó nada.

## Hallazgos

### F-01 · NARANJA · `AlternativasTest.java:76-84` (y todos los catálogos de la prueba) · «Más alta y más baja» no detecta que se tome la primera de la lista
En todos los catálogos de la prueba, la versión que se espera es además la **primera** compatible en el orden en que se cargó la lista. En `masAltaYMasBajaDeVerdad` el plugin va `[1.3.0, 2.0.0, 1.2.0]` y el core `[2.1.0, 3.0.0, 3.1.0]`.

Cambié `.max(...)` y `.min(...)` por `.findFirst()`, en los dos sitios y en cada uno por separado. Las tres variantes dan `BUILD SUCCESSFUL`: 31 pruebas en verde con una implementación incorrecta. Ver `mut-e_findFirst_*.log`.

Para confirmar que el mutante es un error real, escribí una sonda en la copia con el mismo catálogo en orden inverso:
- con el mutante: `expected … kyc-pe 1.3.0 … synkra-core 3.0.0 but was … kyc-pe 1.2.0 … synkra-core 3.1.0`;
- con el código del codificador: pasa.

Esto deja sin cubrir dos cosas:
- La decisión 2 («la más alta») y la 3 («la más baja») frente a un `Catalogo` sin orden, que es lo que permite el javadoc de `Catalogo`.
- El determinismo: ninguna prueba cambia el orden de las listas.

Lo que hace falta: que el caso «más alta y más baja» ponga la respuesta esperada en una posición que no sea la primera compatible (por ejemplo, en orden inverso o mezclado), para el plugin y para el core.

### F-02 · AMARILLO · `Verificador.java` (`altaDelPlugin`) · `descriptor` nulo da un NPE sin mensaje
`verificar` protege `Catalogo.descriptor` contra `null` con un mensaje explícito. `altaDelPlugin` lo llama para otras versiones y, con un catálogo defectuoso, lanzaría un NPE sin mensaje. Lo mismo pasa si `versionesPlugin` o `versionesCore` devuelven `null`. Es solo coherencia.

### F-03 · AMARILLO · `business-rules.md` R-V5 · «plugin elegido» y «plugin pedido» conviven
La viñeta original sigue diciendo «del `coreCompatible` del plugin **elegido**», y la nueva dice «**pedido**». Dejar una sola palabra quita la ambigüedad.

### F-04 · AMARILLO · `bitacoras/U01-T10.md` · evidencia incompleta (desviaciones 4 y CA-3)
- La salida del `sed` de CA-4 no está pegada en la bitácora; la reproduje yo y pasa.
- CA-3 pide la vuelta a verde de cada mutación, y la bitácora trae una sola vuelta a verde al final.

No bloquea, porque reproduje las dos cosas yo mismo.

## Desviaciones declaradas
1. **Mensaje de «ninguna» = original + sufijo:** aceptada. Cumple la decisión 4: mismo código, `plugin` y `campo`, y el mensaje termina en la frase pedida. Además nombra plugin, versión, rango y versión pedida (`"El plugin 'kyc-pe' 2.0.0 requiere … 3.x y se pidió 2.1.0; no hay una versión compatible registrada"`). Las mutaciones h (solo el sufijo), i (`campo` vacío) y q (motivo sin cambiar) las detecta `ningunaReemplazaElMotivo`.
2. **Entrada ajena en un solo test:** aceptada, AMARILLO menor. Si falla la primera aserción, las siguientes no se ven. Aun así, mi mutación l (comparar solo `global`, tamaño y ausencia de alternativas) la detecta la tercera aserción, «otro catálogo» (línea 230), de forma independiente. La decisión 1 sí rechaza un veredicto de otro catálogo.
3. **Pruebas extra:** aceptada. Están dentro de los dos archivos de prueba permitidos y suben la cobertura: descriptor fantasma o de otro core, «solo plugin», selección vacía.
4. **Sin salida del sed de CA-4:** ver F-04.
5. **No creó `revisiones/U01-T10/`:** correcto. Según `loop/orquestador.md:169` ese archivo lo escribe el orquestador.

## Búsqueda adversarial: mutaciones mías (todas en la copia, cada una revertida)
| Mutación | Resultado |
|---|---|
| findFirst en plugin, en core, o en los dos | **sobrevive** → F-01 |
| Core calculado con el rango de la alternativa de plugin (rango equivocado) | detectada (5 pruebas) |
| Sin comprobar `core == seleccion.core()` / sin exigir que el descriptor exista | detectadas (`versionListadaSinDescriptorONoDelCoreNoSeSugiere`) |
| Orden core y luego plugin | detectada (5 pruebas) |
| Decisión 1 solo mira si ya trae alternativas | detectada |
| C-22 solo rechaza si `compatible` | detectada (`VeredictoTest:86`) |
| Se pierden los motivos globales | detectada (`sinIncompatiblesSaleIgual`, S10) |
| «Ninguna» cuando falta solo una de las dos | detectada (4 pruebas) |
| `versionesCore(pedido.core())` / `versionesPlugin(pedido.core(), …)` | sobreviven, pero son **equivalentes**: con `VERSION_INCOMPATIBLE`, `pedido.core() == seleccion.core()` |

Revisé también otros puntos por lectura del código:
- **Inmutabilidad:** `Veredicto.de` y `VeredictoPlugin` usan `List.copyOf`.
- **`equals` del veredicto resultante:** lo ejercita `sinIncompatiblesSaleIgual`.
- **Selección vacía:** cubierta.
- **Plugins sin `VERSION_INCOMPATIBLE`:** salen sin cambios.
- **Alternativas para otros códigos:** imposibles, por la invariante C-22.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. El determinismo frente al orden del catálogo lo puede cubrir también la propiedad de U01-T11, pero el caso de ejemplo de F-01 pertenece a esta tarea.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r1/ca1.log, ca2.log, ca6.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r1/mutaciones.txt y mut-*.log (una por mutación)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r1/sonda-mutado.log, sonda-original.log
- Scripts: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r1/bin/mut.py, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t10r1/bin/run.sh

```
VEREDICTO: NO-VERDE
NARANJA|AlternativasTest.java:76-84|«Más alta y más baja» pasa con findFirst: el orden de la lista coincide con la respuesta esperada
INFORME: revisiones/U01-T10/ronda-1.md
```
