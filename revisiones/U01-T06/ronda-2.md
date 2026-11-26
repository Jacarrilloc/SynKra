# Ronda 2 — U01-T06

VEREDICTO: NO-VERDE

Los cuatro criterios pasan cuando los corro yo, y las seis correcciones de la ronda 1 se sostienen contra mis entradas hostiles de entonces. Pero el cambio a `CoreSchema` rompió la ida y vuelta: 17 de mis 28 textos de prueba ya no vuelven iguales. Usted me pidió comprobar justo eso, y en la ronda 1 los 28 volvían iguales.

Contexto: worktree `/home/user/wt-m6r-U01-T06`, rama `m6r/U01-T06` en 8f135e8, base b18d6e3. La rama `modulo-6-rehecho` sigue sin existir aquí, así que usé la base que usted pasó. El hash de la tarea coincide (160f039…). El worktree está limpio después de la revisión.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del lector, sin caché | `./gradlew :verificador:test --rerun --tests '*LectorDescriptorTest'` | pasa: BUILD SUCCESSFUL. El XML es de las 02:22:17, del final de mi propia corrida |
| 2 | Reporte: tests ≥ 10, nada saltado | `grep -ho 'tests=…' …TEST-*LectorDescriptorTest.xml` | pasa: `tests="26" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 1 y 102 ms, ninguno da cero, y siguen todos los casos mínimos del CA-2 |
| 3 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0`. La única dependencia sigue siendo `snakeyaml-engine:3.2` |
| 4 | Todo el verificador, sin caché | `./gradlew :verificador:check --rerun` | pasa: BUILD SUCCESSFUL. Lector 26, RangoVersion 36 y Version 16, con skipped, failures y errors en 0 en las tres |

Alcance: esta ronda toca `LectorDescriptor`, su prueba, R-V3 en business-rules.md (como pedía el arbitraje de F-04), la bitácora y la respuesta. No hay desborde.

El rojo primero está en la bitácora: 8 pruebas nuevas fallaban, entre ellas un `StackOverflowError`.

## Hallazgos de la ronda 1, comprobados de nuevo con las mismas entradas
- **F-01, cerrado.** Probé de 500 a 32000 niveles (hasta 64067 caracteres), en secuencias, en mapas y por `leer`: todos devuelven `DESCRIPTOR_INVALIDO` y ninguno lanza. Probé además tres casos nuevos y ninguno lanza:
  - un bloque indentado de 200 niveles;
  - un flujo sin cerrar de 5000 corchetes;
  - una cadena de 10 alias de 30 niveles cada uno, que se queda en `Fallo` por campos desconocidos.
- **F-02, cerrado.**
  - `nombre` repetido y `etiquetas {pais: pe, pais: mx}` dan `DESCRIPTOR_INVALIDO`.
  - `"nombre"` entre comillas junto a `nombre` también se detecta.
  - Los duplicados se acumulan con `VERSION_INVALIDA` y `CAMPO_DESCONOCIDO`.
- **F-03, cerrado según el arbitraje.**
  - `descripcion: ~` y `descripcion: null` cuentan como ausentes.
  - Un nulo como valor de etiqueta da `DESCRIPTOR_INVALIDO`.
  - `nombre: 123`, `nombre: true` y `nombre: 0x1f` dan `DESCRIPTOR_INVALIDO`; `nombre: ~` da `FALTA_CAMPO`.
  - `version: !!int 1.0.0` da `DESCRIPTOR_INVALIDO`, y `version: 2.1` sigue dando `VERSION_INVALIDA`.
- **F-04, F-05 y F-06, cerrados.**
- **El resto de las entradas hostiles de la ronda 1 da lo mismo que entonces:** etiquetas de tipo Java, la forma verbatim, `%TAG`, `!!binary`, las «mil carcajadas», varios documentos, el BOM, la bomba ZIP, un archivo que no es ZIP y bytes que no son UTF-8.
- **CA-2 con `CoreSchema`, sin cambios:**
  - un descriptor válido con `pais: pe` y descripción da `Exito`;
  - `version: 2.1` da `VERSION_INVALIDA` y `^2.1` da `RANGO_INVALIDO`;
  - S8, S9 y los demás casos de la suite siguen verdes.

## Hallazgos

### F-07 · NARANJA · LectorDescriptor.java:326 (`aYaml`) · El lector y el serializador usan esquemas distintos y la ida y vuelta se rompió
`desdeYaml` ahora lee con `CoreSchema` (línea 90), pero `aYaml` escribe con el `DumpSettings` por defecto, que es el esquema JSON. El serializador deja sin comillas los textos que no parecen un valor en JSON, y el lector Core los resuelve como nulo, bool, int o float.

Lo comprobé construyendo `Descriptor` válidos y pasándolos por `desdeYaml(aYaml(d))`:
- **La descripción se pierde sin aviso.** Con `descripcion` igual a `"~"`, `"Null"` o `"NULL"`, el YAML sale `descripcion: ~` y vuelve como **`Exito` con `descripcion=Optional.empty`**. Es un éxito con otro valor, no un fallo.
- **Lo que era válido vuelve como `Fallo DESCRIPTOR_INVALIDO`:**
  - en `descripcion`: `"True"`, `"TRUE"`, `"False"`, `"007"`, `"0o17"`, `"0x1F"`, `"+1"`, `"-.Inf"` y `".NaN"`;
  - en el valor de etiqueta `cliente`: `"~"`, `"007"`, `"0x1F"`, `"True"` y `"+51"`.

  Un código de cliente `"007"` es un caso realista.

En total fallan 17 de 28 textos. Con el `LectorDescriptor` de 4c1a396 (ronda 1), compilado aparte en mi scratchpad, los 28 vuelven iguales. Es una regresión de esta ronda.

La prueba `idaYVuelta` no lo ve porque solo usa `pais: pe` y `'a: b'`. La propiedad general con jqwik es de U01-T07, pero el contrato de `aYaml` («el YAML que desdeYaml lee de vuelta») sí es de esta tarea.

Una corrección posible, que no apliqué a su código: `DumpSettings.builder()…setSchema(new CoreSchema())`. Con eso, los diez textos que probé (`~`, `Null`, `True`, `007`, `0o17`, `0x1F`, `+1`, `-.Inf`, `.NaN`) salen entre comillas y `hola` sale sin ellas. Hace falta además una prueba de ejemplo con al menos `~` en `descripcion` y `007` en una etiqueta.

### F-08 · AMARILLO · LectorDescriptor.java:172-180 (`etiquetaExplicitaNoStr`) · Una etiqueta explícita que coincide con la implícita no se detecta
`version: !!int 1` da `VERSION_INVALIDA` y `coreCompatible: !!int 2` da `RANGO_INVALIDO`, no `DESCRIPTOR_INVALIDO` como dice el arbitraje. El código compara contra lo que YAML resolvería solo y no puede distinguir `!!int 1` de `1`.

El resultado sigue siendo `Fallo` y no encontré ningún `Exito` indebido. Basta con documentarlo en R-V3 o en el javadoc.

### F-09 · AMARILLO · LectorDescriptor.java:293 (`etiquetas`) · Las claves de etiqueta no tienen la regla de tipos que tiene `nombre`
`etiquetas: {1: pe}` y `{true: pe}` dan `Exito` con las claves `"1"` y `"true"`, mientras que `nombre: 123` da `DESCRIPTOR_INVALIDO`. El arbitraje no cubre las claves, así que no bloquea. Conviene decidir la regla y escribirla en R-V3.

## Tareas candidatas (defectos reales fuera de alcance)
- La que dejé en la ronda 1 sigue en pie: los mensajes de `leer` incluyen la ruta absoluta del sistema de archivos (`leer-dir` muestra `/tmp/h…/dir.jar`). Conviene revisarlo cuando se exponga por la API (U04).
- Para U01-T07: el generador de jqwik debería incluir textos que en YAML 1.2 Core se resuelven como nulo, bool, int o float (`~`, `True`, `007`, `0x1F`, `.NaN`). F-07 muestra que es justo ahí donde se rompe.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r2/ca1.txt, ca3.txt y ca4.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r2/Hostil.txt y Hondo.txt (las entradas hostiles de la ronda 1, repetidas)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r2/Ronda2.txt (sondas nuevas: ida y vuelta, tipos y profundidad; el código está en `src/Ronda2.java`)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r2/viejo/ (el lector de 4c1a396 compilado aparte para comparar)

```
VEREDICTO: NO-VERDE
NARANJA|LectorDescriptor.java:326 (aYaml)|Dump con esquema JSON y carga con CoreSchema: ida y vuelta rota (descripcion "~" se pierde; "007", "True" y otros pasan a DESCRIPTOR_INVALIDO), regresión de esta ronda
INFORME: revisiones/U01-T06/ronda-2.md
```

No escribí `revisiones/U01-T06/ronda-2.md` porque mis herramientas son de solo lectura; el informe completo es este mensaje.
