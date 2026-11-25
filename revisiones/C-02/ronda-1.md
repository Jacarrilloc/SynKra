# Ronda 1 — C-02

VEREDICTO: VERDE

Contexto: tarea con el hash vigente `09ca51e7188b23082d4f7889513041f1dbb4be80`, que comprobé con `git hash-object`. El codificador trabajó con `f4bfec33…`. `git diff 026b586 1d623f2` cambia una sola línea, el comando de CA-3, de `grep -c jqwik-database` a `grep -c '\.jqwik-database$'`. Es lo que se dijo: un defecto de especificación del orquestador. El codificador lo detectó y lo dejó en su bitácora sin tocar la tarea, y eso es correcto.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | git ignora la base de jqwik en cualquier subproyecto | `git check-ignore -v verificador/.jqwik-database backend/.jqwik-database` | pasa: dos líneas, `.gitignore:7:**/.jqwik-database` para cada ruta, exit 0 |
| 2 | lectura de vuelta tras correr jqwik | `./gradlew :verificador:test --rerun`; `test -f verificador/.jqwik-database && echo existe`; `git status --porcelain --untracked-files=all \| grep -c jqwik` | pasa: `BUILD SUCCESSFUL in 2s`, `existe`, `0` |
| 3 | no está rastreado (texto vigente) | `git ls-files \| grep -c '\.jqwik-database$'` | pasa: `0` |

Evidencia de frescura para CA-2:
- XML de `verificador/build/test-results/test/`:
  - `RangoVersionTest`: tests=36, skipped=0, failures=0, errors=0, time=0.258 s.
  - `VersionTest`: tests=16, skipped=0, failures=0, errors=0, time=0.043 s.
- Los timestamps de los XML (20:55:08Z) y el mtime de `verificador/.jqwik-database` (15:55:08 -05) coinciden con la hora de mi ejecución. La base la regeneró mi corrida, no es un resto anterior.
- Que la suite dure 2 s es normal: 52 pruebas puras en memoria, sin estado externo.

Rojo primero, reproducido por mí:
- Cloné la rama base `m6r/U01-T04` con `--shared` en el scratchpad y creé `verificador/.jqwik-database`.
- `check-ignore` salió con exit 1 y el conteo de `git status` dio `1`. Coincide con lo que registra la bitácora.
- Después borré el clon.

## Alcance
- `git diff m6r/U01-T04...m6r/C-02 --stat` toca `.gitignore` (+3), `bitacoras/C-02.md` y `tareas/C-02-ignorar-jqwik-database.md`.
- El commit del codificador (026b586) toca solo `.gitignore` y la bitácora. El archivo de tarea viene de 1a19e88 y 1d623f2, que son del orquestador.
- En `.gitignore` solo se añaden `# jqwik` y `**/.jqwik-database`. No hay más cambios ni en `.gitignore`, ni en Gradle, ni en código, y nada se mueve a `build/`. No hay desborde.

## Hallazgos
Ninguno ROJO ni NARANJA.

### F-01 · AMARILLO · bitacoras/C-02.md · acuse con hash anterior
El acuse de lectura cita `f4bfec33…`, y la tarea vigente es `09ca51e7…`. No es culpa del codificador, porque la tarea cambió después de su commit. La diferencia se limita al comando de CA-3, que ya corrí con el texto vigente. Lo anoto solo para que la cadena de evidencia quede explícita. No pide ningún cambio.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. Mover la base de jqwik a `build/` con `junit-platform.properties` sigue excluido de forma explícita en esta tarea. Si se quiere hacer, sería una candidata aparte.

## Estado del worktree
`git status --porcelain --untracked-files=all` sale vacío tras mi revisión: el worktree está limpio. Hay un `.jqwik-database` en disco, pero está ignorado, que es justo el comportamiento que pide la tarea.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.log

```
VEREDICTO: VERDE
AMARILLO|bitacoras/C-02.md|acuse de lectura cita el hash anterior de la tarea (cambio posterior del orquestador en CA-3)
INFORME: revisiones/C-02/ronda-1.md
```
