# Ronda 2 — U07-F

VEREDICTO: NO-VERDE

Comprobaciones previas:
- **Hash de la tarea.** `git hash-object` de /home/casdav/Projects/Master/wt-orq-U04/tareas/U07-F-reversion.md da 2f97d8d3287a5f677e52caa6520b6975a993cb33, que coincide.
- **Estado de la rama.** La punta es d9febfc. El último commit de código es d832791 y solo toca `ReversionTest.java`. Después de d832791 solo cambian la evidencia, la bitácora y la respuesta. El worktree quedó limpio al terminar (`git status --short` vacío).
- **Merge 50e419a: limpio.**
  - `git show --cc --format= 50e419a` sale vacío.
  - Sus padres son 8434aca y 1783515, que es `origin/m6r/U07-E`. La base común es 2d9d21f.
  - `cmp <(git diff 2d9d21f 8434aca) <(git diff 1783515 50e419a)` da igual: el merge no trae nada de U07-F que no estuviera ya.
  - `cmp <(git diff 2d9d21f 1783515) <(git diff 8434aca 50e419a)` da igual: el lado de U07-E entró completo y sin cambios.
- **Edad de la evidencia (r2/).** Los ficheros salen de d832791, con horas de inicio entre 22:26:17 y 22:28:47. El commit es de las 22:26:15 y la evidencia se commiteó a las 22:29:51. Las horas son coherentes.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | ReversionTest y lectura de vuelta | `./gradlew :backend:test --rerun --tests '*ReversionTest'`, los dos grep y el python del criterio, en la punta d9febfc | `BUILD SUCCESSFUL`, `tests="9" skipped="0" failures="0" errors="0"`, de 0,017 a 0,94 s por caso. Las seis salidas de Python son idénticas a las esperadas. **Pero `cl` ya no comprueba «sin peticiones» (F-04)** |
| 2 | JAR real sin clúster, puerto 18095 libre | el guion literal, con `$JAVA_HOME/bin/java`, más `abc` y 65 con el cuerpo completo | pasa: `201 201 201`, USUARIO_INVALIDO, 503, ORIGEN_NO_LISTO dos veces, ORIGEN_DESACTUALIZADO, COMBINACION_RECHAZADA `['plugins.kyc-pe']`, PAIS_NO_PERMITIDO, 404, `(0,)` y las seis filas, con 63 `aplicando` |
| 3 | Mutaciones, cada una en su copia `git archive HEAD` con `git init -q` | m1, m2, m3, m4a, m4b, m5a, m5b, m6, m7 y m8, más mis sondas P1, P3, P4, T1 y T2 | Las diez de la tarea dan `BUILD FAILED` y caen en la prueba de su regla. **P1 ahora cae.** P4 cae. **P3 sobrevive (F-04)** |
| 4 | Todo el backend y los grep | `./gradlew :backend:test --rerun :backend:check` y los grep | pasa: 60 XML, todos con `skipped="0" failures="0" errors="0"`. `registro-interno=1`, `tablas-reg=1`, solo ConfirmacionServicio, `rutas=1`, `requiresNew=1`, solo TareasProgramadas, y ninguna línea `TipoDespliegue.` en ConfirmacionServicio (usa `valueOf` en ReservaConfirmacion:64) |
| 5 | Diff acotado contra 1783515 | `git diff --stat` y `--name-status 1783515...HEAD -- ...` | pasa: solo `despliegue/**`, `A ReversionTest.java`, la bitácora, la evidencia y las revisiones. El commit d9febfc solo trae `r2/ca5.txt` y una línea de la bitácora |

## Respuestas a la ronda 1, verificadas
- **F-01, cerrado.**
  - 977 compara ahora el `motivo` exacto contra `POST /api/verificaciones` para `kyc-qq:1.0.0`, con `esperaError` y `detalle(...)`.
  - `/abc/` usa `esperaError(..., BAD_REQUEST, "SOLICITUD_INVALIDA", "La solicitud es inválida o su cuerpo no se puede leer")` sin detalles. Es el mismo cuerpo que da mi JAR.
- **F-02, cerrado.**
  - 970 lleva `kyc-pe` 2.0.0 y sigue `FALLIDO`.
  - Mi sonda P1 mete R-D1 y R-D3 antes de la comprobación de `LISTO`. Ahora da `BUILD FAILED`: cae `coElOrigenQueNoEstaListoNoSeRevierte` con `COMBINACION_RECHAZADA` en lugar de `ORIGEN_NO_LISTO`.
  - Mi sonda P4 pone R-D1 después de R-D3 y cae en `mx`.
  - La salida de CA-1 no cambia.
- **F-03, ratificado por el orquestador.** No queda en pie.

## Hallazgos

### F-04 · ROJO · ReversionTest.java:352 y :362 · En `cl`, «sin peticiones» no comprueba nada: se drena justo antes de mirar
La decisión 5 pide para `cl` «Sin filas y sin peticiones», y CA-1 exige las afirmaciones de la decisión 5. Las dos comprobaciones de `cl` hacen esto:
```java
esperaError(revertir(97x, "marta"), ...);
SIM.nuevasPeticiones();            // drena lo que hizo la reversión
sinFilasNiPeticiones("cl", 0);     // y luego afirma que no hay peticiones
```
Así, cualquier petición de la reversión al clúster se descarta antes de comprobarla.
- **Origen.** El drenaje de :352 (972) ya estaba en la ronda 1 y se me escapó. El de :362 (977) es **nuevo en d832791**. En la ronda 1, el caso 977 sí comprobaba que no hubiera peticiones; ahora `cl` no lo comprueba en ninguno de los dos orígenes. Es una regresión de esta ronda.
- **Demostración** (en `ca3.txt` y `ca3-b.txt` del scratchpad):
  - **P3**, en `prepararReversion`: `try { rechazaSiNoCompatible(verificado.veredicto()); } catch (ExcepcionApi e) { leer(d.pais()); throw e; }`. Es decir, se lee el clúster al rechazar por R-D3, lo que viola «los pasos 1 a 5 no hablan con el clúster» (R-D13). Resultado: **`BUILD SUCCESSFUL`, 9/9**.
  - **m8** (leer antes de `LISTO`): `cl` ya no cae. Caen co, ar, mx y ec. En la ronda 1 `cl` sí caía.
  - **T1** (las dos líneas `SIM.nuevasPeticiones();` de :352 y :362 quitadas, con el código real): `BUILD SUCCESSFUL`, 9/9. Los drenajes no hacen falta.
  - **T2** (P3 con los drenajes quitados): `BUILD FAILED`. Cae `cl` con «Expecting empty but was: ["GET /apis/apps/v1/namespaces/synkra-cl/deployments/synkra-core", "GET /api/v1/namespaces/synkra-cl/configmaps/synkra-core-config"]».
- **Arreglo.** Quitar esas dos líneas de drenaje. El drenaje de la línea 342, antes de la primera acción, ya basta.
- **Barrido de la clase.** Revisé todos los `SIM.nuevasPeticiones()` sin afirmación de `ReversionTest`. Los demás van antes de la acción que se mide, o justo después de un 201 en `py` y `ec`, donde la decisión 5 no pide «sin peticiones». En `co`, `ar`, `mx`, `bo` y «sin país», la comprobación es efectiva. `cl` es el único caso.

## Tareas candidatas (defectos reales fuera de alcance)
- Se mantiene la de la ronda 1: R-D1 se comprueba dos veces en la reversión (en el paso explícito y en `ResumenCatalogo.armar`). Vale una nota de diseño para U07-G.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r2/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r2/ca2.txt (guion: ca2.sh)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r2/ca3.txt (m1, m2, m4a a m8, P1, P4; los intentos de m3 y P3 de este fichero no aplicaron porque el patrón salía dos veces)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r2/ca3-b.txt (m3, P3, T1, T2; los parches están en bin/lib.py y bin/mut-*.py)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r2/ca4.txt

```
VEREDICTO: NO-VERDE
ROJO|ReversionTest.java:352, :362|cl drena SIM.nuevasPeticiones() justo antes de sinFilasNiPeticiones: «sin peticiones» no comprueba nada (sonda P3 sobrevive; regresión en 977)
INFORME: revisiones/U07-F/ronda-2.md
```
