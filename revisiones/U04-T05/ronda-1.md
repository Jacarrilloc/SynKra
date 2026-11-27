# Ronda 1 — U04-T05

VEREDICTO: NO-VERDE

Los cuatro criterios pasan con mis propias corridas. Todas las mutaciones de la comprobación de rutas, de C-45 y de C-47 mueren, salvo una que es menor (F-03). El bloqueo está en la propiedad de disco vacío: tiene dos huecos que se comprueban con mutantes que sobreviven (F-01, NARANJA).

Datos de partida: el hash de la tarea es `a0794820b735ab61d95827942e9e98c4b8adc8d1` y coincide. HEAD es 3a09f6d. La base es origin/main en dc3f852, que es la base de fusión y la que pide CA-4. La plantilla del revisor nombra `modulo-6-rehecho`, pero `origin/modulo-6-rehecho` (7e7281d) es ancestro de origin/main, así que no cambia nada. El worktree quedó limpio (`git status --short` vacío). `revisiones/U04-T05/` existe vacío y sin rastrear.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La propiedad pasa sin caché | `./gradlew :backend:test --rerun --tests '*RutasSegurasPropiedades'` y el grep del XML | pasa: BUILD SUCCESSFUL en 4 s, `tests="2" skipped="0" failures="0" errors="0"`. Tiempos de 1,83 s y 1,03 s por 1000 intentos, así que no se saltó nada. Aceptados 31 % y 32 %, rechazados 69 % y 68 %. Semillas `-7940208518825505671` y `1092906437526745251` en el `system-out` del XML |
| 2 | Detecta una comprobación rota | mis mutantes P1, P2 y P3 en copias `git archive HEAD` del scratchpad | pasa. P1 (sin `..`) se reduce a `'../'`. P2 (sin normalizar `\`) se reduce a `'..\../'`. P3 (sin mirar enlaces) se reduce a `ENLACE 'a'` |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` y el bucle de XML | pasa: BUILD SUCCESSFUL en 16 s, 17 XML, todos con `skipped="0" failures="0" errors="0"`. RecepcionArtefactoTest tiene 67 pruebas y SubirPluginTest 52 |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 5 archivos, 643 inserciones, todos de la lista permitida. `build.gradle.kts` solo agrega jqwik 1.10.2 y no se toca `catalogo/**` de main |

La evidencia de la bitácora tiene una edad válida: `git diff 726fc75 HEAD` solo toca `bitacoras/U04-T05.md`.

## Mutaciones propias (cada una en su copia del scratchpad)
- **Comprobación de rutas, todas mueren:**
  - P4: sin `startsWith("/")`. Contraejemplo `'/'`.
  - P5: sin letra de unidad. Contraejemplo `'C:/'`.
  - P6: sin NUL. Contraejemplo `'\0/'`.
  - P8: `..` solo al principio. Contraejemplo `'META-INF/..\../'`.
  - P12: la expresión regular solo con mayúsculas. Contraejemplo `'c:/'`.
  - P13: `equals("..")`. Contraejemplo `'..\../'`.
  - P14: lo absoluto mirado sin normalizar `\`. Contraejemplo `'\/'`.
  - G1: rechazar todo. Muere en la puerta: `Percentage of 0.0 for ["aceptado"]`.
- **Generador:** H1 sube el peso de los normales a 400. La puerta de hostiles muerde: `Percentage of 20.2 for hostil`.
- **C-45, todas mueren:**
  - C02 (`if (false)` en la comparación con commons) la mata `c45_c02`.
  - C03 (las dos comparaciones como conjuntos) y C03b (solo la de la JDK) las matan `c45_c03` y `c45_c21`.
  - C21 (`distinct()`) la matan `c45_c21` y dos pruebas de c44.
  - M02 (sin el corte del límite) da tres `TimeoutException` a los 20 s y una aserción, en 1 min 6 s en vez de colgarse.
- **C-47, todas mueren:**
  - N08 (`isDirectory` sin `NOFOLLOW_LINKS`) y N13 (sin `throw existe`) las matan las dos `c47_*`.
  - N14 (`createDirectories`) la matan las dos `c47_*` y otra prueba más.
- **Sobreviven:** P7 (sin `isEmpty`), D1 y D2. Ver F-01 y F-03.

## Hallazgos
### F-01 · NARANJA · RutasSegurasPropiedades.java:186-190 y 239-250 · La propiedad de disco vacío no ve directorios ni nada fuera de `datos`
`archivos()` descarta los directorios y solo recorre `r.datos()`. Lo comprobé con dos mutantes en `recibir`, corridos con `--tests 'io.synkra.catalogo.R*'`:
- **D1:** en el `finally` del rechazo, `Files.createDirectories(temporales.resolve("residuo"))`. Todo queda en verde: la propiedad (2/0) y RecepcionArtefactoTest (67/0).
- **D2:** en vez de borrar el temporal, `Files.move(temporal, <padre de datos>)`. Todo queda en verde, y en `/tmp` aparecieron 1399 `recepcion-*.jar`. Los borré yo porque eran de mi mutante.

La tarea dice «todo JAR rechazado deja el disco vacío». Una prueba con la forma actual no detecta directorios residuales ni escrituras fuera de `synkra.data`. Dos arreglos posibles:
- afirmar el conjunto exacto de rutas que quedan, directorios incluidos (`{datos, artefactos, artefactos/.tmp}`);
- crear `datos` dentro de una caja por intento y recorrer la caja entera.

### F-02 · AMARILLO · RutasSegurasPropiedades.java:46 y 151-155 · Parte del oráculo copia la regla del código
`UNIDAD_WINDOWS` es literalmente la misma expresión que `RecepcionArtefacto.UNIDAD` (línea 43), y `replace('\\','/')` repite la línea 232. Lo demás sí es independiente: resolver, normalizar y `startsWith` es otro mecanismo distinto de `split("/").contains("..")`. Esta copia es inevitable en Linux y está declarada. El riesgo es que un punto ciego que tengan las dos expresiones no lo detectaría nadie. Basta con anotarlo en la bitácora.

### F-03 · AMARILLO · RutasSegurasPropiedades.java:158-163 · Para el oráculo, «resolver al destino mismo» cuenta como dentro
P7 (quitar `nombre.isEmpty()`) sobrevive porque `DESTINO.resolve("")` es igual a `DESTINO`. `'.'` también se acepta. No es una fuga, pero la regla del nombre vacío queda sin cubrir por la propiedad. Se arregla exigiendo un descendiente estricto, o con una prueba de ejemplo.

### F-04 · AMARILLO · RutasSegurasPropiedades.java:72-90 · El generador no cubre todo lo que el código vigila
El generador no produce el extra `UnicodePathExtraField`, así que la propiedad no ejercita la regla `unicodeDistinto`. Además, los enlaces siempre llevan el modo `0120777` y la plataforma UNIX. Nada de eso lo exige la decisión 3, así que no bloquea.

## Lo que pediste juzgar
- **Independencia del oráculo:** se mantiene en lo esencial. No llama a `motivoDeRechazo`. El matiz está en F-02.
- **Sustitución por `u`:** no esconde casos en la lectura de Linux. En UTF-8 ningún byte de un carácter no ASCII es `0x2E` ni `0x2F`, y `UnixPath` no normaliza unicode. Lo comprobé con un programa propio (`U.java`, en el scratchpad de la sesión) en locale UTF-8: los diez casos dan el mismo resultado con y sin sustitución, incluidos `．．`, `․․`, `∕..∕`, `／`, `\uFEFF..` y `é/../..`. Los equivalentes que se vuelven `..` o `/` solo son peligrosos con un extractor de Windows que use la conversión «best-fit». Va como tarea candidata, igual que la declaró el codificador.
- **Decisión 3:** se cumple:
  - están los 8 tipos;
  - los hostiles pesan el 40 % y en la corrida salieron un 44-45 % de los segmentos;
  - el bit de enlace va en un 12 % de las entradas;
  - las puertas de aceptados ≥20 %, rechazados ≥20 % y hostiles ≥30 % muerden (G1 y H1).
- **PBT-08:** se cumple: la semilla queda en el `system-out` de cada XML.
- **Desviaciones 1 a 5:** las acepto. La 3 queda respaldada porque `vistas()` comprueba el andamiaje y hay una prueba de control.

## Tareas candidatas (defectos reales fuera de alcance)
- Puntos y barras unicode (`．．`, `／`, `∕`) que un extractor de Windows con «best-fit» convierte en `..` y `/`. Iría junto a C-46 y U07.
- La rama `isInterrupted()` de C-45 (C11) no está en las notas de esta tarea ni se cubrió.

## Rutas de transcripciones largas
- `ca1.log`, `ca3.log`, `mut/` y `bin/` (`correr.sh`, `mutar.py`, `lote1.sh` a `lote3.sh`), en el scratchpad de la sesión.

```
VEREDICTO: NO-VERDE
NARANJA|RutasSegurasPropiedades.java:186-190,239-250|La propiedad de disco vacío no ve directorios ni escrituras fuera de datos (sobreviven D1 y D2)
AMARILLO|RutasSegurasPropiedades.java:46,151-155|El oráculo copia la regex de unidad y el reemplazo de \ del código
AMARILLO|RutasSegurasPropiedades.java:158-163|El oráculo cuenta «resolver al destino mismo» como dentro (sobrevive P7, sin isEmpty)
AMARILLO|RutasSegurasPropiedades.java:72-90|El generador no produce UnicodePathExtraField y fija el modo de enlace en 0120777
INFORME: revisiones/U04-T05/ronda-1.md
```
