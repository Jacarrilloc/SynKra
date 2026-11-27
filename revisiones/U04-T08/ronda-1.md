# Ronda 1 — U04-T08

VEREDICTO: NO-VERDE

Contexto: worktree `/home/casdav/Projects/Master/wt-m6r-U04-T08`, rama `m6r/U04-T08` en `d90758f` (es lo que reportó el codificador). La tarea tiene el hash `7934b30fc0772dbbcf43bba2532b2aafc80bcca4`, el esperado. Base `origin/main`. El worktree estaba limpio antes de revisar y sigue limpio después (`git status --short` sin salida). El puerto 18080 quedó libre (`ss -ltn | grep -c ':18080 '` da 0).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C12, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*ListarPluginsTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, la tarea test se ejecutó (`1 executed`), `tests="9" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,015 y 0,777 s, así que corrieron de verdad. Los 9 casos que pide el criterio están cubiertos |
| 2 | Los `curl` de H-05 y H-06 contra el backend arrancado | bootJar recién construido, `SYNKRA_DATA` temporal, puerto 18080, espera con `timeout 90` y una condición; mismo montaje que la bitácora | pasa: `1` y `['kyc-co', 'kyc-pe']`; los 2 cores y las 3 subidas dan 201 |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + grep de todos los XML | pasa: `BUILD SUCCESSFUL`, 19 XML, todos con `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 9 archivos, 465+ y 9-. Solo `catalogo/**` (main y test) y `bitacoras/U04-T08.md` |

Las afirmaciones del codificador, comprobadas una por una:
- **Una sola consulta con parámetros enlazados.** Cierto. `SELECT` + `LEFT JOIN cat_etiqueta` y un `EXISTS` con `?` por cada pareja (`ListadoPlugins.java:30-50`). Ningún texto del usuario entra en el SQL. No hay N+1: las etiquetas llegan en las mismas filas.
- **Inyección SQL por nombre de clave.** No la hay. La clave nunca se concatena (va como `?`) y además pasa antes por `Descriptor.NOMBRE`. `?x%27%29%20OR%201%3D1--=pe` da 422, con el campo `x') OR 1=1--`. Un valor `' OR '1'='1` da `[]`, cubierto por la prueba.
- **Aislamiento entre carpetas.** Se cumple: `WHERE p.core = ?`, y el `JOIN`/`EXISTS` va atado a `p.core`. En el backend arrancado, `otro-core?pais=pe` devuelve solo `kyc-xx` y `synkra-core?pais=pe` devuelve solo `kyc-pe`.
- **Comparación exacta (C-49).** Cierto: `cat_etiqueta.valor` no tiene `COLLATE` (V2__catalogo.sql:31-40), así que `=` compara en binario. La prueba confirma que `PE` no coincide.
- **Orden con `Version` de U01.** Cierto: `Version implements Comparable` y la prueba ordena `1.9.0` antes que `1.10.0`.
- **La ruta pasó de `RutasCores` a `RutasPlugins`; C-56 limpio.** Cierto. La prueba de U04-T03 conserva el 404 y solo cambia de nombre.
- **Clave repetida da 422.** Cierto, con `campo="pais"`.

## Hallazgos

### F-01 · NARANJA · `ListadoPlugins.java:33,47` · Un filtro con muchas parejas tumba la consulta con un 500
Cada pareja añade un `AND EXISTS (...)` al árbol de expresiones de SQLite, y SQLite tiene un tope de profundidad de 1000. No hay ningún límite al número de parejas, así que una entrada del usuario provoca un error interno:
```
GET /api/cores/synkra-core/plugins?k0=v&...&k1099=v   (1100 parejas)
-> {"codigo":"ERROR_INTERNO",...} 500
Caused by: org.sqlite.SQLiteException: [SQLITE_ERROR] ... (Expression tree is too large (maximum depth 1000))
GET ...?k0=v&...&k599=v                                (600 parejas)  -> [] 200
```
Una entrada inválida del usuario debe dar un 4xx con su motivo, no un 500. Falta un tope de parejas con un 422 que lo explique, como una constante con nombre y no un literal. Falta también la prueba de ese tope.

### F-02 · NARANJA · `ListadoPlugins.java:95`, `RutasPlugins.java:92` · Los 422 recortan a 50 detalles sin el aviso de C-19
Las dos rutas de 422 nuevas cortan con `subList(0, SubidaPlugin.MAX_DETALLES)` y no añaden el detalle final de `MotivosCatalogo.MOTIVOS_OMITIDOS` («Se omitieron %d motivos más»). Ese aviso es la regla de C-19 y ya existe implementada en `SubidaPlugin.acotar` (línea 248). Lo comprobé en el backend arrancado:
```
?K0=v&...&K59=v -> 50 detalles, el último {'campo': 'K53', 'motivo': "La clave de etiqueta 'K53' debe llevar solo minúsculas..."}
```
Hay 60 claves malas y diez desaparecen sin aviso. Las dos apariciones son copias de un mismo bloque de recorte. Lo pertinente es reutilizar `acotar` (o subirlo a un sitio común) y probar el caso de más de 50. La tercera copia, en `RutasPlugins.java:138`, es anterior a este diff y va como tarea candidata.

### F-03 · AMARILLO · `RutasPlugins.java:76-95` frente a `CatalogoServicio.java:120` · El orden 404/422 depende del tipo de error
La clave repetida se comprueba en la ruta, antes de saber si el core existe. La clave inválida se comprueba después de `obtenerCore`. Resultado en el backend arrancado:
- `no-existe/plugins?pais=pe&pais=co` da 422;
- `no-existe/plugins?Pais=pe` da 404.

No rompe ningún criterio, pero el cliente ve una prioridad distinta según el error. Convendría una sola regla, por ejemplo el 404 primero.

### F-04 · AMARILLO · `RutasPlugins.java:79` · `getParameterMap` también incluye el cuerpo de formulario
Un `GET` con `Content-Type: application/x-www-form-urlencoded` y cuerpo `pais=co`, sobre la URL `?pais=pe`, devolvió 200 con `kyc-pe`. No dio el 422 de clave repetida que tocaría si contara el cuerpo. Tomcat no analiza el cuerpo en un GET, así que hoy no hace daño. Aun así, el filtro depende de un detalle del contenedor y no de la query. Por otro lado, `?=pe`, con la clave vacía, se descarta en silencio y se devuelven todos los plugins. Es comportamiento de Tomcat, y queda anotado sin bloquear.

## Tareas candidatas (defectos reales fuera de alcance)
- `RutasPlugins.java:138` (de U04-T07): los 422 por valor de etiqueta largo también recortan a `MAX_DETALLES` sin el aviso de C-19. Es la misma clase que F-02, pero anterior a este diff.

## Rutas de transcripciones largas
- Borré los logs de CA-1, CA-2 y CA-3 del scratchpad, como pediste. La salida completa de las sondas, con la traza del 500 de F-01, quedó en el archivo de resultados que guardó la herramienta: `/home/casdav/.claude/projects/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/tool-results/b6sf2g5z4.txt`.

```
VEREDICTO: NO-VERDE
NARANJA|ListadoPlugins.java:33,47|Un filtro con más de ~1000 parejas da 500 (Expression tree is too large) en vez de 422
NARANJA|ListadoPlugins.java:95, RutasPlugins.java:92|Los 422 recortan a 50 detalles sin el aviso de omitidos de C-19
AMARILLO|RutasPlugins.java:76-95 / CatalogoServicio.java:120|El orden 404/422 depende del tipo de error de clave
AMARILLO|RutasPlugins.java:79|El filtro lee getParameterMap (también el cuerpo de formulario) y descarta la clave vacía en silencio
INFORME: revisiones/U04-T08/ronda-1.md
```
