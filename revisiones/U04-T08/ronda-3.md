# Ronda 3: U04-T08

VEREDICTO: VERDE

Revisé el worktree `/home/casdav/Projects/Master/wt-m6r-U04-T08`, rama `m6r/U04-T08`, en `1317b8c`, que es el sha reportado. El hash de la tarea es `7934b30fc0772dbbcf43bba2532b2aafc80bcca4`, el esperado. La base es `origin/main`. El worktree estaba limpio antes de revisar y sigue limpio después: `git status --short` da 0 líneas. El puerto 18080 quedó libre: `ss -ltn | grep -c ':18080 '` da 0.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C12, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*ListarPluginsTest'` + grep del XML | pasa. `BUILD SUCCESSFUL`, `1 executed`, `tests="14" skipped="0" failures="0" errors="0"`. Cada caso tardó entre 0,012 y 0,8 s, así que corrieron de verdad. Siguen cubiertos los 9 casos del criterio, y hay una prueba nueva de decodificación |
| 2 | Los `curl` de H-05 y H-06 contra el backend arrancado | `bootJar --rerun`, `SYNKRA_DATA` temporal, puerto 18080, espera con `timeout 90` + `until curl -sf` | pasa. El jar es de las 11:59:49, posterior al commit de las 11:58:33. Salen `1` y `['kyc-co', 'kyc-pe']`. Los 2 cores y las 3 subidas dan 201, y `otro-core?pais=pe` da `['kyc-xx']` |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + grep de todos los XML | pasa. `BUILD SUCCESSFUL in 13s`, 19 XML, todos con `skipped="0" failures="0" errors="0"`. ListarPluginsTest tiene 14 pruebas y RutasPluginsTest 28 |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa. 15 archivos, 874 inserciones y 12 borrados. Solo toca `catalogo/**` (main y test), `bitacoras/U04-T08.md` y `revisiones/U04-T08/` |

## Hallazgos de la ronda 2, comprobados de nuevo contra el jar fresco

**F-01 (NARANJA, `+` como espacio): resuelto.** Probé con plugins subidos con `cliente: "banco x"`, `"a+b"` y `"perú"`:
```
?cliente=banco%20x   -> ['dec-esp'] 200
?cliente=banco+x     -> ['dec-esp'] 200
?cliente=a%2Bb       -> ['dec-mas'] 200
?cliente=a+b         -> [] 200        (en form-urlencoded esto es "a b", que es lo correcto)
?cliente=per%C3%BA   -> ['dec-utf'] 200
?cliente=per%FA      -> 422 ENTIDAD_NO_PROCESABLE, campo 'filtro'
?cliente=%C3         -> 422, campo 'filtro'
?cliente=%zz         -> 422, campo 'filtro'
curl -G --data-urlencode 'cliente=banco x' -> [dec-esp]
curl -G --data-urlencode 'cliente=a+b'     -> [dec-mas]
urllib.parse.urlencode de 'banco x', 'a+b' y 'perú' -> dec-esp, dec-mas y dec-utf
```
También probé más casos. `%`, `%2`, `?cl%zzave=x`, `%E2%82` (UTF-8 truncado), `%ED%A0%80` (surrogate) y `%C0%AF` (overlong) dan 422 con el campo `filtro`. La prueba nueva `laConsultaSeDecodificaComoFormularioYUnaCodificacionRotaDa422` manda la consulta cruda por socket y cubre `banco+x`, `banco%20x`, `a%2Bb`, `per%C3%BA` y las cuatro entradas rotas. Su rojo está en la bitácora: `14 tests completed, 1 failed`, y cuadra con el conteo.

**F-02 (NARANJA, literal `"filtro"`): resuelto.** `grep -rn '"filtro"' backend/src` solo encuentra la definición `ListadoPlugins.java:31 CAMPO_FILTRO = "filtro"`. Las cuatro apariciones y las pruebas usan ahora la constante.

**F-03 (AMARILLO): resuelto.** La prueba está renombrada y hay un comentario sobre el acoplamiento de `MAX_PAREJAS` con `MAX_DETALLES`.

**F-04 (AMARILLO): resuelto**, como se ve arriba.

## Sondas de la ronda 1, relanzadas contra el jar fresco
- **1100 parejas:** 422 con un solo detalle `('filtro', 'El filtro tiene más de 50 parejas clave=valor')`. 50 parejas dan `[]` 200 y 51 dan 422. El log no tiene ningún `ERROR_INTERNO` ni `SQLiteException` (el conteo da 0).
- **60 claves malas:** dan 422 con el detalle único del tope. 50 claves malas dan 50 detalles. 60 claves repetidas dan 50 detalles, y el último es `('filtro', 'Se omitieron 11 motivos más')`.
- **Orden 404/422:** sobre `no-existe`, `?pais=pe&pais=co`, `?Pais=pe`, `?=pe` y 51 parejas dan todos 404 `NO_ENCONTRADO`.
- **Cuerpo de formulario:** un GET a `?pais=pe` con cuerpo `pais=co` da `['kyc-pe']`. Sin query da los dos plugins, así que el cuerpo se ignora. Un POST con form-urlencoded da 415.
- **Bordes:** `?=pe` da 422 con campo `""`. `?pais` y `?pais=` dan `[]`, y `?&&pais=pe&&` da `['kyc-pe']`.

## Hallazgos
### F-01 · AMARILLO · `RutasPlugins.java:103` / `MotivosCatalogo.CONSULTA_INVALIDA` · El motivo de un UTF-8 roto habla de «porcentaje»
`?cliente=per%FA` y `%C3` responden «La consulta tiene una codificación de porcentaje no válida». La codificación de porcentaje es correcta y lo que falla es el UTF-8. El código y el campo son los adecuados, así que no bloquea.

### F-02 · AMARILLO · `ListarPluginsTest.laConsultaSeDecodificaComoFormularioYUnaCodificacionRotaDa422` · Las entradas rotas solo comprueban el estado 422
La prueba no lee de vuelta `codigo` ni `campo == CAMPO_FILTRO`. Mi sonda confirma que hoy salen bien, pero la prueba no lo fija.

### F-03 · AMARILLO · `ListadoPlugins.java:36` · El acoplamiento `MAX_PAREJAS == MAX_DETALLES` solo está en un comentario
Ninguna aserción falla si alguien cambia una de las dos constantes. Además, el comentario de línea queda suelto bajo el Javadoc partido de `MAX_PAREJAS`. Es cosmético.

## Sobre la evidencia del codificador
- El rojo de la ronda 3 (`14 tests completed, 1 failed`, `banco+x`) y los conteos del verde (14 y 28) coinciden con mi ejecución.
- La bitácora resume CA-2 de la ronda 3 en una línea, sin el script completo. La salida que da (`1 ['kyc-co', 'kyc-pe']`) se reprodujo igual en mi corrida.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r3/ca1.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r3/ca3.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r3/ca2.out` (CA-2 y sondas de la ronda 1)
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r3/dec.out` (sondas de decodificación)
- Scripts: `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/scripts/r3-ca2.sh` y `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/scripts/r3-dec.sh`

```
VEREDICTO: VERDE
AMARILLO|RutasPlugins.java:103|El motivo de un UTF-8 roto dice "codificación de porcentaje no válida"
AMARILLO|ListarPluginsTest.laConsultaSeDecodificaComoFormularioYUnaCodificacionRotaDa422|Las entradas rotas solo comprueban el 422, no codigo ni campo
AMARILLO|ListadoPlugins.java:36|El acoplamiento MAX_PAREJAS == MAX_DETALLES solo está en un comentario
INFORME: revisiones/U04-T08/ronda-3.md
```
