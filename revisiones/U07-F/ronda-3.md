# Ronda 3 — U07-F

VEREDICTO: VERDE

Volví a correr yo mismo los cinco criterios. Los cinco pasan. F-04 está cerrado y no encontré hallazgos nuevos.

Comprobaciones previas:
- **Hash de la tarea.** `git hash-object` de /home/casdav/Projects/Master/wt-orq-U04/tareas/U07-F-reversion.md da 2f97d8d3287a5f677e52caa6520b6975a993cb33, que coincide.
- **Estado de la rama.**
  - La punta es b923f41.
  - El último commit de código es 8cd0286 (22:37:47). Solo cambia `ReversionTest.java`: quita las dos líneas `SIM.nuevasPeticiones();` de `cl`.
  - Después de 8cd0286, `git diff 8cd0286 HEAD -- backend` sale vacío.
  - El commit b923f41 solo trae `r3/ca5.txt` y una línea de la bitácora.
  - El worktree quedó limpio al terminar (`git status --short` vacío).
  - En `git stash list` hay una entrada `stash@{0}` de la rama m6r/U04-T07 (16:13Z). No es de esta tarea ni la toqué; la menciono solo para que conste.
- **CA-2 sin rehacer.** `git diff d832791 8cd0286 -- backend/src/main` da 0 bytes, así que `r2/ca2.txt` sigue vigente. Aun así, lo volví a correr con el JAR (resultado abajo).
- **Edad de la evidencia de r3/.**
  - ca3 empieza a las 22:37:49 sobre 8cd0286, ca1 a las 22:40:15 y ca4 a las 22:40:24.
  - El commit de la evidencia es de las 22:41:23 y ca5 de las 22:41:25, con el árbol limpio.
  - Las horas son coherentes: esta evidencia sí pudo salir del diff actual.
- **Merge 50e419a.** `git show --cc` sigue vacío, igual que en la ronda 2.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | ReversionTest y lectura de vuelta | `./gradlew :backend:test --rerun --tests '*ReversionTest'`, los dos grep y el Python literal, en b923f41 | pasa: `BUILD SUCCESSFUL`, `tests="9" skipped="0" failures="0" errors="0"`. Cada caso tarda de 0,017 a 1,15 s: los que solo rechazan son rápidos y los que arrancan contexto o aplican son más lentos. Las seis salidas de Python son idénticas a las esperadas |
| 2 | JAR real sin clúster, puerto 18095 libre | el guion literal con `$JAVA_HOME/bin/java`, más `abc` y 65 con el cuerpo completo | pasa: las respuestas salen en el orden esperado (abajo) |
| 3 | Mutaciones, cada una en su copia `git archive HEAD` con `git init -q` | las 10 de la tarea (m1, m2, m3, m4a, m4b, m5a, m5b, m6, m7, m8), mis sondas P1, P3 y P4 y dos sondas nuevas, P5 y T3 | pasa: las 10 dan `BUILD FAILED` y cada una cae en la prueba de su regla. Las sondas, en la sección de F-04 |
| 4 | Todo el backend y los grep | `./gradlew :backend:test --rerun :backend:check` y los grep | pasa (detalle abajo) |
| 5 | Diff acotado contra 1783515 | `git diff --stat` y `--name-status 1783515...HEAD -- ...` | pasa: la segunda orden solo da `A backend/src/test/java/io/synkra/despliegue/ReversionTest.java`, y no hay rutas fuera de `despliegue/**`, `ReversionTest`, `bitacoras/U07-F*` y `revisiones/U07-F/` |

Detalle de CA-2, en orden:
- `201 201 201`;
- `422 USUARIO_INVALIDO`;
- `503 KUBERNETES_NO_DISPONIBLE`;
- `ORIGEN_NO_LISTO` dos veces;
- `ORIGEN_DESACTUALIZADO`;
- `COMBINACION_RECHAZADA ['plugins.kyc-pe']`;
- `PAIS_NO_PERMITIDO ['pais']`;
- `404 NO_ENCONTRADO`;
- `(0,)` y las seis filas, con 63 todavía `aplicando`.

Detalle de CA-4:
- `BUILD SUCCESSFUL`. Hay 60 XML y ninguno tiene `skipped`, `failures` o `errors` distinto de 0.
- `registro-interno=1` y `tablas-reg=1`.
- `aplicar` o `construir` solo aparecen en `ConfirmacionServicio.java`.
- `rutas=1` y `requiresNew=1`; `@Scheduled` solo aparece en `TareasProgramadas.java`.
- `ConfirmacionServicio` no tiene ninguna línea `TipoDespliegue.`. El tipo se mapea con `TipoDespliegue.valueOf(plan.tipo().toUpperCase(Locale.ROOT)), plan.origenId()` en `ReservaConfirmacion.java:64`, que es el caso que admite el criterio («o no hay ninguna si usa `valueOf`»).

## Respuesta a la ronda 2, verificada
- **F-04, cerrado.** Las sondas que pidió el orquestador, cada una en su copia de b923f41:
  - **P3** (leer el clúster al rechazar por R-D3): `BUILD FAILED`. Cae `clUnPlugin…` con «Expecting empty but was: ["GET /apis/apps/v1/namespaces/synkra-cl/deployments/synkra-core", "GET …/configmaps/synkra-core-config"]». En la ronda 2 esta sonda sobrevivía.
  - **T1** (el código real sin los drenajes) es ahora la propia HEAD: 9/9 en CA-1.
  - **T2** (P3 con los drenajes quitados) es ahora P3 sobre HEAD, y cae.
  - **m8** (leer antes de `LISTO`): caen 5 pruebas, `co`, `ar`, `mx`, `cl` y `ec`. **`cl` vuelve a caer.**
  - **P5** (nueva): lee el clúster solo cuando se rechaza 977 (`kyc-qq`). Cae `cl`, así que el segundo bloque de `cl` también comprueba de verdad, no solo el primero.
  - **T3** (control nuevo): P3 con los dos drenajes vueltos a poner da `BUILD SUCCESSFUL`. Eso confirma que lo que tumba a P3 es precisamente haber quitado esos drenajes.
  - P1 y P4 siguen cayendo: P1 en `co` y `ec`, P4 en `mx`.
- **Barrido de la clase.** Revisé todos los `SIM.nuevasPeticiones()` sin afirmación de `ReversionTest.java` en b923f41. Ninguno queda entre una acción y la afirmación que la mide:
  - **Ayudantes.** `confirma` drena después del 202 y `cierraListo` después de `revisar()`. Ninguno de los dos va seguido de una afirmación sobre peticiones.
  - **`pe`.** Drena tras el `fallido` y antes de la reversión. La lectura HTTP del registro entre medias no habla con el simulador. Después de la reversión y de la confirmación, las afirmaciones consumen las peticiones directamente.
  - **`py` y `ec`.** Drenan justo después del 201, que hace `GET` legítimos, y antes de confirmar. La confirmación se mide con `isEmpty()`. La decisión 5 no pide «sin peticiones» en la creación de estos dos casos.
  - **`ec`, al final.** Drena después del último `revisar()` y no hay nada más que medir.
  - **`co`, `cl`, `ar`, `mx`, `bo` y «sin país».** Un único drenaje de preparación antes de la primera acción. En los bucles de `co` y `ar`, cada `sinFilasNiPeticiones` consume las peticiones de su propia iteración.
  - **`cl`.** Entre la reversión de 972 y su afirmación solo está `POST /api/verificaciones`, que no habla con el clúster.

## Hallazgos
Ninguno en pie (ni ROJO, ni NARANJA, ni AMARILLO nuevo).

## Tareas candidatas (defectos reales fuera de alcance)
- Se mantiene la de las rondas 1 y 2: R-D1 se comprueba dos veces en la reversión, en el paso explícito y dentro de `ResumenCatalogo.armar`. Vale una nota de diseño para U07-G.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r3/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r3/ca2.txt (guion: ca2.sh en la misma carpeta)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r3/ca3.txt (las 15 mutaciones y sondas, con su texto viejo y nuevo; parches en bin/muts.py, ejecución en bin/run.sh)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-F-r3/ca4.txt

En el scratchpad r3 quedan las carpetas temporales d.*, j.* y h.* de CA-2, porque el control de seguridad bloqueó su `rm -rf` con variables. Viven fuera del worktree y se pueden borrar a mano.

```
VEREDICTO: VERDE
INFORME: revisiones/U07-F/ronda-3.md
```
