# Ronda 2 — U07-C

VEREDICTO: VERDE

Volví a correr yo mismo los seis criterios sobre la punta 24651bf, cuyo último commit de código es 7246352. Todos pasan. Las cinco respuestas a la ronda 1 se sostienen con mi propia ejecución, y las dos mutaciones de F-02 que sobrevivían ahora caen. No queda ningún ROJO ni NARANJA en pie.

Comprobaciones previas:
- El hash de la tarea es 7dbe5d3e5fe3ab8bff6f7777271b4aef4cf54d52 y coincide.
- 285ef22 es ancestro de HEAD.
- El worktree quedó limpio al terminar: `git status --short` sale vacío.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | PlanPendienteTest y lectura de vuelta | el literal de CA-1 | pasa: BUILD SUCCESSFUL, `tests="9" skipped="0" failures="0" errors="0"`, casos de 0,02 a 1,27 s. El arreglo de Python es idéntico al esperado y da `(0,)` |
| 2 | Un solo bean `Clock` | el literal de CA-2 | pasa: `tests="1" skipped="0"…`. Solo aparece `plataforma/Reloj.java`, y da `otros=1` |
| 3 | JAR real: 410, 501, 409 y 404 | el literal de CA-3 con `$JAVA_HOME/bin/java`, HOME y KUBECONFIG falsos, puerto 18092 (libre antes y después) | pasa: 410 dos veces, luego 501, 409 y 404, con los mensajes exactos; la base queda en `[(71,'vencido'),(72,'pendiente'),(73,'confirmado'),(74,'pendiente')]` |
| 4 | Las ocho mutaciones muerden | mi `ca4.sh`: una copia `git archive HEAD` por mutación, corriendo PlanPendienteTest y VencimientoPlanesTest | pasa: las 8 dan BUILD FAILED, y en cada una cae la prueba de su regla (detalle abajo) |
| 5 | Suite completa y greps | el literal de CA-5 | pasa: BUILD SUCCESSFUL en 41 s; los 41 XML en `skipped="0" failures="0" errors="0"`; `registro=1 aplica=1 k8s=1 requiresNew=1 programado=1` |
| 6 | Diff acotado | el literal de CA-6 con la base 285ef22 | pasa: solo rutas permitidas, y bajo `plataforma` y `test` solo hay líneas `A`. Desde la ronda 1 (18af33e) cambian solo GuardaConfirmacion, VencimientoPlanes, PlanPendienteTest, la nueva VencimientoPlanesTest, la bitácora, la evidencia y la respuesta |

Qué cayó con cada mutación de CA-4, contando las dos clases:
- **frontera Q6:** cae `laFronteraEsVenceEnExacto`, y en VencimientoPlanesTest caen las dos pruebas.
- **persistir:** caen `laFronteraEsVenceEnExacto` y `confirmarUnPlanVencidoDa410…`.
- **solo pendiente:** el `sed` está adaptado a la forma con `Plan.PENDIENTE`, y comprobé que aplica. Cae `unPlanConfirmadoDa409YNoCambia`, y en VencimientoPlanesTest caen las dos.
- **por id:** caen `confirmarUnPlanVencidoDa410…` y `vencerSiTocaDiceSiCambioUnaFila`.
- **409:** cae `unPlanConfirmadoDa409YNoCambia`.
- **reloj:** caen 8 de 9 en PlanPendienteTest.
- **R-D13 crear:** caen 8 de 9 en PlanPendienteTest.
- **R-D13 confirmar:** caen `confirmarUnPlanVigenteDa501…` y `laFronteraEsVenceEnExacto`.

**La clase nueva no altera el arreglo de CA-1.** Después de la suite completa de CA-5, donde corren las dos clases, volví a leer `build/sqlite-pruebas/PlanPendienteTest/synkra.db`: el arreglo sigue idéntico y da `(0,)`. VencimientoPlanesTest escribe en su propia base, `build/sqlite-pruebas/VencimientoPlanesTest/synkra.db`; la leí y tiene sus 4 filas sembradas, todas `pe`, ids 1 a 4. Sus casos tardan 11 y 15 ms. Eso no indica que se hayan saltado: son UPDATE sobre SQLite con el contexto ya levantado, la base muestra que corrieron, y las mutaciones F-02 las hacen caer.

## Verificación de las respuestas a la ronda 1
- **F-01 (ROJO), cerrado.** Las cabeceras de `ca1-r2.txt` a `ca5-r2.txt` dicen «sha 7246352», con hora 20:38. Ese commit es de las 20:36, y después de él solo hay un commit, que no toca código: 24651bf trae solo bitácora, evidencia y respuesta. Mis ejecuciones dan las mismas salidas que esas transcripciones.
- **F-02 (NARANJA), cerrado.** Las dos mutaciones que sobrevivían en la ronda 1 ahora caen, cada una en su copia:
  - `== 1` cambiado a `>= 0`: da BUILD FAILED y cae `vencerSiTocaDiceSiCambioUnaFila`.
  - el barrido con `toEpochMilli() - 1`: da BUILD FAILED y cae `elBarridoVenceDesdeVenceEnExactoYSoloLosPendientes`.
  - Las dos caen solo en VencimientoPlanesTest; PlanPendienteTest sigue en 9 de 9.
  - La prueba lee el `boolean` en sus cinco variantes (vigente, en la frontera, ya vencido, confirmado e inexistente) y prueba el barrido en `vence_en − 1`, `vence_en` y `vence_en + 1`.
- **F-03 (AMARILLO), cerrado.** El SQL de `VencimientoPlanes.java:15` vuelve a construirse con `Plan.VENCIDO` y `Plan.PENDIENTE`.
- **F-04 (AMARILLO), cerrado.** Ahora hay un `case Plan.PENDIENTE -> plan` explícito y `default` lanza `IllegalStateException`.
  - Probé una mutación extra, devolver `default -> plan`, y sobrevive. No es hallazgo: el CHECK de `V4__despliegue.sql:14` limita `estado` a los tres valores, así que esa rama no se puede alcanzar.
  - El texto de la excepción no llega al usuario: `ManejadorErrores.imprevista` responde el 500 genérico. No aplica la invariante 8.
- **F-05 (AMARILLO), cerrado.** El caso `abc` (`PlanPendienteTest.java:254-255`) ya compara `codigo`, el `mensaje` exacto y `detalles` vacío, y pasa.

## Hallazgos
Ninguno nuevo de severidad ROJO, NARANJA ni AMARILLO.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-109 (programar `vencerPendientes()` en U07-E) y C-113 (pasar `RegistroServicio` al bean `Clock` en U07-D) siguen registradas.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca1-ca2.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca3.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca3.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca4.sh
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca4.txt (incluye las 8 mutaciones, las 2 de F-02 y la extra de F-04)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-C-r2/ca5.txt (incluye la relectura de la base de CA-1 tras la suite)

No escribí este informe en ningún archivo, porque mis herramientas son de solo lectura. Hay que guardarlo en `revisiones/U07-C/ronda-2.md`.

```
VEREDICTO: VERDE
INFORME: revisiones/U07-C/ronda-2.md
```
