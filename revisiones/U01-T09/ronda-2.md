# Ronda 2 — U01-T09

VEREDICTO: NO-VERDE

Hay un solo hallazgo NARANJA en pie, F-06. Es la última instancia de la clase F-02: ninguna prueba exige que el mensaje de PLUGIN_REPETIDO nombre el plugin. Todo lo demás se corrigió y yo mismo lo comprobé. Los 8 criterios pasan y las 8 mutaciones de la ronda 1 fallan, incluida la b2 acumulativa.

Contexto:
- Worktree /home/user/wt-m6r-U01-T09, HEAD b01a6ee, base origin/main 078a4d1.
- El hash de la tarea coincide: `git hash-object` da 7865240311c637cf568f88bb6e3c13e34a58e3ce.
- Las mutaciones corrieron en una copia nueva de `git archive HEAD` en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r2rev/copia, con el VerificadorTest de HEAD.
- Al terminar, el worktree está limpio (`git status --short` sin salida).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | VerificadorTest | `./gradlew :verificador:test --rerun --tests '*VerificadorTest'` + grep del XML | pasa: `tests="15" skipped="0" failures="0" errors="0"` |
| 2 | CatalogoEnMemoria | `--rerun --tests '*CatalogoEnMemoriaTest'` | pasa: 5/0/0/0. Esta ronda no tocó el archivo y mis 3 mutaciones de la ronda 1 lo siguen cubriendo |
| 3 | Mutaciones (a)–(d) | copia de archive, una a la vez, y revertir | pasa: (a), (b) y (c) fallan en `s7…`; (d) da 3 fallos (`s10…`, `dosNombres…`, `tresApariciones…`). Al revertir: 15/0/0/0 y `cmp` idéntico. En el worktree, `git diff --stat -- verificador/src/main` muestra solo Catalogo.java y Verificador.java |
| 4 | R-V4 | `sed -n '/## R-V4/,/## R-V5/p' …` | pasa: el párrafo de las decisiones 1, 2 y 3 sigue, y la cabecera ahora dice «(a lo sumo uno por plugin, ver abajo)». El diff del archivo es +7/−1, todo dentro de R-V4 |
| 5 | C-06 | `--rerun --tests '*LectorDescriptorTest'` | pasa: 29/0/0/0. En la ronda 1 comprobé que la prueba falla si se cambia el campo en Version o en RangoVersion |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE …` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL, 9 XML, **134** pruebas, todas con `skipped="0"` |
| 8 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: los 9 archivos de la ronda 1 más `revisiones/U01-T09/ronda-1.md` y `ronda-1-respuesta.md`, que entran por «revisiones/U01-T09/». Nada fuera de la lista |

## Mutaciones (VerificadorTest de HEAD, 15 pruebas cada una; hora UTC)
| Mutación | Resultado | Prueba que la detecta |
|---|---|---|
| a: pasos 3 y 4 intercambiados | falla (1) | s7 |
| b: sin corte 3→4 | falla (1) | s7 |
| bfull: ningún corte | falla (2) | s7, pluginAusenteCorta… |
| **b2 acumulativa** (la de la ronda 1) | falla (1) | pluginAusenteCorta…: `but was: <[PLUGIN_AUSENTE\|kyc-zz\|, CORE_EQUIVOCADO\|kyc-zz\|core]>` |
| c: paso 2 en `s.core()` | falla (1) | s7 |
| d: repetidos dentro de `porPlugin` | falla (3) | s10, dosNombres…, tresApariciones… |
| hash: LinkedHashMap cambiado por HashMap | falla (1) | dosNombres…: `expected: <[kyc-co, kyc-pe]> but was: <[kyc-pe, kyc-co]>` |
| repglobal: `plugin` vacío en REPETIDO | falla (3) | s10, dosNombres…, tresApariciones… |
| msgAus / msgCore / **msgVer** / **msgRep** (mensaje completo reemplazado) | fallan las 4 | s6 / s7 / s4 / s10 |
| msgVerSinRango, msgAusSinVersion, msgCoreSinPedido | fallan las 3 | s4 / s6 / s7 |
| sinNombre1..3 (sin el nombre del plugin en AUSENTE, CORE e INCOMPATIBLE) | fallan las 3 | s6 / s7 / s4 |
| **msgRepSinNombre** (`"Un plugin aparece N veces … (versiones [...])"`) | **sobrevive: BUILD SUCCESSFUL, 15/0/0/0** (18:26:36Z) | ninguna |

Después de todas, la copia quedó restaurada (`cmp` idéntico) y en verde con 15/0/0/0.

## Hallazgos

### F-06 · NARANJA · VerificadorTest.java `s10RepetidoUnSoloMotivoGlobal` · El nombre del plugin en el mensaje de PLUGIN_REPETIDO no está atado (resto de la clase F-02)
La decisión 4 pide que los mensajes nombren el plugin. Para los tres motivos por plugin, quitar el nombre ya rompe una prueba (sinNombre1..3). Para PLUGIN_REPETIDO no: `s10` solo comprueba `msg.contains("1.3.0") && msg.contains("2.0.0")`.

La mutación msgRepSinNombre quita `'kyc-pe'` del mensaje y deja las versiones. Pasa en verde: log en `mut-msgRepSinNombre.log`. La mutación gruesa msgRep sí falla, pero solo porque se lleva también las versiones.

Basta con añadir `msg.contains("kyc-pe")` en `s10`. Si se quiere barrer también el caso de dos nombres, en `dosNombresRepetidos…` se puede comprobar el nombre en cada mensaje.

### Hallazgos de la ronda 1
- **F-01 · cerrado.** El caso `pluginAusenteCortaAunqueSeaDeOtroCoreOIncompatible` detecta b2, tanto en mi versión acumulativa como en la del codificador por caída al paso siguiente, y también bfull.
- **F-02 · cerrado en parte.** Cubre AUSENTE, CORE e INCOMPATIBLE en el nombre y en las versiones o cores. Queda el nombre en REPETIDO, que es F-06.
- **F-03 · cerrado.** Se usa `Version.toString()` y se eliminó `texto()`.
- **F-04 · cerrado.** Con los dos órdenes, la mutación hash falla.
- **F-05 · cerrado.** La cabecera de R-V4 se ajustó.

### Regresiones y sondeo nuevo
No encontré ninguna regresión. En Verificador.java cambian solo los mensajes (ahora con `toString`), y la salida es la misma de la ronda 1 porque `toString` y el antiguo `texto` coinciden. Las pruebas nuevas son JUnit puras, no usan `sleep` ni estado externo, y ninguna se salta. Los 134 tests suman la ronda 1 (132) más las 2 nuevas.

## Juicio de las desviaciones declaradas
1. **b2 hecha como «caída al paso siguiente».** La acepto. Además corrí la versión acumulativa de la ronda 1, y también falla.
2. **Las mutaciones corrieron con un test anterior.** Queda resuelta: yo corrí todas con el VerificadorTest de HEAD, con los resultados de la tabla.
3. **Pruebas nuevas sin rojo previo.** La acepto. Atan comportamiento que ya existía, y su valor se ve en que las mutaciones que antes sobrevivían ahora fallan. Solo queda la excepción de F-06.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
En /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r2rev/:
- Salida de todos los criterios: `ca.txt`.
- Un log por mutación: `mut-*.log`.
- Fuentes mutadas: `mut/`.
- Generadores: `mutar2.py` en esa carpeta y `mutar.py` en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r1/.

```
VEREDICTO: NO-VERDE
NARANJA|VerificadorTest.java s10RepetidoUnSoloMotivoGlobal|el nombre del plugin en el mensaje de PLUGIN_REPETIDO no está atado (mutación msgRepSinNombre sobrevive, 15/0/0/0)
INFORME: revisiones/U01-T09/ronda-2.md
```
