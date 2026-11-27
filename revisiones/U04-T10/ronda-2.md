# Ronda 2 — U04-T10

VEREDICTO: VERDE

Estado de partida: la tarea tiene el hash `ea950b982b7d7acdc7631b1ca9887b811d415ec1` según `git hash-object`, y coincide. HEAD es `b91af05`, que coincide con el sha reportado. El worktree estaba limpio antes de la revisión y sigue limpio después. `git stash list` solo tiene el `stash@{0}` antiguo de U04-T07. La ronda 2 es un solo commit sobre `f06b71b`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C13, al menos 8 pruebas, sin saltos | `./gradlew :backend:test --rerun --tests '*ArtefactosPorHashTest'` y el `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="10" skipped="0" failures="0" errors="0"`. Cada prueba tarda entre 0,015 y 0,741 s, así que se ejecutaron de verdad. Las dos pruebas nuevas aparecen en el XML |
| 2 | `curl` de H-15 en 18080 | El arranque del CA-2 de U04-T07 (bootJar, core 201, subida, repetida 409) y después el `curl` del CA-2 | pasa: los dos hashes son iguales (`fe185956…841b`) y el hash de ceros da `404`. Cabeceras: `Content-Type: application/java-archive`, `Content-Length: 224` (igual a `tamano`), `X-Synkra-Sha256` igual al hash pedido, `Content-Disposition: attachment; filename="kyc-pe-1.3.0.jar"` |
| 3 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` | pasa: `BUILD SUCCESSFUL`, 21 XML, 289 pruebas (287 de la ronda 1 más 2 nuevas). Ningún XML tiene skipped, failures ni errors distinto de 0 |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 11 archivos. Solo toca `catalogo/**`, la prueba nueva, la bitácora y `revisiones/U04-T10/` |

## Seguimiento de la ronda 1

### F-01 (ROJO): cerrado
La raíz es ahora `SubidaPlugin.raizArtefactos(datos)`, es decir `$SYNKRA_DATA/artefactos`, con `normalize()`. La comprobación es `startsWith(raiz)`, junto con `!equals(raiz)`, la ruta no vacía y `hayEnlace`. Hay prueba nueva con `synkra.db` y `artefactos/../synkra.db` con el tamaño cuadrado. La bitácora registra el rojo (200 en vez de 500).

Volví a lanzar la sonda contra el backend en 18080, en modo WAL y con `synkra.db-wal` (119512 B) y `synkra.db-shm` presentes en disco. En cada caso edité la fila con python `sqlite3`. Donde había archivo real, cuadré `tamano` con su tamaño. Todos los casos dan **500**, con el motivo `La ruta guardada del artefacto no es válida`, sin `X-Synkra-Sha256` y sin cuerpo binario:

- `synkra.db`, `synkra.db-wal` y `synkra.db-shm`
- `artefactos/../synkra.db`, `artefactos/synkra-core/../../synkra.db` y `artefactos/synkra-core/kyc-pe/../../../synkra.db`
- `./artefactos/../synkra.db` y `../synkra.db`
- `suelto.jar` (un archivo en `$SYNKRA_DATA` fuera de `artefactos/`)
- `artefactosX/k.jar` (un hermano con el mismo prefijo de texto: `Path.startsWith` compara por componentes y no cae en la trampa del prefijo)
- `artefactos` (la propia raíz)
- la ruta absoluta de `synkra.db` y la absoluta `…/artefactos/../synkra.db`

Como control, al restaurar la ruta original vuelve a dar 200 con el JAR. La transcripción está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca2.log`.

### `SubidaPlugin` no cambia de comportamiento
El diff en `SubidaPlugin` es solo extraer un método. Antes era `this.datos.resolve("artefactos")` y ahora es `raizArtefactos(this.datos)`, que devuelve exactamente eso. No es un literal nuevo, solo se movió. Lo comprobé en ejecución:
- El arranque guardó `artefactos/synkra-core/kyc-pe/1.3.0.jar`.
- La fila tiene `ruta='artefactos/synkra-core/kyc-pe/1.3.0.jar'`, `tamano=224` y el sha correcto.
- La repetida da 409.
- Las pruebas de subida dentro de las 289 del CA-3 pasan.

### Los demás hallazgos de la ronda 1
- **F-02 (AMARILLO): cerrado** según el arbitraje del orquestador. Está documentado en la bitácora (ronda 2, «Cambio»).
- **F-03 (AMARILLO): cerrado.** La prueba se renombró a `hashMalFormadoEs404` y hay una nueva, `directorioIntermedioEnlazadoEs500`, que pasa.
- **F-04 (AMARILLO): cerrado.** Ahora se abre primero `FileChannel.open(ruta, READ, NOFOLLOW_LINKS)` y `canal.size()` se compara con `tamano`. El flujo que se sirve es ese mismo canal, y el canal se cierra si algo falla.
- **F-05 (AMARILLO): cerrado.** La bitácora dice ahora 21 XML, y `ronda-1-respuesta.md` es ya una respuesta real.

## Hallazgos

### F-06 · AMARILLO · `CatalogoApi.java` (javadoc de `abrirArtefacto`) y `ArtefactosPorHash.java:25-26` · El javadoc sigue hablando de la «raíz de datos»
Los dos comentarios dicen «ruta fuera de la raíz de datos» y «que no salga de la raíz de datos». Desde la ronda 2, la raíz es `$SYNKRA_DATA/artefactos` y no `$SYNKRA_DATA`. Es solo documentación desfasada: no bloquea.

### F-07 · AMARILLO · `ArtefactosPorHashTest.directorioIntermedioEnlazadoEs500` · La prueba no comprueba el motivo
Solo comprueba `500 ERROR_INTERNO`, no que el motivo sea `ARTEFACTO_RUTA_FUERA`. Por eso no distingue el rechazo por enlace de otro 500 cualquiera. La prueba de `synkra.db` sí comprueba el motivo. Es secundario.

## Tareas candidatas (defectos reales fuera de alcance)
- Se mantiene la de la ronda 1. Un archivo corrupto del mismo tamaño se sirve con un `X-Synkra-Sha256` falso, porque la decisión 4 no recalcula el hash. El init container de U07 debería verificar el sha256 de lo que descarga.

## Limpieza
- El puerto 18080 quedó libre al terminar (`ss -ltn`).
- `SYNKRA_DATA` y `W` del arranque se crearon con `mktemp` en `/tmp` y los borré al terminar, junto con la base, las cabeceras, el cuerpo descargado y el log de arranque. No queda ningún dato de la base en el scratchpad.
- Worktree limpio, HEAD `b91af05`.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca3.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r2-ca2.log (CA-2 y las sondas de F-01)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/scripts/r2-ca2.sh

```
VEREDICTO: VERDE
AMARILLO|CatalogoApi.java y ArtefactosPorHash.java:25-26|El javadoc sigue diciendo «raíz de datos» y la raíz es artefactos/
AMARILLO|ArtefactosPorHashTest.directorioIntermedioEnlazadoEs500|La prueba no comprueba el motivo ARTEFACTO_RUTA_FUERA
INFORME: revisiones/U04-T10/ronda-2.md
```
