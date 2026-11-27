# Ronda 1 — U04-T10

VEREDICTO: NO-VERDE

Contexto que comprobé: el hash de la tarea es `ea950b98…` (coincide), HEAD es `365aa59`, el worktree está limpio antes y después de mi revisión, y `git stash list` solo tiene el `stash@{0}` antiguo de U04-T07. El `git stash` que el codificador usó para su rojo no dejó restos.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C13, 8 o más pruebas, sin saltos | `./gradlew :backend:test --rerun --tests '*ArtefactosPorHashTest'` y luego el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="8" skipped="0" failures="0" errors="0"`. Cada prueba tarda entre 0,015 y 0,744 s, así que se ejecutaron de verdad. La cobertura tiene un hueco, ver F-01 |
| 2 | `curl` de H-15 en 18080 | El arranque de CA-2 de U04-T07, con `SYNKRA_DATA` en el scratchpad | pasa: los dos hashes son iguales (`cc3979b2…`) y el hash de ceros da `404`. Cabeceras: `Content-Type: application/java-archive`, `Content-Length: 534` (igual a `stat`), `X-Synkra-Sha256` igual al hash pedido, `Content-Disposition: attachment; filename="kyc-pe-1.3.0.jar"`. Al terminar, el puerto 18080 quedó libre |
| 3 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` | pasa: `BUILD SUCCESSFUL`, 21 XML, 287 pruebas, ninguna con skipped, failures ni errors distinto de 0 |
| 4 | Diff acotado | `git diff --stat origin/main...m6r/U04-T10` | pasa: solo toca `catalogo/**`, la prueba nueva, la bitácora y `revisiones/U04-T10/` |

## Juicio sobre los puntos pedidos
- **No recalcular el hash y R-C13.** R-C13 pide 404, `Content-Length` y el hash en una cabecera, y la decisión 4 prohíbe expresamente recalcular. El código solo compara el tamaño, antes de abrir el flujo, y la bitácora lo dice. **Cumple.** Queda un efecto secundario, anotado como tarea candidata: un archivo corrupto del mismo tamaño se sirve con un `X-Synkra-Sha256` que no es cierto.
- **El hash en la ruta.** Se comprueba con `[0-9a-f]{64}` antes de tocar la base o el disco, y lo verifiqué yo:

  | Entrada | Resultado |
  |---|---|
  | Mayúsculas | 404 |
  | 63 caracteres | 404 |
  | `%2e%2e` | 404 |
  | `../` sin codificar (`--path-as-is`) | 404 |
  | `<hash>/` | 404 |
  | `<hash>.jar` | 404 |

  Algunas entradas dan 400 en vez de 404, ver F-02.
- **Leer fuera de `artefactos/`.** **No cumple**, ver F-01.
- **Cabeceras.** Las cuatro son correctas, verificadas en CA-2.
- **«Mismos bytes» por SQL.** Está justificado. El descriptor que va dentro del JAR lleva `nombre`, `version` y `core`, y `SubidaPlugin` exige que `core` coincida con la carpeta. Por la API, dos filas no pueden compartir bytes, así que insertar la fila por SQL es la única forma de crear el caso. La elección es determinista: el mínimo por (core, nombre, versión).

## Hallazgos

### F-01 · ROJO · `ArtefactosPorHash.java:35,47` · La raíz es `$SYNKRA_DATA` y no `artefactos/`: se puede descargar la base SQLite
`this.raiz = Path.of(datos).normalize()`, y la comprobación es `ruta.startsWith(raiz)`. En cambio, `SubidaPlugin` (líneas 63-64) define la raíz de los artefactos como `datos.resolve("artefactos")`, y `synkra.db` vive en `$SYNKRA_DATA` (`BaseDeDatos.java:15`). Lo reproduje con el backend en 18080: edité la fila con `update cat_plugin set ruta='synkra.db', tamano=<tamaño del archivo>` y el resultado fue `GET /api/artefactos/<hash>` igual a **200**, con un cuerpo que empieza por `SQLite format 3`, `Content-Length: 4096` y `filename="kyc-pe-1.3.0.jar"`. Es justo el caso del último punto del CA-1 («la ruta guardada apunta fuera de la raíz: 500 y nada servido»), si «raíz» se lee con el sentido que ya tiene en el código. `rutaFueraDeLaRaizEs500YNoSirveNada` solo prueba rutas fuera de `$SYNKRA_DATA` y por eso no lo detecta. Los mismos archivos `synkra.db-wal` y `synkra.db-shm` quedan expuestos de la misma forma.

**Qué hay que hacer:** usar `datos/artefactos` como raíz en la comprobación y añadir un caso de prueba con `ruta='synkra.db'` y el tamaño cuadrado.

Si el orquestador lee «raíz» en la decisión 3 como `$SYNKRA_DATA`, el hallazgo baja a NARANJA, pero sigue bloqueando. El orquestador pidió además que no se pueda leer nada fuera de `artefactos/`.

### F-02 · AMARILLO · decisión 1 · Algunas entradas mal formadas dan 400 HTML de Tomcat en vez de 404 `ErrorApi`
- `..%2f..%2fsynkra.db` da 400 con una página HTML de Tomcat.
- `<hash>%00` da 400.

Tomcat las rechaza antes de llegar al controlador, así que no hay fuga. Aun así, no es el «cualquier otra cosa da 404» de la decisión 1. Basta con dejarlo documentado en la bitácora.

### F-03 · AMARILLO · `ArtefactosPorHashTest.java` · El nombre de una prueba promete algo que no verifica, y falta un caso de enlace
- `hashMalFormadoEs404SinTocarLaBase` no comprueba que no se toque la base. El código sí lo cumple, porque comprueba el patrón antes de consultar.
- `hayEnlace` también rechaza un directorio intermedio que sea un enlace simbólico. Lo verifiqué a mano moviendo `artefactos/synkra-core/kyc-pe` y dejando un enlace en su lugar: da 500 con el motivo de la ruta. Pero ninguna prueba cubre ese caso, solo el enlace en el último componente.

### F-04 · AMARILLO · `ArtefactosPorHash.java:52-67` · Carrera entre comprobar y abrir el archivo (TOCTOU)
Los atributos se leen antes de `newInputStream`. Si el archivo se reemplaza entre las dos llamadas, el `Content-Length` deja de corresponder con lo que se envía. Se cierra abriendo primero el canal y comprobando el tamaño sobre el archivo ya abierto. Es secundario, porque no hay borrados en el MVP (R-C14).

### F-05 · AMARILLO · bitácora y `revisiones/` · Pequeñas imprecisiones en la evidencia y un archivo de ruido
- La bitácora dice «22 XML». Hay 21: la entrada número 22 de `test-results/test/` es el directorio `binary`.
- `revisiones/U04-T10/ronda-1-respuesta.md` («F-: sin informe del revisor…») responde a un informe que todavía no existía. Es ruido.

## Tareas candidatas (defectos reales fuera de alcance)
- Un archivo corrupto del mismo tamaño se sirve con un `X-Synkra-Sha256` falso, porque el hash no se recalcula (es lo que dice la decisión 4). El init container de U07 debería verificar el sha256 de lo que descarga y no fiarse de la cabecera.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca3.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/boot.log (CA-2 y la prueba con `synkra.db`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/robado.bin (la base SQLite tal como se descargó por la API)

```
VEREDICTO: NO-VERDE
ROJO|backend/src/main/java/io/synkra/catalogo/ArtefactosPorHash.java:35|La raíz es $SYNKRA_DATA y no artefactos/: una fila editada sirve synkra.db con 200
AMARILLO|decisión 1|Algunas entradas mal formadas dan 400 HTML de Tomcat en vez de 404 ErrorApi
AMARILLO|ArtefactosPorHashTest.java|«SinTocarLaBase» sin verificar; enlace en directorio intermedio sin prueba
AMARILLO|ArtefactosPorHash.java:52-67|Carrera entre leer atributos y abrir el flujo
AMARILLO|bitacoras/U04-T10.md|«22 XML» son 21; ronda-1-respuesta.md es ruido
INFORME: revisiones/U04-T10/ronda-1.md
```
