# Ronda 1 — U07-D

VEREDICTO: NO-VERDE

Todos los criterios pasan cuando los corro yo, salvo un `grep` de CA-6 que está mal escrito en la tarea. Lo que bloquea es un NARANJA (F-01): cinco reglas de la decisión 6 no tienen ninguna prueba que caiga si se rompen.

Comprobaciones previas:
- Hash de la tarea: `2d710b026e2a23ba49b60987b5997abe1ab41e80`. Coincide.
- Rama `m6r/U07-D`, punta `47fa192`. El último commit de código es `6bd8db6`.
- Padres de `ade68f6`: `208d5f9` (fusión de U07-C, cuyo segundo padre es `f9d38c9`) y `5b471bc`. Cuadra con la aclaración del orquestador.
- Corrí todo en copias hechas con `git archive` más `git init -q`, con `JAVA_HOME` de Adoptium 21, HOME y KUBECONFIG falsos, y `python3 -I`.
- El worktree quedó limpio (`git status --short` = 0) y no quedó ningún JAR corriendo.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 0 | Unión limpia y en verde | Los de CA-0 sobre `ade68f6` y `./gradlew :backend:test --rerun :backend:check` en una copia | pasa: `0 0 0 0`; `grep -cx` literal = **1**; Clock: `Reloj.java` y `RegistroServicio.java`; solo `Reloj.java:12`; BUILD SUCCESSFUL, 48 XML con 0/0/0 (las siete clases nombradas están) |
| 1 | Confirmar contra el simulador | `--rerun --tests '*ConfirmarTest'` y el Python del criterio | pasa: tests=10, skipped=0, failures=0, errors=0; las seis salidas de Python coinciden letra por letra |
| 2 | P4 | `--rerun --tests '*CarpetaEquivocadaPlanTest'` y Python | pasa: tests=3, 0/0/0; `[('co','otro-core','pendiente','[{"nombre":"kyc-pe","version":"1.3.0"}]')]` y `(0,)` |
| 3 | Sin clúster (doble y JAR real) | La prueba, y las líneas 183 a 221 de la tarea copiadas literalmente, en el puerto 18093 libre | pasa: tests=1, 0/0/0; `201 201 201`, 422 USUARIO_INVALIDO, 503, 410, 409, 422 `['plugins.kyc-pe']`, 422 PLAN_DESACTUALIZADO, 404, `[] 200`, `404`; las cinco filas esperadas y `(0,)` |
| 4 | R-D13, sin 501 | `--rerun` de PlanPendienteTest y RelojTest, Python, `grep` del 501 y diff | pasa: 9 y 1 con 0/0/0; la lista de U07-C es idéntica; `501=1`; solo cambian la línea `@Autowired guarda` y los casos `py`/`uy` |
| 5 | Rojo primero y mutaciones | Diez mutaciones mías, cada una en su copia | Las cinco que repiten o amplían la lista del codificador (M6 a M10) caen. **Cinco reglas no muerden** (F-01) |
| 6 | Suite y fronteras | `--rerun :backend:check` y los `grep` de CA-6 | BUILD SUCCESSFUL, 51 XML con 0/0/0, `FronterasModulosTest` incluida. `registro-interno=1`, `tablas-reg=1`, solo `ConfirmacionServicio.java` con `2`, `vigilancia=1`, `programado=1`, `confirmar-tx=1`. `requiresNew=0`: el criterio está mal (ver «Las tres desviaciones», punto 3); el uso real da 1 |
| 7 | Diff acotado | Las tres órdenes de CA-7 en el worktree | pasa: A para las tres pruebas y `HttpPlanes`; M solo para `PlanPendienteTest` y `SimuladorKubernetes`; `0` líneas quitadas; nada en resources, registro, plataforma, kubernetes ni catalogo. El commit `47fa192` solo trae `ca7.*` y la bitácora |

## Hallazgos

### F-01 · NARANJA · CA-5 y decisión 6 · Cinco reglas de la decisión 6 sin prueba que muerda
Cada mutación se aplicó en una copia limpia y se corrieron las tres clases (`ConfirmarTest`, `CarpetaEquivocadaPlanTest` y `ConfirmarSinClusterTest`). En las cinco el resultado fue `BUILD SUCCESSFUL`, con 14 pruebas y 0 fallos:
- **M1, paso 7, «cualquier otra RuntimeException»:** borré `fallido(plan.id(), e.getMessage() == null ? ...)` en `ConfirmacionServicio`. Ninguna prueba cubre una excepción de `aplicar` que no sea `AplicacionFallidaException`, así que el `fallido` con el nombre simple de la clase nunca se ejercita.
- **M2, paso 6.1, «la guarda vive en el WHERE»:** quité `AND vence_en > ?` del `UPDATE` de `ReservaConfirmacion` y el plan vencido se confirma igual sin que nada caiga.
- **M3, paso 6.1, 410 si venció entre medias:** cambié el 410 `PLAN_VENCIDO` de `reservar` por un 409 y nada cae. Una prueba que llame a `reserva.reservar(plan)` con el reloj movido más allá de `venceEn` cubriría M2 y M3 a la vez.
- **M4, paso 6.3, plugins del registro:** `reservar(..., List.of())` registra el despliegue sin plugins y nada cae.
- **M5, paso 6.3, etiquetas del catálogo:** con `Map.of()` en lugar de `catalogo.obtenerPlugin(...).etiquetas()` tampoco cae nada. Es un cambio de estado sin lectura de vuelta (invariante 6): ni `ConfirmarTest` ni el Python de CA-1 leen `plugins` ni etiquetas del registro, ni con `GET /api/despliegues/{id}` ni con `registro.obtener`.

Las otras cinco mutaciones caen: transacción (co), `@Transactional` en confirmar (bo), R-D8, usuario, y la versión aplicada cambiada a `9.9.9` (pe, por `leerPais`). Salida completa en `mutaciones.txt` (ruta abajo).

### F-02 · AMARILLO · bitácora, desviación 2, y `ca0.txt` · El diagnóstico del `grep -cx` es falso
- El comando literal del criterio da **1**, tanto en `bash -c` como en zsh, sobre el `application.properties` de `ade68f6`.
- El `0` de `ca0.txt` lo produjo el arnés del codificador: el patrón pasó por comillas dobles o por `eval`, y `${SYNKRA_PAISES:co,pe}` se expandió a vacío. Lo reproduje: `bash -c 'c="grep -cx synkra.paises=\${SYNKRA_PAISES:co,pe} f"; eval "$c"'` da `0`, y `echo "[synkra.paises=${SYNKRA_PAISES:co,pe}]"` da `[synkra.paises=]`.
- No es un problema de expresiones regulares: en BRE, ese `$` intermedio y las llaves son literales.
- CA-0 se cumple. Lo que hay que corregir es la bitácora, para que nadie «arregle» un criterio que está bien.

### F-03 · AMARILLO · `Plan.java:10` · Se cambió un comentario para contentar un `grep`
- Quitar `reg_despliegue` del Javadoc de `Plan` no cambia ningún comportamiento.
- Lo que importa sí se cumple: en `despliegue` no hay SQL sobre `reg_*`, y `registro-interno=1`.
- Aun así, `Plan.java` no está en la lista de archivos que «cambian» según CA-7, y es acomodar el código a un `grep` defectuoso. Para `REQUIRES_NEW`, el mismo codificador declaró el defecto y no tocó el comentario.
- Lo legítimo era hacer lo mismo aquí: declarar el defecto y correr un `grep` sobre literales SQL. Está declarado en la bitácora y no tiene riesgo, así que no bloquea. Si el orquestador lo prefiere, se revierte.

### F-04 · AMARILLO · `ConfirmarTest.java`, caso `ec` · «Exactamente tres PATCH» solo cuenta peticiones
`SIM.nuevasPeticiones()).hasSize(3)` no compara método ni ruta. Bastaría `sinConsulta(...)` contra `tresPatch("ec")`, como en `pe`, `cl` y `bo`.

### F-05 · AMARILLO · `RutasConfirmacion.java:29-30` · Literales en la respuesta
`"aplicando"` y `"/api/despliegues/"` están escritos a mano. Lo primero podría salir de `EstadoDespliegue.APLICANDO` o de una constante. No es un punto de decisión, así que no llega a NARANJA.

## Las tres desviaciones declaradas
1. **`aceptaAplicar`: legítima.**
   - `AplicarTest` de U05 ya documenta el 404 del modo CRUD ante un server-side apply.
   - El diff de `SimuladorKubernetes.java` solo añade líneas (`grep -c '^-[^-]'` = 0) y no expone tipos de Fabric8 en su API.
   - La respuesta al `PATCH` guarda el cuerpo que envía el cliente y el `GET` lo devuelve, así que `leerPais` lee lo que de verdad se aplicó. M6 lo demuestra: con la versión `9.9.9`, `pe` cae con `expected "1.3.0" but was "9.9.9"`.
   - Solo acepta la consulta exacta `fieldManager=…&force=true`. Si producción cambiara eso, las pruebas caerían, no lo esconderían.
   - Que `aceptaAplicar` y la reescritura de `fallaAplicar` llegaran después del commit rojo `79af276` es correcto: el rojo cayó por el 501.
2. **`grep -cx`: el diagnóstico es falso.** Ver F-02.
3. **`REQUIRES_NEW`: es un defecto del criterio, no del diff.**
   - La coincidencia es el comentario `RegistroServicio.java:60`, que viene de U06-A (`6296584`, anterior a la base) y está fuera de alcance.
   - Mi `grep` de uso real da `uso-real=1`: busca `Propagation.REQUIRES_NEW`, `propagation = ...REQUIRES_NEW`, `PROPAGATION_REQUIRES_NEW` y `setPropagationBehavior`.
   - `reservar` lleva `@Transactional` con la propagación por defecto, y `confirmar` no lleva ninguna anotación transaccional (`confirmar-tx=1`). C-107 se cumple.
   - Hay que corregir el criterio, aquí y en las tareas que copien ese `grep`.

## Lo que comprobé de R-D5 a R-D9
- **El orden** usuario → guarda → verificar → resumen → `disponible` → `construir` → `reservar` → `aplicar` coincide con la decisión 6.
- **R-D7 con el pool de una conexión** (`BaseDeDatos.java:48` fija `setMaximumPoolSize(1)`): `ec` da 202 y 409. Si se quita la transacción (M7), `co` deja el plan `confirmado`.
- **El 502** se da sin reintentos: `bo` comprueba exactamente los tres PATCH en orden y el contexto `("", "", "No se pudo aplicar Deployment/synkra-core")`, y el Python de CA-1 lo lee de vuelta.
- **P4** se cumple al crear y al confirmar: lo prueba la clase y también el JAR real (84 da 422).
- **La desviación de R-D9** está aprobada y documentada.

## Tareas candidatas (defectos reales fuera de alcance)
- Arreglar los `grep` de CA-0 (comillas o `-F` explícito) y de CA-6 (`REQUIRES_NEW` solo en uso real y `reg_` solo en literales SQL) en la plantilla de tareas.
- `ConfirmacionServicio`: si `actualizarResultado` lanza dentro del `catch`, se pierde la excepción original del `aplicar`. Es un borde secundario y conviene verlo con U07-E.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-D-r1/`:
- `ca0-gradle.txt` y `ca0-xml.txt`
- `cas.txt` (CA-1, 2, 4, 6 y la prueba de CA-3) y `ca6-xml.txt`
- `ca3-jar.txt` y `ca3-jar.sh`
- `mutaciones.txt` y `mut.sh`

```
VEREDICTO: NO-VERDE
NARANJA|CA-5 / ConfirmacionServicio.java y ReservaConfirmacion.java (decisión 6, pasos 6.1, 6.3 y 7)|Cinco reglas sin prueba que muerda: RuntimeException→fallido, vence_en en el WHERE, 410 dentro de reservar, plugins y etiquetas del registro sin lectura de vuelta
AMARILLO|bitacoras/U07-D.md desviación 2 y ca0.txt|El grep -cx literal da 1; el 0 salió de expandir ${…} en el arnés, no de las expresiones regulares
AMARILLO|backend/src/main/java/io/synkra/despliegue/Plan.java:10|Comentario cambiado para contentar el grep de CA-6
AMARILLO|ConfirmarTest.java caso ec|«Tres PATCH» solo cuenta peticiones, no compara método ni ruta
AMARILLO|RutasConfirmacion.java:29-30|Literales "aplicando" y "/api/despliegues/"
INFORME: revisiones/U07-D/ronda-1.md
```
