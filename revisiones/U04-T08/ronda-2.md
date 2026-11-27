# Ronda 2 — U04-T08

VEREDICTO: NO-VERDE

Contexto: worktree `/home/casdav/Projects/Master/wt-m6r-U04-T08`, rama `m6r/U04-T08` en `41d9937` (coincide con el sha que se reportó). La tarea tiene el hash `7934b30fc0772dbbcf43bba2532b2aafc80bcca4`, que es el esperado. Base `origin/main`. El worktree estaba limpio antes de revisar y sigue limpio después (`git status --short` no da salida). El puerto 18080 quedó libre: `ss -ltn | grep -c ':18080 '` da 0.

Los cuatro criterios pasan. El veredicto es NO-VERDE por dos NARANJA, y el F-01 lo introdujo justamente el arreglo de F-04 de la ronda 1.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C12, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*ListarPluginsTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `1 executed`, `tests="13" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,010 y 0,781 s, así que corrieron de verdad. Siguen cubiertos los 9 casos del criterio |
| 2 | Los `curl` de H-05 y H-06 contra el backend arrancado | `bootJar --rerun` (jar de las 11:54:38, posterior al commit de las 11:52:36), `SYNKRA_DATA` temporal, puerto 18080, espera `timeout 90` + `until curl -sf` | pasa: `1` y `['kyc-co', 'kyc-pe']`. Los 2 cores y las 3 subidas dan 201. `otro-core?pais=pe` da `['kyc-xx']` |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + grep de todos los XML | pasa: `BUILD SUCCESSFUL`, 19 XML, todos con `skipped="0" failures="0" errors="0"` (ListarPluginsTest 13, RutasPluginsTest 28) |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 13 archivos, 695+ y 12-. Solo `catalogo/**` (main y test), `bitacoras/U04-T08.md` y `revisiones/U04-T08/`. La línea 138 de `RutasPlugins` y la visibilidad de `SubidaPlugin.acotar` entran por el arbitraje |

Las sondas de la ronda 1, relanzadas contra el jar fresco:
- **1100 parejas.** Da 422 con un solo detalle, `filtro`: «El filtro tiene más de 50 parejas clave=valor». 50 parejas dan `[]` 200 y 51 dan 422. El log no tiene ningún `SQLiteException` ni `ERROR_INTERNO`. **F-01 de la ronda 1 está resuelto.**
- **60 claves malas.** Dan 422 con el detalle único `filtro` del tope, porque la comprobación del tope va antes que la de las claves. Con 50 claves malas salen 50 detalles. Con 60 claves repetidas salen 50 detalles y el último es `('filtro', 'Se omitieron 11 motivos más')`. **F-02 está resuelto**, también en la tercera copia (`RutasPluginsTest.masDe50ValoresLargos…`).
- **Orden 404/422.** Sobre `no-existe`, todos dan 404: `?pais=pe&pais=co`, `?Pais=pe`, `?=pe` y 51 parejas. **F-03 está resuelto.**
- **Cuerpo de formulario.** Un GET a `?pais=pe` con cuerpo `pais=co` da `['kyc-pe']`. Un GET sin query y con ese cuerpo da los dos plugins, así que el cuerpo se ignora. `?=pe` da 422 con campo `""`, y `?pais=%zz` da 422 `CONSULTA_INVALIDA`. **F-04 está resuelto**, pero ver F-01 de esta ronda.

## Hallazgos

### F-01 · NARANJA · `RutasPlugins.java:97-98` · El `+` ya no se decodifica como espacio: un filtro válido devuelve `[]` en silencio
`UriUtils.decode` aplica la decodificación de porcentaje de RFC 3986 y no la de `application/x-www-form-urlencoded`. Por eso el `+` de la query queda literal. `getParameterMap` de la ronda 1, `@RequestParam` de Spring, `urllib.parse.urlencode`/`requests` y `curl -G --data-urlencode` tratan el `+` como espacio. Lo comprobé con plugins subidos con `cliente: "banco x"`, `cliente: "a+b"` y `cliente: "perú"`:
```
?cliente=banco%20x                 -> ['dec-esp'] 200
?cliente=banco+x                   -> [] 200          <- urlencode({'cliente':'banco x'}) produce esto
curl -G --data-urlencode 'cliente=banco x'  -> []
?cliente=a%2Bb                     -> ['dec-mas'] 200
?cliente=a+b                       -> ['dec-mas'] 200 <- en form-urlencoded esto significa "a b"
?cliente=per%C3%BA                 -> ['dec-utf'] 200 (UTF-8 bien)
?cliente=per%FA                    -> [] 200          (UTF-8 roto: carácter de reemplazo, sin 422)
```
`%20` y UTF-8 se decodifican bien. Con `+` cambia el comportamiento respecto a la ronda 1, y el cliente recibe un resultado erróneo con 200 en vez de un error. Los valores de etiqueta admiten espacios: R-C08 solo limita la longitud. Ninguna prueba cubre `+`, `%20` ni UTF-8 en la ruta.
- **Qué falta:** decodificar cada trozo como form-urlencoded, que pase `+` a espacio y además siga dando 422 con un `%` roto.
- **Prueba que falta:** `banco+x` y `banco%20x` encuentran `banco x`, `a%2Bb` encuentra `a+b`, y `per%C3%BA` encuentra `perú`.

### F-02 · NARANJA · `RutasPlugins.java:100,114`, `ListadoPlugins.java:86,102` · El campo `"filtro"` va como literal en cuatro puntos
Es un valor nuevo que el usuario ve: es el `campo` del contrato de error y el cliente decide con él. El diff lo repite como literal en los cuatro sitios de la clase:
- `invalido("filtro", …)`;
- dos llamadas a `acotar(…, "filtro")`;
- `new DetalleError("filtro", …)`.

El módulo ya nombra sus campos con constantes (`PARTE_ARCHIVO`, `PARTE_ETIQUETAS` y `ETIQUETAS` de `SubidaPlugin`). Va una sola constante para las cuatro apariciones, y las pruebas la referencian en vez de repetir `"filtro"`.

### F-03 · AMARILLO · `ListadoPlugins.java:102`, `ListarPluginsTest.masDe50ClavesMalas…` · El `acotar` de las claves malas no se puede alcanzar
`MAX_PAREJAS` y `MAX_DETALLES` valen los dos 50, y el tope se comprueba antes que las claves. Así que la lista de claves malas nunca pasa de 50 y `acotar` nunca recorta ahí. La prueba manda exactamente 50 claves malas y espera 50 detalles. El «más de 50» de su nombre solo lo ejercen las claves repetidas, que pasan por la ruta. No es un defecto, pero el nombre engaña y el acoplamiento entre las dos constantes no está escrito en ningún sitio.

### F-04 · AMARILLO · `RutasPlugins.java:97-98` · Un UTF-8 mal formado no da 422
Un `%FA` suelto se decodifica como U+FFFD y la respuesta es `[]` 200. Un `%zz` sí da 422. Es inofensivo, pero las dos entradas rotas no reciben el mismo trato.

## Sobre la evidencia del codificador
- El rojo de la ronda 2 (`41 tests completed, 5 failed`) cuadra con 13 + 28 pruebas. Los conteos del verde coinciden con los míos.
- CA-4 no se volvió a correr en la ronda 2, como el codificador declara. Lo corrí yo y pasa.
- CA-2 de la ronda 2 aparece resumido en una línea. Se reprodujo igual en mi corrida.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log`, `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.log`
- CA-2 y las sondas: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.out`. En esa corrida las 3 subidas de decodificación dieron 422 por un error de sangría en el YAML de mi script. Las sondas de decodificación se rehicieron en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/dec.out`.
- Scripts: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/scripts/ca2.sh`, `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/scripts/dec.sh`

```
VEREDICTO: NO-VERDE
NARANJA|RutasPlugins.java:97-98|UriUtils.decode no pasa '+' a espacio: ?cliente=banco+x devuelve [] en vez del plugin con "banco x" (regresión del arreglo de F-04, sin prueba)
NARANJA|RutasPlugins.java:100,114 / ListadoPlugins.java:86,102|El campo de error "filtro" es un literal repetido en cuatro puntos, sin constante
AMARILLO|ListadoPlugins.java:102|El acotar de claves malas es inalcanzable (MAX_PAREJAS == MAX_DETALLES) y el nombre de la prueba engaña
AMARILLO|RutasPlugins.java:97-98|Un UTF-8 mal formado (%FA) da 200 con carácter de reemplazo, no 422 como %zz
INFORME: revisiones/U04-T08/ronda-2.md
```
