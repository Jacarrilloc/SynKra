# Ronda 3 — U01-T06

VEREDICTO: VERDE

Corrí yo mismo los cuatro criterios y todos pasan. Las correcciones de F-07 y F-09 aguantan mis sondas. Mis 28 textos de ida y vuelta de la ronda 2 vuelven ahora iguales, los 28: en la ronda 2 fallaban 17. Las entradas hostiles de las rondas 1 y 2 dan lo mismo que antes, sin una sola excepción lanzada. No queda ningún ROJO ni NARANJA en pie.

Contexto:
- Worktree `/home/user/wt-m6r-U01-T06`, rama `m6r/U01-T06` en d889b80873fe.
- El hash git de la tarea coincide (160f039…).
- Diff de la tarea: b18d6e3..d889b80. Cambios de esta ronda: 9b5be46..d889b80. En esta copia la base solo existe como `origin/modulo-6-rehecho`, así que usé la base que usted me pasó.
- El worktree sigue limpio después de la revisión (`git status --short` sin líneas).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del lector, sin caché | `./gradlew :verificador:test --rerun --tests '*LectorDescriptorTest'` | pasa: BUILD SUCCESSFUL. La tarea `test` sí se ejecutó (`1 executed`) y el XML tiene la marca 2026-11-26T02:26:52Z, de mi propia corrida |
| 2 | Reporte: tests ≥ 10, nada saltado | `grep -ho 'tests=…' …TEST-*LectorDescriptorTest.xml` | pasa: `tests="28" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 1 y 86 ms y ninguno da cero. Siguen todos los casos mínimos del CA-2 |
| 3 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0`. La única dependencia sigue siendo `snakeyaml-engine:3.2` |
| 4 | Todo el verificador, sin caché | `./gradlew :verificador:check --rerun` | pasa: BUILD SUCCESSFUL. Lector 28, RangoVersion 36 y Version 16, con skipped, failures y errors en 0 y las marcas de tiempo de mi corrida |

**El «1s» de la bitácora.** Me pareció poco para un `--rerun`, pero lo reproduje: el daemon ya estaba caliente, la compilación estaba al día y la tarea `test` corrió de verdad. Las marcas de tiempo del XML son nuevas.

**Alcance.** Esta ronda solo toca:
- `LectorDescriptor.java`, con cambios en `aYaml` y en la condición de clave de `etiquetas`;
- `LectorDescriptorTest.java`, con 2 pruebas nuevas;
- R-V3 en `business-rules.md`;
- la bitácora y la respuesta.

En el diff completo de la tarea no hay archivos fuera de `verificador/src`, `functional-design`, `bitacoras` y `revisiones`. No hay desborde.

**Rojo primero.** Está en la bitácora: 28 pruebas, de las cuales fallan las 2 nuevas. Además, mis sondas de la ronda 2 ya mostraban que `~` y `007` no volvían iguales con el `aYaml` anterior, así que la prueba nueva sí habría fallado.

## Hallazgos de la ronda 2, comprobados con las mismas entradas
- **F-07, cerrado.** Compilé de nuevo mi `Ronda2.java` contra las clases actuales, y los 28 textos vuelven iguales: 22 en `descripcion` y 6 en la etiqueta `cliente`.
  - `~`, `007` y `True` salen entre comillas (`'~'`, `'007'`, `'True'`) y `hola` sale sin ellas.
  - Añadí 25 textos más y todos vuelven iguales:
    - 17 textos difíciles, cada uno en `descripcion` y en `cliente`: `a: b`, `# c`, multilínea, espacio inicial, `- x`, `&ancla`, `*alias`, `!etq`, `{a}`, `[b]`, `yes`, `on`, Unicode, comilla simple, comilla doble, tabulador y NEL;
    - 8 claves de etiqueta que cumplen el patrón pero parecen otro tipo: `1`, `007`, `true`, `null`, `0x1f`, `0o17`, `1e3` y `pais`.
- **F-08, cerrado según el arbitraje.** Queda documentado en R-V3 y no hubo cambio de código.
- **F-09, cerrado.**
  - Las claves que YAML resuelve como otro tipo dan `DESCRIPTOR_INVALIDO` con `campo = etiquetas`: `{1: pe}`, `{true: pe}`, `{1.5: pe}`, `{.inf: pe}`, una clave vacía (`? :`) y `{!!int pais: pe}`. Con dos claves malas a la vez salen dos motivos acumulados.
  - Las claves `'1'`, `"true"` y `!!str 1` dan `Exito`.
  - Una clave que es una secuencia (`[a]`) da `DESCRIPTOR_INVALIDO` desde el parser.
- **Sin regresiones.**
  - Mis sondas `Hostil` (52 líneas) y `Hondo` (10 líneas) de las rondas 1 y 2 dan una salida idéntica byte a byte a la de la ronda 2.
  - En el resto de `Ronda2` solo cambian las dos líneas que F-09 debía cambiar (`etq clave int` y `etq clave true`), que pasan de `Exito` a `Fallo DESCRIPTOR_INVALIDO`.
  - Ninguna sonda lanza excepción.

## Hallazgos
### F-10 · AMARILLO · business-rules.md R-V3 · El trato de una etiqueta explícita no es el mismo en campos y en claves
`version: !!int 1` da `VERSION_INVALIDA`, como documenta F-08. En cambio, `{!!int pais: pe}` y `{pais: !!int pe}` en `etiquetas` dan `DESCRIPTOR_INVALIDO`, porque ahí se exige `str` estricto. Los dos son `Fallo` y en ningún caso sale un `Exito` indebido. No bloquea, pero conviene escribirlo en R-V3 en una línea si se toca de nuevo.

## Tareas candidatas (defectos reales fuera de alcance)
- Sigue en pie la de la ronda 1: los mensajes de `leer` incluyen la ruta absoluta del archivo. Hay que revisarlo cuando se exponga por la API (U04).
- Lo del generador para U01-T07 ya está registrado como C-15. Sugiero sumarle las claves de etiqueta que parecen otro tipo (`007`, `true`, `null`), que también cumplen el patrón de `nombre`.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/ca3.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/ca4.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/Ronda2.txt (los 28 textos de ida y vuelta de la ronda 2 y las sondas de tipos)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/Hostil.txt y /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/Hondo.txt (las entradas hostiles de la ronda 1, repetidas)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/Ronda3.txt (sondas nuevas; el código está en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r3/src/Ronda3.java)

No escribí `revisiones/U01-T06/ronda-3.md`, porque mis herramientas son de solo lectura. El informe completo es este mensaje.

```
VEREDICTO: VERDE
AMARILLO|business-rules.md R-V3|Etiqueta explícita: en campos se trata como escalar plano y en claves y valores de etiquetas exige str (documentar)
INFORME: revisiones/U01-T06/ronda-3.md
```
