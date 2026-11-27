# Ronda 2 — U05-T02

VEREDICTO: VERDE

Los tres criterios pasan con mis propias ejecuciones. La evidencia nueva de la bitácora coincide línea por línea con lo que obtuve, salvo una diferencia en CA-3 que se explica sola (F-04, AMARILLO). M4 y M2 mueren las dos. No queda ningún ROJO ni NARANJA abierto.

Comprobaciones previas:
- **Tarea:** hash `617c30473945d41e87c6ab35e674d367a765acef`, coincide.
- **HEAD:** `63b353d` en `m6r/U05-T02`. `origin/main` está en `1234f82`.
- **Commits de la ronda 2:** son dos. `1f4f37b` solo toca la prueba y la bitácora. `63b353d` solo añade 12 líneas a la bitácora.
- **Código de main:** no cambió respecto a la ronda 1.
- **Worktree:** limpio antes y después de la revisión (`git status --short` vacío).
- **Entorno:** todas las ejecuciones usaron `HOME`, `user.home` (vía `JAVA_TOOL_OPTIONS`) y `KUBECONFIG` apuntando a archivos falsos en el scratchpad. `GRADLE_USER_HOME` quedó en la caché real de Gradle. No usé `kubectl` ni ningún clúster.
- **Mutaciones:** se hicieron en copias del scratchpad sacadas con `git archive 63b353d`, nunca en el worktree.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La guardia rechaza sin hacer peticiones; tests ≥ 10, sin saltos | `./gradlew :backend:test --rerun --tests '*GuardiaNamespaceTest'` + grep del XML | pasa. `BUILD SUCCESSFUL in 12s`, `tests="22" skipped="0" failures="0" errors="0"`, suite de 4,66 s con el servidor simulado real. Hay 18 casos de rechazo: los 12 que pide la tarea más `synkra-pe\n`, `Synkra-pe`, ` synkra-pe`, `synkra-p1`, `synkra-pé` y `synkra-ñu`. Pasan también los aceptados `synkra-pe` y `synkra-co`, el recorte a 63 y el mensaje exacto con `null` y con `synkra-PE` |
| 2 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + bucle de grep sobre los XML | pasa. `BUILD SUCCESSFUL in 22s` y 24 XML, todos con `skipped="0" failures="0" errors="0"` |
| 3 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa. Son 8 archivos y 304 inserciones: 5 bajo `kubernetes/**` (main y test), `bitacoras/U05-T02.md` y dos en `revisiones/U05-T02/`. No hay nada fuera de lo permitido |

## Evidencia de la bitácora contra mis salidas
- **CA-2:** extraje las 24 líneas `TEST-*` de la sección «CA-2 fresco» de la bitácora y las comparé con `diff` contra las 24 de mi bucle. Son **idénticas**: mismos archivos, mismos conteos, incluido `GuardiaNamespaceTest tests="22"`. F-01 de la ronda 1 queda cerrado.
- **CA-3:** las cinco líneas de `kubernetes/**` y las dos de `revisiones/` coinciden exactamente. Solo difieren `bitacoras/U05-T02.md` (79 frente a 91) y el total (292 frente a 304). Ver F-04.
- **Rojo primero de M4:** la bitácora dice «22 tests completed, 2 failed», y mi M4 da lo mismo (abajo).

## Mutaciones (copias del scratchpad)
| Mutación | Resultado |
|---|---|
| M4: `[a-z]{2}` cambiado a `\p{Ll}{2}` | **muerta**. `22 tests completed, 2 failed`: fallan exactamente `[17] synkra-pé` y `[18] synkra-ñu`. F-02 de la ronda 1 queda cerrado |
| M2: guardia movida después de `cliente.namespaces().withName(namespace).get()`, tragando la `RuntimeException` del cliente | **muerta**. `22 tests completed, 15 failed`: el contador de peticiones sube en 15 de los 18 rechazos |

En M2 sobreviven tres casos (`null`, `""` y ` synkra-pe`), los mismos que en la ronda 1. El motivo es que Fabric8 valida esos nombres en el cliente antes de mandar la petición. No es un hueco de la prueba: el resto de casos detecta la mutación.

## Hallazgos de la ronda 1, estado
- **F-01 (ROJO), cerrado.** CA-2 trae ahora las 24 líneas pegadas, idénticas a las mías, y CA-3 trae su `--stat`.
- **F-02 (AMARILLO), cerrado.** M4 muere.
- **F-03 (AMARILLO), cerrado.** `mensajeConNullYConNamespaceCorto` usa `hasMessage` exacto con `"Namespace no permitido: null"` y `"Namespace no permitido: synkra-PE"`, y pasa.

## Hallazgos
### F-04 · AMARILLO · bitacoras/U05-T02.md (sección «CA-3 fresco») · El `--stat` pegado es del commit anterior al que lo contiene
- La bitácora muestra `bitacoras/U05-T02.md | 79` y `292 insertions`. En HEAD salen 91 y 304.
- Lo comprobé: `git show 1f4f37b:bitacoras/U05-T02.md | wc -l` da 79, y `git diff 1f4f37b 63b353d --stat` da `bitacoras/U05-T02.md | 12 ++++++++++++`, que es justo la diferencia.
- La salida es real y fresca para `1f4f37b`. Es inevitable que no coincida con el commit que la pega, porque pegar el `--stat` en la bitácora cambia el propio `--stat`.
- Las líneas de código coinciden exactamente, así que no es evidencia imposible para el diff actual.
- Sugerencia que no bloquea: anotar el sha sobre el que se tomó la salida (`git rev-parse HEAD` junto al `--stat`).

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La del bean `KubernetesClient`, que permite saltarse la guardia fuera del adaptador, sigue abierta y corresponde al orquestador, como se indicó.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/ca2.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/ca2-xml.txt (mis 24 líneas)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/bit-ca2.txt (las 24 líneas de la bitácora; `diff` vacío contra la anterior)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/m4.log (M4, con su copia en `u05r2/m4/`)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/u05r2/m2.log (M2, con su copia en `u05r2/m2/`)

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U05-T02.md (CA-3 fresco)|El --stat pegado es de 1f4f37b, no del commit que lo contiene
INFORME: revisiones/U05-T02/ronda-2.md
```
