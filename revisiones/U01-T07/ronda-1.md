# Ronda 1 — U01-T07

VEREDICTO: NO-VERDE

Los 7 criterios pasan con mis propias ejecuciones. Quedan tres hallazgos NARANJA en pie: un ajuste de C-10 que no se aplicó, una prueba de CA-3 que falla al azar y un hueco en la propiedad «nunca lanza». No hay ningún ROJO. Revisé el worktree `/home/user/wt-m6r-U01-T07` (HEAD 0cae570), el hash de la tarea coincide con d7baf898…, y lo dejé limpio: `git status --short` sale vacío. Solo quedan `build/`, `.gradle/` y `verificador/.jqwik-database`, que están en el gitignore.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Propiedades del descriptor | `./gradlew :verificador:test --rerun --tests '*DescriptorPropiedades'` | pasa (BUILD SUCCESSFUL) |
| 2 | 1000 intentos, ninguno descartado, semilla registrada | los dos `grep` de CA-2 | pasa: 2×`tries = 1000`, 2×`checks = 1000`, `seed = -4618225868389870252` y `seed = 4413307508908241340`, `tests="3" skipped="0" failures="0" errors="0"`; ~0,6 s por propiedad, es decir, corrieron de verdad |
| 3 | El generador cubre los textos ambiguos | `--tests '*…generadorCubreTextosAmbiguos'` | pasa. Rojo reproducido en la copia (con `textos()` en minúsculas falla en «nulo») y verde al restaurar. Además, al quitar `007/true/null` de los nombres engañosos falla en «clave 007». Pero tiene fallos al azar, ver F-02 |
| 4 | La propiedad detecta F-07 | mutación en la copia (borré `.setSchema(new CoreSchema())` de `aYaml`) y luego `git diff --stat -- verificador/src/main` en el worktree | pasa: con la mutación falla (`etiquetas={a=~}` en 26 pasos, `{007=~}`, `nombre=007`, `descripcion=True`); revertida, pasa; diff vacío en el worktree |
| 5 | `RangoVersionPropiedades` con los generadores compartidos | `--tests '*RangoVersionPropiedades'` y su `grep` | pasa: 2×`tries = 1000`, 2×`checks = 1000`, skipped 0 |
| 6 | Diseño alineado (C-09 y C-16) | los dos `grep` de CA-6 | pasa: línea 62 de PBT-08 y línea 39 de R-V3. Comprobé en la copia los tres ejemplos de R-V3: `{!!int pais: pe}` y `{pais: !!int pe}` dan DESCRIPTOR_INVALIDO; `version: !!int 1` da VERSION_INVALIDA |
| 7 | Todo el verificador | `./gradlew :verificador:check --rerun` | pasa: `:verificador:test` se ejecutó (las marcas de tiempo del XML son frescas), 3/28/2/36/16 pruebas, skipped 0 en los 5 XML |

Alcance: el diff toca solo `src/test` (3 archivos), los dos documentos de diseño y la bitácora. Nada en `src/main` ni en `build.gradle.kts`, sin semilla fija y sin `testFixtures`. Sobre los descartes: no hay `filter` ni `Assume`, y `checks = tries` en las dos propiedades. La mutación N+2 de U01-T05 (línea 53 de `RangoVersion`, `addExact(n, 2)`) la detectan las dos propiedades, así que el poder de detección no se perdió.

## Hallazgos
### F-01 · NARANJA · RangoVersionPropiedades.java (`nXIncluyeSoloSuMayor`) · El ajuste F-02 de C-10 no se aplicó
El alcance pide cerrar «los ajustes de C-10». C-10/F-02 (revisión U01-T05 ronda 1) se refiere a `nXIncluyeSoloSuMayor`: generar el mayor como `n + desplazamiento` para que la reducción llegue a `0, 1` y la detección sea más densa. El codificador aplicó el desplazamiento a `mayorQueAcotado` (rango cerrado). En `nXIncluyeSoloSuMayor`, `n` y `mayorAzar` se siguen generando por separado. Con la mutación N+2 y 5 semillas nuevas, la reducción se queda en `6,7`, `4,5`, `4,5`, `4,5` y `24,25`, y en otra corrida en `11,12`. Nunca llega a `0,1`. F-01 de C-10 sí está aplicado.

### F-02 · NARANJA · DescriptorPropiedades.java:87-90 · `generadorCubreTextosAmbiguos` falla al azar, ~1 % de las veces
`sampleStream()` usa una semilla aleatoria. Solo ~500 de los 1000 descriptores traen `descripcion`, y bool y float se buscan con 2 de las 52 grafías de `AMBIGUOS`. Repetí la misma comprobación 2000 veces en la copia y salieron **20 fallos (desc.bool=13, desc.float=8)**; en otra tanda, 1 de 300. Como la prueba corre en cada `check`, se pondrá roja sin causa más o menos una vez de cada cien.

### F-03 · NARANJA · DescriptorPropiedades.java:27-49 (`entradas`) · «Nunca lanza» no cubre la clase hostil que el lector sí protege
Mutación M2 en la copia: quité la guarda de profundidad (`String excesiva = null;`). `desdeYamlNuncaLanza` **sigue pasando**, y sin embargo `desdeYaml("a: " + "[".repeat(20000))` lanza `java.lang.StackOverflowError`, que el `catch (RuntimeException)` no atrapa. Las entradas tienen como mucho ~200–400 caracteres y una sola inserción, así que nunca generan anidación profunda, bombas de alias, varios documentos (`---`), directivas (`%`), claves complejas (`? `), `<<` ni tamaños cercanos a `MAX_BYTES`. La propiedad sí detecta que se quite el `catch` (M1: la reducción da `"nombre: a v"`, ScannerException). Probé a mano 44 entradas hostiles contra el código actual y ninguna lanza, así que hoy no hay defecto: lo que falta es cobertura.

### F-04 · AMARILLO · CA-4 · La reducción se queda a veces con varias etiquetas (lo que señaló el codificador)
Con 8 semillas nuevas, 4 corridas reducen a una sola etiqueta (`{007=~}`), 2 se quedan en 2–3 etiquetas `~`, y el resto cae en otra forma mínima (`nombre=007`, `descripcion=True`). En la corrida del codificador quedaron 4. Viene de cómo jqwik reduce mapas con claves únicas. El contraejemplo se sigue leyendo bien (`~` sin comillas) y CA-4 se cumple, así que no bloquea.

### F-05 · AMARILLO · Generadores.java:38,63-66 · Cota 50 repetida y mínimo del rango cerrado recortado sin necesidad
El tope 50 está escrito en tres sitios: `between(0, 50)`, `49 - x` y `between(0, 49)`. Convendría una constante. Además, el menor y el parche del mínimo de `>=A <B` van en `0..49`. Con eso no se genera, por ejemplo, `>=1.50.0 <2.0.0`, y basta con que el mayor sea menor que 50 para que siempre haya un máximo.

### F-06 · AMARILLO · Generadores.java:14-18 · Textos que el generador no produce
Faltan textos de más de 80 caracteres con espacios (plegado de línea del volcador), `\r`, `\u0085`, ` `, BOM, controles, `+.inf`, `.NAN`, `+12` y palabras de YAML 1.1 (`yes`, `no`, `on`). Probé la ida y vuelta de 63 textos de este tipo, en `descripcion` y como valor de etiqueta (126 casos): **0 rotos**. Hoy no esconden ningún defecto.

## Tareas candidatas (defectos reales fuera de alcance)
- `desdeYaml` produce un Motivo por cada clave desconocida, sin tope. Un documento de menos de 64 KB con 5000 claves devuelve 5000 motivos, unos 360 KB de salida. Convendría acotar o agrupar los motivos.
- `desdeYaml` mide `MAX_BYTES` en caracteres y `leer` lo mide en bytes. Por ejemplo, 20000 emojis ocupan unos 80 KB en UTF-8 y aun así pasan `desdeYaml`. Hay que aclarar el nombre o la unidad.

## Rutas de transcripciones largas
- Copia y mutaciones: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/copia
- XML de las reducciones de CA-4: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/shrink-xml-1.xml … shrink-xml-8.xml
- Mutación N+2: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t05-mut.txt
- F-02: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/flaky.txt
- F-03 (M1, M2 y el StackOverflowError): /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/m1.txt, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/m2.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/m2-sonda.txt
- Entradas hostiles: /root/.claude/projects/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/tool-results/bpcetir1t.txt
- Pares de CA-3: /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3-rojo.txt, /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3-claves.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/ca3-verde.txt

Mis herramientas son de solo lectura, así que no creé `revisiones/U01-T07/ronda-1.md`: este texto es el informe para que lo archive el orquestador.

```
VEREDICTO: NO-VERDE
NARANJA|RangoVersionPropiedades.java (nXIncluyeSoloSuMayor)|El ajuste F-02 de C-10 no se aplicó: la reducción no llega a 0,1
NARANJA|DescriptorPropiedades.java:87-90|generadorCubreTextosAmbiguos falla al azar (~1 %, 20/2000)
NARANJA|DescriptorPropiedades.java:27-49|desdeYamlNuncaLanza no detecta que se quite la guarda de profundidad (StackOverflowError)
AMARILLO|CA-4|La reducción a veces se queda con 2–4 etiquetas
AMARILLO|Generadores.java:38,63-66|Cota 50 repetida y mínimo cerrado recortado a 49
AMARILLO|Generadores.java:14-18|Faltan textos largos, CR, NEL, LS, BOM y palabras de YAML 1.1 (hoy no rompen nada)
INFORME: revisiones/U01-T07/ronda-1.md
```
