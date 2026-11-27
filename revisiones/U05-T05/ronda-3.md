# Ronda 3 — U05-T05

VEREDICTO: VERDE

Los tres criterios pasan en mi propia ejecución. El mutante `nullsFirst`, que sobrevivía en la ronda 2, ahora muere. No queda ningún ROJO ni NARANJA en pie. No hay hallazgos nuevos en esta ronda.

Datos de la revisión:
- **Tarea.** El hash con `git hash-object` es 65a449cecd38706d8a1f17df2d817a5e51150f09 y coincide con el esperado.
- **Diff.** HEAD `2919672` contra la base `0a52e2e`.
- **Gradle.** Corrió con `HOME`, `KUBECONFIG` y `-Duser.home` en mi scratchpad, y con `GRADLE_USER_HOME=/root/.gradle`.
- **Clúster.** No usé kubectl ni ningún clúster, solo el servidor simulado.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `EstadoYContextoTest`: al menos 8 pruebas, sin saltos ni fallos | `./gradlew :backend:test --rerun --tests '*EstadoYContextoTest'` + grep del XML | **pasa**: `BUILD SUCCESSFUL in 7s` y `tests="20" skipped="0" failures="0" errors="0"`. El XML es de mi corrida (`2026-11-27T21:00:41Z`). Cada prueba tarda entre 0,036 y 1,45 s, con el servidor simulado real detrás, así que no se saltaron. |
| 2 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | **pasa**: `BUILD SUCCESSFUL in 1m 4s`. Hay 28 XML y ninguno tiene saltos, fallos ni errores (0 líneas fuera de `skipped="0" failures="0" errors="0"`). Sus marcas van de 21:00:55Z a 21:01:55Z. |
| 3 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 0a52e2e...HEAD` | **pasa**: `2919672`, 18 archivos, +1114. Ningún archivo cae fuera de estas rutas (el filtro con `grep -v` sale vacío): `backend/src/{main,test}/java/io/synkra/kubernetes/**`, `bitacoras/U05-T05*` y `revisiones/U05-T05/`. En la ronda 3 el código de `main` solo cambia el import de `Supplier`. |

## Estado de los hallazgos anteriores
| Id | Estado | Evidencia mía |
|---|---|---|
| F-01 a F-05 | **cerrados** (ronda 2) | Las pruebas siguen presentes y pasan en mi corrida de CA-1. |
| F-06 | **cerrado** | Lo cierra F-08. |
| F-07 | **no aplica** | Quedó arbitrado en la ronda 1. |
| F-08 | **cerrado** | Detalle debajo de la tabla. |
| F-09 | **cerrado** | `RUTA_PODS` se construye ahora con `URLEncoder.encode(ETIQUETA_NOMBRE + "=" + CORE, UTF_8)`. La usa `falla(RUTA_PODS, 500)` en la línea 402, y esa prueba pasa, así que la ruta codificada coincide con la que pide el cliente. |
| F-10 | **no se cambia** | Así lo arbitró el orquestador, que amplía C-76. No bloquea. |
| F-11 | **cerrado** | `Supplier` ya se importa en `LectorPais` y los imports de la prueba están ordenados. |

Cómo comprobé F-08:
- **La prueba sirve los eventos con una expectativa.** La lista llega por `servidor.expect().get().withPath(RUTA_EVENTOS)...always()`. En ella `e0` no trae ninguna marca y `ebasura` trae `lastTimestamp: "no-es-una-fecha"`. La prueba exige 5 líneas en el orden primero, segundo, tercero, sin marca, marca ilegible.
- **La expectativa sí se usa.** El servidor CRUD ya no tiene eventos sembrados para esta prueba, así que las 5 líneas solo pueden venir de ella.
- **Mutante 1, con `nullsFirst`.** Lo corrí en una copia hecha con `git archive` en el scratchpad, cambiando `nullsLast` por `nullsFirst` en `LectorPais.java:126`. Resultado: `losEventosSeOrdenanPor...SinMarcaVanAlFinal() FAILED`, `20 tests completed, 1 failed`, `BUILD FAILED`. El mutante muere.
- **Mutante 2, mío.** En la misma copia, el `catch (DateTimeParseException)` devolvía `Instant.EPOCH` en lugar de `null`. También muere: la misma prueba falla, 1 de 20. Así queda probada también la rama de la marca ilegible.
- **Evidencia del codificador.** La transcripción `bitacoras/U05-T05-evidencia/r3-mutante-nullsFirst.txt` coincide con lo que obtuve yo.

## Hallazgos nuevos
Ninguno. Revisé el diff de la ronda 3 (`7602fb8..2919672`) con estos resultados:
- **Alcance.** No hay ningún cambio fuera de él.
- **Literales.** No aparece ningún literal nuevo visible para el usuario en `main`. Las fechas y `"no-es-una-fecha"` solo están en la prueba.
- **Pruebas.** No hay `sleep`, `@Disabled` ni pruebas omitidas.
- **Orden de los dos eventos sin marca.** La prueba supone que `e0` queda antes que `ebasura`. Eso depende de que `Stream.sorted` sea estable sobre un stream ordenado, y lo es por contrato. No es un defecto.

**Estado del worktree al terminar:** limpio. `git status --short` sale vacío en `/home/user/wt-m6r-U05-T05` y HEAD sigue en `2919672`. Los mutantes corrieron en `/tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/mut`. No toqué `/home/user/wt-m6r-U02-T06`.

## Tareas candidatas (defectos reales fuera de alcance)
- **C-76 ampliada (F-10).** Siguen sin probarse la configuración de reintentos de producción y la latencia de unos 19 s con 11 `GET` cuando el servidor responde 5xx o 429. Le toca a U07.
- C-82 y C-83 siguen en pie, tal como las registró el orquestador.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca2.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/ca2-xml.txt (los 28 XML)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/mut1.log (mutante `nullsFirst`, muere)
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r3/mut2.log (mutante con `Instant.EPOCH` para la marca ilegible, muere)

```
VEREDICTO: VERDE
INFORME: revisiones/U05-T05/ronda-3.md
```
