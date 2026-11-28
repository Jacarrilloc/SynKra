# Ronda 1 — U07-C

VEREDICTO: NO-VERDE

Corrí yo los seis criterios contra la punta 16c5cf8 y todos pasan. El código es correcto, y los tres desvíos declarados se aceptan. Quedan dos cosas en pie: la evidencia de CA-1 a CA-3 en la bitácora salió de un commit anterior al código actual (ROJO), y el `boolean` que devuelve `vencerSiToca` no lo prueba nada (NARANJA). Arreglar las dos cuesta poco.

Hash de la tarea verificado: 7dbe5d3e5fe3ab8bff6f7777271b4aef4cf54d52, coincide. La base 285ef22 es ancestro de HEAD. El worktree quedó limpio después de la revisión (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | PlanPendienteTest + lectura de vuelta | literal del CA-1 (`--rerun`, grep del XML, python3 -I) | pasa: BUILD SUCCESSFUL, `tests="9" skipped="0" failures="0" errors="0"`. Los tiempos por caso van de 0,03 a 1,1 s, así que corrieron de verdad. El arreglo de Python es idéntico al esperado y da `(0,)` |
| 2 | Un solo bean `Clock` | literal del CA-2 | pasa: `tests="1" skipped="0"…`. Solo aparece `plataforma/Reloj.java` y da `otros=1` |
| 3 | JAR real, 410/501/409/404 | literal del CA-3 con `$JAVA_HOME/bin/java`, puerto 18092 libre (ss=0) y TMPDIR en mi scratchpad | pasa: 410 x2, 501, 409, 404 con los mensajes exactos y `[(71,'vencido'),(72,'pendiente'),(73,'confirmado'),(74,'pendiente')]` |
| 4 | Ocho mutaciones muerden | mi propio `ca4.sh`, una copia `git archive HEAD` por mutación | pasa: las 8 dan BUILD FAILED y cae la prueba de cada regla (frontera→`laFronteraEsVenceEnExacto`; persistir→`…Da410…` y frontera; solo pendiente y 409→`unPlanConfirmadoDa409YNoCambia`; por id→`…Da410…`; reloj y R-D13 crear→8 de 9; R-D13 confirmar→`…Vigente…Da501…` y frontera) |
| 5 | Suite completa y greps | literal del CA-5 | pasa: BUILD SUCCESSFUL en 42 s y los 40 XML en `skipped="0" failures="0" errors="0"`. `registro=1 aplica=1 k8s=1 requiresNew=1 programado=1` |
| 6 | Diff acotado | literal del CA-6 en 16c5cf8 | pasa: solo las rutas permitidas. Bajo `plataforma` y `test` hay solo líneas `A`, y no se cambia ninguna prueba existente. `bitacoras/U07-C-evidencia/` sigue el precedente de U07-A y U07-B |

Rojo primero: el commit 1dda34a trae solo las pruebas, `RelojMovible`, `CatalogoDePrueba` y las firmas de `VencimientoPlanes` que lanzan `UnsupportedOperationException`. Su XML muestra 9 de 9 y 1 de 1 cayendo. Se acepta.

## Juicio de los desvíos declarados y de los puntos pedidos
- **`@Configuration("configuracionReloj")`: aceptado.** Sin nombre, el bean de la clase `Reloj` se llamaría `reloj` y chocaría con `@Bean reloj()`. Se mantiene la firma pedida y `RelojTest` confirma un solo bean `Clock`, en UTC.
- **El UPDATE como un solo literal: aceptado como semánticamente equivalente**, aunque genera F-01 (ver abajo) y la nota AMARILLA F-03.
- **`CatalogoDePrueba`: aceptado.** La decisión 7 lo permite expresamente y `CrearPlanTest` no se toca.
- **La guarda no es `@Transactional`: confirmado** en `GuardaConfirmacion.java:25`. La mutación «persistir» lo ata: con `@Transactional` el plan `ar` sigue `pendiente` y caen dos pruebas.
- **El paso a `vencido` queda guardado tras el 410: confirmado** tanto por HTTP como en el JAR real. En las pruebas, `ar` y `uy` quedan `vencido`; en el JAR, el plan 71 queda `vencido`.

## Hallazgos

### F-01 · ROJO · bitacoras/U07-C.md (CA-1, CA-2 y CA-3) · Evidencia que no sale del código actual
Las transcripciones `ca1.txt`, `ca2.txt` y `ca3.txt` son de 6e6a6c4 (cabecera `6e6a6c4`, 20:25). Pero después, el commit 473f386 (20:26) cambió código de producción: la constante `SQL` de `VencimientoPlanes.java`. El JAR de CA-3 y la lectura de vuelta en Python de CA-1 se tomaron sobre código que ya no es el de HEAD. La bitácora dice «pruebas re-corridas en verde ese mismo commit», pero no pega comando ni salida. CA-5, de 473f386, solo cubre la parte JUnit. Es decir, ninguna evidencia de CA-3 corresponde al código actual. El cambio es equivalente (`Plan.VENCIDO` = `"vencido"`) y mis propias ejecuciones en 16c5cf8 pasan, pero según la invariante 3 la evidencia vieja es ROJO por sí sola. Arreglo: volver a correr CA-1, CA-2 y CA-3 en el sha actual y pegar esas salidas.

### F-02 · NARANJA · VencimientoPlanes.java:34 · Hay contratos de `VencimientoPlanes` que ninguna prueba ata
Lo barrí en esta ronda con dos mutaciones extra, cada una en su copia. Las dos sobreviven con `tests="9" failures="0"`:
- Cambiar `toEpochMilli(), id) == 1;` por `>= 0;` deja a `vencerSiToca` devolviendo siempre `true`. La decisión 3 dice que el método «devuelve si cambió una fila», pero ninguna prueba lee ese `boolean` y la guarda lo ignora.
- `vencerPendientes()` con `toEpochMilli() - 1`: la frontera `>=` de Q6 solo se prueba por `vencerSiToca` (caso `uy`). El barrido no tiene caso en `vence_en` exacto. Hoy no se nota porque las dos rutas comparten el literal, pero cualquier separación futura del SQL lo dejaría expuesto.

La primera es cobertura ausente de un contrato decidido, y por eso NARANJA. La segunda, sola, sería AMARILLO. Las agrupo porque son la misma clase de hueco. Transcripción en `ca4b.txt`.

### F-03 · AMARILLO · VencimientoPlanes.java:15-16 · Estados duplicados como literales SQL
`'vencido'` y `'pendiente'` vuelven a estar incrustados en el SQL, cuando antes salían de `Plan.VENCIDO` y `Plan.PENDIENTE`. No son valores nuevos y la decisión 3 escribe el SQL así, de modo que no es NARANJA. Pero el motivo («para que las mutaciones fueran literales») es débil: un `sed` podía apuntar igual a la forma concatenada, y ahora una constante de `Plan` puede desviarse del SQL sin que nada lo note.

### F-04 · AMARILLO · GuardaConfirmacion.java:37 · `default -> plan`
El caso que deja pasar es `default`, no `case Plan.PENDIENTE`. Si se añade un estado nuevo, la guarda lo dejaría pasar en silencio. Lo propuesto: un `case PENDIENTE` explícito y un `default` que lance.

### F-05 · AMARILLO · PlanPendienteTest.java:253-255 · El 400 de `abc` solo compara `codigo`
Para `/abc/` no se comprueban `mensaje` ni `detalles`. CA-1 no lo exige para el 400, pero sería una afirmación más completa.

Nota sin severidad: quitar `@Transactional` de `VencimientoPlanes` también sobrevive. Con autocommit no hay diferencia observable, así que no se puede exigir una prueba.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Las que propone el codificador (programar `vencerPendientes()` en U07-E y `RegistroServicio` al bean `Clock` en U07-D) ya están registradas como C-109 y C-113.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca2.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca3.sh y ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca4.sh y ca4.txt (8 mutaciones más 2 extra)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca4b.sh y ca4b.txt (mutaciones de F-02)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r1/ca5.txt

Mis herramientas son de solo lectura, así que este informe no lo escribí en ningún archivo: hay que guardarlo en `revisiones/U07-C/ronda-1.md`.

```
VEREDICTO: NO-VERDE
ROJO|bitacoras/U07-C.md (CA-1, CA-2, CA-3)|Evidencia tomada en 6e6a6c4, anterior al cambio de producción de 473f386
NARANJA|backend/src/main/java/io/synkra/despliegue/VencimientoPlanes.java:34|El boolean de vencerSiToca y la frontera de vencerPendientes no los ata ninguna prueba (mutaciones sobreviven)
INFORME: revisiones/U07-C/ronda-1.md
```
