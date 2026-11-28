# Ronda 1 — U07-F

VEREDICTO: NO-VERDE

Tarea: `git hash-object` de /home/casdav/Projects/Master/wt-orq-U04/tareas/U07-F-reversion.md da 2f97d8d3287a5f677e52caa6520b6975a993cb33, que coincide. El worktree está en 8612dd0, el último commit de código es b13084a y la base es 2d9d21f. `git diff b13084a 8612dd0 -- backend` sale vacío, así que la evidencia es del código actual. El worktree quedó limpio al terminar (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | ReversionTest y lectura de vuelta en SQLite | `./gradlew :backend:test --rerun --tests '*ReversionTest'`, los grep y el python del criterio | `BUILD SUCCESSFUL`, `tests="9" skipped="0" failures="0" errors="0"`, tiempos de 0,03 a 1,06 s por caso. Las seis salidas de Python salen idénticas a las esperadas. **Pero faltan afirmaciones de la decisión 5 (F-01)** |
| 2 | JAR real sin clúster, puerto 18095 libre | el guion del criterio literal, más `abc` y 65 con cuerpo completo | pasa: `201 201 201`, luego USUARIO_INVALIDO, 503, ORIGEN_NO_LISTO dos veces, ORIGEN_DESACTUALIZADO, COMBINACION_RECHAZADA `['plugins.kyc-pe']`, PAIS_NO_PERMITIDO y 404. Al final `(0,)` y las seis filas, con 63 todavía `aplicando` |
| 3 | Cada mutación, en su propia copia `git archive b13084a` con `git init -q` | m1 a m8 y m4b, m4c, más dos sondas mías (P1, P2) | las nueve de la tarea caen con `BUILD FAILED` y `<failure` en la prueba de su regla. **P1 sobrevive (F-02)** |
| 4 | Todo el backend y los grep de fronteras | `./gradlew :backend:test --rerun :backend:check` y los grep | pasa: todos los XML con `skipped="0" failures="0" errors="0"`. Salen `registro-interno=1`, `tablas-reg=1`, solo ConfirmacionServicio aplica o construye, `rutas=1`, `requiresNew=1`, solo TareasProgramadas, y ConfirmacionServicio no tiene ninguna línea `TipoDespliegue.` |
| 5 | Diff acotado | `git diff --stat` y `--name-status 2d9d21f...HEAD` | pasa: solo `despliegue/**`, `A ReversionTest.java` y la bitácora con su evidencia. El commit 8612dd0 solo trae ca5.txt y dos líneas de la bitácora |

## Hallazgos

### F-01 · ROJO · ReversionTest.java:354-361 y :446-448 · Dos rechazos no comparan `mensaje` y `detalles` exactos
La decisión 5 dice: «Todos los rechazos comparan `codigo`, `mensaje` exacto y `detalles` (campo y motivo) exactos». CA-1 exige «los casos y afirmaciones de la decisión 5». Dos casos no lo cumplen:
- **977 (`kyc-qq`, cl)**: solo comprueba `codigo`, `mensaje`, que haya un detalle y que su `campo` sea `plugins.kyc-qq`. El `motivo` no se compara. Para 972 sí se compara, contra `POST /api/verificaciones`, y 977 puede hacer lo mismo con `kyc-qq:1.0.0`.
- **`/abc/reversion` (400)**: solo comprueba `codigo` `SOLICITUD_INVALIDA`. Ni `mensaje` ni `detalles`. Mi ejecución del JAR da `{"codigo":"SOLICITUD_INVALIDA","mensaje":"La solicitud es inválida o su cuerpo no se puede leer","detalles":[]}`, así que el caso cabe en `esperaError(..., BAD_REQUEST, "SOLICITUD_INVALIDA", <mensaje>)`.
Los demás rechazos usan `HttpPlanes.esperaError`, que compara `detalles` con `List.of(...)` exacto, y esos están bien.

### F-02 · NARANJA · PlanServicio.java (prepararReversion) y ReversionTest · El lugar de ORIGEN_NO_LISTO antes de R-D1 y R-D3 no tiene prueba que muerda
La decisión 2 dice que «el orden es contrato». El codificador ya lo aplicó a R-D1 frente a R-D3: cambió el dato de 974 para eso. Pero ninguna prueba distingue que la comprobación de `LISTO` vaya antes que R-D1 y R-D3.

Sonda P1: inserté `paises.namespace(d.pais())` y `rechazaSiNoCompatible(verificacion.verificar(...))` justo después de `var d = origen.get();`, antes de la comprobación de `LISTO`. Salió `BUILD SUCCESSFUL`, `tests="9" failures="0"`. La transcripción está en `.../U07-F-r1/ca3-b.txt`.

La causa es que todos los orígenes no `listo`, tanto 970 y 971 en la prueba como 62 y 63 en CA-2, están en un país permitido y con una combinación compatible. Para cerrarlo basta un origen no `listo` con combinación incompatible o con país no permitido, que espere `ORIGEN_NO_LISTO`. Por ejemplo, 970 podría llevar `kyc-pe` 2.0.0 y seguir `FALLIDO`, y la salida de CA-1 no cambiaría. Tocar los datos de la decisión 5 lo decide el orquestador, igual que en F-03.

### F-03 · AMARILLO · ReversionTest.java:386 · El dato de 974 se aparta de la decisión 5
La decisión 5 fija «974 (`mx`, S2, `LISTO`)». La prueba usa `kyc-pe` 2.0.0. El motivo es real y lo verifiqué: `ResumenCatalogo.armar` también llama a `paises.namespace`, de modo que con S2, quitar R-D1 no mordería. Con mi propia m2, el caso cae con `COMBINACION_RECHAZADA`. El cambio está anotado en la bitácora y no altera la salida de CA-1. Falta que el orquestador lo ratifique.

### Sobre los puntos que declara el codificador
1. **ConfirmacionServicio sin cambios: lo acepto.** `ReservaConfirmacion.java:64` hace `TipoDespliegue.valueOf(plan.tipo().toUpperCase(Locale.ROOT)), plan.origenId()`. Mi m6, que fija `DESPLIEGUE, null` en esa línea, cae en `pe` con «expected: REVERSION but was: DESPLIEGUE». La bitácora lo anota.
2. **Dato de 974:** es F-03.
3. **Mutaciones de solo tipo o solo origen que caen con 500 por los CHECK:** aceptable. La combinada m5 cae por afirmación en `pe` y `py`, y la combinada m6 en `pe`.
4. **Commit rojo sin Co-Authored-By:** no es hallazgo. e333c8b solo trae `ReversionTest.java`, con 452 inserciones.

### Lo demás que revisé y está bien
- **Orden de comprobaciones:** usuario, 404 (con `≤ 0`), `LISTO`, R-D1 con `d.pais()`, verificar con el core, la versión y los plugins del origen, resumen contra la imagen y las ternas del origen, y al final `guardar`, que llama a `leerPais`. El orden coincide con la decisión 2.
- **Ese orden muerde:** m8 (`leer` antes de la comprobación de `LISTO`) hace caer `co`, `ar`, `mx` y `cl` por peticiones. P2 (usuario validado después) hace caer `bo`. m4c (sin sha en la terna) hace caer `pe`, `py` y `ec`.
- **Plan nuevo `pendiente` y camino normal:** la reversión crea un plan `pendiente` que se confirma por la ruta normal, sin rama propia (`ec` da 409 y `py` da 410).
- **Lectura de vuelta:** cubre `tipo` y `origen_id` en `dep_plan` (`filaPlan`), en el registro (`registro.obtener`) y en el `GET /api/despliegues/{id}`.
- **Literales:** los nuevos textos visibles están en `CodigosDespliegue` y `MotivosDespliegue`. No encontré literales nuevos en puntos de decisión.

## Tareas candidatas (defectos reales fuera de alcance)
- R-D1 se comprueba dos veces en la reversión: el paso explícito y `ResumenCatalogo.armar`. El paso explícito solo se nota por el orden de los errores. Vale una nota de diseño para U07-G; no hay defecto.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r1/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r1/ca2.txt (y el guion ca2.sh)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r1/ca3-a.txt (m1 a m5)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r1/ca3-b.txt (m6, m7, m8, m4b, m4c, P1, P2)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r1/ca4.txt

```
VEREDICTO: NO-VERDE
ROJO|ReversionTest.java:354-361, :446-448|977 y /abc/ no comparan mensaje y detalles exactos (decisión 5, CA-1)
NARANJA|PlanServicio.prepararReversion / ReversionTest|ORIGEN_NO_LISTO antes de R-D1 y R-D3 sin prueba que muerda (sonda P1 sobrevive)
AMARILLO|ReversionTest.java:386|974 lleva kyc-pe 2.0.0 en vez de S2: el orquestador debe ratificarlo
INFORME: revisiones/U07-F/ronda-1.md
```
