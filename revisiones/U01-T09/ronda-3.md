# Ronda 3 — U01-T09

VEREDICTO: VERDE

F-06, el último hallazgo NARANJA, quedó cerrado. Corrí yo los 8 criterios sobre HEAD 54c96e2 y todos pasan. Mis 19 mutaciones fallan las 19, incluida msgRepSinNombre, que en la ronda 2 sobrevivía. No queda ningún hallazgo ROJO ni NARANJA en pie.

Contexto:
- Worktree /home/user/wt-m6r-U01-T09, HEAD 54c96e2, base origin/main 078a4d1.
- El hash de la tarea sigue siendo 7865240311c637cf568f88bb6e3c13e34a58e3ce.
- Las mutaciones corrieron en una copia nueva de `git archive HEAD` en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r3rev/copia.
- Al terminar, el worktree está limpio (`git status --short` sin salida).

## Diff de esta ronda (b01a6ee..54c96e2)
- Solo cambian `VerificadorTest.java` (+4/−1), la bitácora, `revisiones/U01-T09/ronda-2-respuesta.md` y `revisiones/U01-T09/ronda-2.md`.
  - `s10` exige ahora `kyc-pe`, `1.3.0` y `2.0.0` en el mensaje.
  - `dosNombres…` comprueba en r1 que cada mensaje global contiene su propio `plugin`.
- Con `git diff --quiet b01a6ee..HEAD` comprobé que no cambiaron `verificador/src/main`, `src/testFixtures`, `aidlc-docs` ni `CANDIDATAS.md`. Por eso CA-3 a CA-5 descansan sobre el mismo código y los mismos documentos que ya verifiqué. Aun así, CA-2, CA-5 y CA-6 los volví a correr.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | VerificadorTest | `./gradlew :verificador:test --rerun --tests '*VerificadorTest'` + grep del XML | pasa: `tests="15" skipped="0" failures="0" errors="0"` |
| 2 | CatalogoEnMemoria | `--rerun --tests '*CatalogoEnMemoriaTest'` | pasa: 5/0/0/0 |
| 3 | Mutaciones (a)–(d) | copia de archive, una a la vez, y revertir | pasa: (a), (b) y (c) fallan en `s7…`; (d) da 3 fallos. Al revertir: 15/0/0/0 y `cmp` idéntico. `git diff --stat -- verificador/src/main` muestra solo Catalogo.java y Verificador.java |
| 4 | R-V4 | sin cambios desde la ronda 2 (`git diff --quiet`) | pasa: lo verifiqué en la ronda 2 |
| 5 | C-06 | `--rerun --tests '*LectorDescriptorTest'` | pasa: 29/0/0/0 |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE …` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL, 9 XML, los 9 con `skipped="0"`, **134** pruebas |
| 8 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 13 archivos, los de la lista más `revisiones/U01-T09/` (ronda-1, ronda-1-respuesta, ronda-2 y ronda-2-respuesta). Nada fuera de la lista |

## Banco de mutaciones (VerificadorTest de HEAD, 15 pruebas cada una; 18:29:17Z a 18:29:49Z)
El fuente de Verificador.java es idéntico al de la ronda 2 (`cmp`), así que reutilicé los mismos mutantes sin cambios.

| Mutación | Fallos | Pruebas que la detectan |
|---|---|---|
| a | 1 | s7 |
| b | 1 | s7 |
| b2 (acumulativa) | 1 | pluginAusenteCorta… |
| bfull | 2 | s7, pluginAusenteCorta… |
| c | 1 | s7 |
| d | 3 | s10, dosNombres…, tresApariciones… |
| hash | 1 | dosNombres… |
| repglobal | 3 | s10, dosNombres…, tresApariciones… |
| msgAus / msgAusSinVersion / sinNombre1 | 1 c/u | s6 |
| msgCore / msgCoreSinPedido / sinNombre2 | 1 c/u | s7 |
| msgVer / msgVerSinRango / sinNombre3 | 1 c/u | s4 |
| msgRep | 2 | s10, dosNombres… |
| **msgRepSinNombre** | **2** | **s10, dosNombres…**. Coincide con lo que declara el codificador |

Las 19 mutaciones fallan. Después de todas, la copia quedó restaurada (`cmp` idéntico) y en verde con 15/0/0/0.

## Hallazgos
- **F-06 · cerrado.** msgRepSinNombre ya no sobrevive.
- **F-01 a F-05 · siguen cerrados.** Sus mutaciones siguen fallando, y el código principal no cambió.
- **Regresiones · ninguna.** Siguen 134 pruebas, 0 saltadas, con el mismo código principal.
- **Sondeo adicional de la aserción nueva · sin hallazgo.**
  - `m.mensaje().contains(m.plugin().orElseThrow())`: si `plugin` queda vacío, `orElseThrow` lanza y la prueba falla, como muestra repglobal.
  - Si el mensaje nombra otro plugin, también falla, porque en r1 conviven dos nombres distintos.
- No queda abierto ningún hallazgo nuevo, ni siquiera AMARILLO.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
En /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r3rev/:
- Salida de los criterios: `ca.txt`.
- Un log por mutación: `mut-*.log`.
- Mutantes: `mut/`.

```
VEREDICTO: VERDE
INFORME: revisiones/U01-T09/ronda-3.md
```
