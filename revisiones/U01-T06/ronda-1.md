# Ronda 1 — U01-T06

VEREDICTO: NO-VERDE

Los cuatro criterios pasan cuando los corro yo, pero mis propias entradas hostiles contra el modo seguro encontraron tres defectos NARANJA en pie. El más serio: un YAML de unos 4 KB tumba al lector con `StackOverflowError`.

Contexto: worktree `/home/user/wt-m6r-U01-T06`, rama `m6r/U01-T06` en 4c1a396, base b18d6e3. La rama `modulo-6-rehecho` no existe en este repositorio, así que usé la base que pasó el orquestador (merge-base = b18d6e3). El hash de la tarea coincide (`git hash-object` da 160f039…). El worktree sigue limpio después de la revisión (`git status --short` vacío).

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del lector, sin caché | `./gradlew :verificador:test --rerun --tests '*LectorDescriptorTest'` | pasa: BUILD SUCCESSFUL en 10 s. El XML se escribió a las 02:16:13, un segundo antes de terminar mi corrida, así que es fresco |
| 2 | Reporte: tests ≥ 10, nada saltado | `grep -ho 'tests=…' verificador/build/test-results/test/TEST-*LectorDescriptorTest.xml` | pasa: `tests="18" skipped="0" failures="0" errors="0"`. Los tiempos por caso van de 2 a 63 ms, no son ceros. Cada caso mínimo del CA-2 tiene su prueba con el código exacto |
| 3 | Sin Spring ni Fabric8 en ejecución | `./gradlew :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | pasa: `0`. La única dependencia es `org.snakeyaml:snakeyaml-engine:3.2` |
| 4 | Todo el verificador, sin caché | `./gradlew :verificador:check --rerun` | pasa: BUILD SUCCESSFUL. LectorDescriptorTest 18, RangoVersionTest 36 y VersionTest 16, todas con skipped, failures y errors en 0 |

Alcance: el diff toca solo `Descriptor`, `LectorDescriptor`, `CodigoMotivo` (+`DESCRIPTOR_INVALIDO`), la fila nueva en domain-entities.md, la prueba y la bitácora. No hay desborde. El rojo primero quedó registrado en la bitácora (42 errores de compilación).

## Hallazgos

### F-01 · NARANJA · LectorDescriptor.java:85-104 (`desdeYaml`) y `leer` · Un YAML anidado de 4 KB lanza `StackOverflowError`
La nota de la tarea pide que `leer` y `desdeYaml` nunca lancen excepción por culpa del archivo, y el javadoc dice «Nunca lanza». Pero el `catch (RuntimeException)` no atrapa `Error`, y snakeyaml-engine 3.2 no tiene un ajuste de profundidad máxima. Lo comprobé con `LoadSettingsBuilder` en javap: los únicos ajustes son `setAllowDuplicateKeys`, `setAllowRecursiveKeys`, `setMaxAliasesForCollections`, `setCodePointLimit`, `setAllowNonScalarKeys` y `setUseMarks`.

Lo corrí con la pila por defecto de la JVM (ThreadStackSize = 1024 KB), sobre un descriptor válido más `extra: [[[…]]]`:
- 500 y 1000 niveles (2 KB) devuelven `Fallo`.
- **2000 niveles (4067 caracteres) lanzan `java.lang.StackOverflowError`**, y lo mismo pasa con todas las profundidades mayores, hasta 32000.
- Pasa igual con mapas anidados (`{a: {a: …}}`) y por `leer` desde un JAR.

Esto deja abierta una denegación de servicio con un archivo muy por debajo del límite de 64 KB. Falta un límite de profundidad propio (contando eventos del parser antes de componer, o atrapando `StackOverflowError` y devolviendo `DESCRIPTOR_INVALIDO`) y una prueba que lo cubra.

### F-02 · NARANJA · LectorDescriptor.java:87-91 · `setAllowDuplicateKeys(false)` y `setAllowRecursiveKeys(false)` no tienen efecto: se aceptan claves duplicadas
Ese código solo usa `Composer` y nunca el `Constructor`, que es la capa de snakeyaml-engine que aplica esos dos ajustes. En el bytecode de `Composer`, lo único que se lee de la configuración es `getAllowNonScalarKeys`. Resultado de mis corridas:
- `nombre: kyc\nnombre: otro\n…` da `Exito[nombre=otro]`: la última clave gana en silencio.
- `etiquetas: {pais: pe, pais: mx}` da `Exito[etiquetas={pais=mx}]`.

La bitácora afirma «sin claves recursivas ni duplicadas», y para las duplicadas eso es falso. Las claves recursivas sí se rechazan, pero por otra razón: `? &a [*a]` cae por `AllowNonScalarKeys`, que viene así por defecto. Hay que detectar los duplicados en `validar` y en `etiquetas` (como `DESCRIPTOR_INVALIDO`, sumado a los demás motivos), probarlo, y quitar los ajustes que no hacen nada o documentar por qué están.

### F-03 · NARANJA · LectorDescriptor.java:195-205 (`texto`) y 228-229 (`etiquetas`) · Se ignora la etiqueta resuelta de los escalares: un nulo se vuelve el texto «~» o «null»
`texto()` devuelve `s.getValue()` sin mirar el `Tag`. En los campos obligatorios el nulo sí se trata como campo faltante, pero en los opcionales no. Lo que obtuve:
- `descripcion: ~` da `descripcion=Optional[~]`.
- `descripcion: null` da `Optional[null]`, es decir, el texto «null».
- `etiquetas: {pais: ~}` da `etiquetas={pais=~}`.

Así, un descriptor con `descripcion: ~` no es igual al mismo descriptor sin ese campo.

La misma clase cubre los valores del tipo equivocado. La fila nueva de domain-entities.md dice que «un valor del tipo equivocado» es `DESCRIPTOR_INVALIDO`, pero `nombre: true` y `nombre: 123` dan `Exito`, y `version: !!int 1.0.0` también da `Exito`.

Corrección pedida, para todos los escalares a la vez: `descripcion` y los valores de etiqueta exigen `Tag.STR`, y un nulo explícito en `descripcion` cuenta como ausente. En `nombre` y `core`, decidir si un bool o un int es un tipo equivocado; en `version: 2.1` se conserva `VERSION_INVALIDA`, como pide el CA-2. Cada regla con su prueba.

### F-04 · NARANJA · LectorDescriptor.java:38-39 · Límites nuevos que ve el usuario, incrustados como literales y sin respaldo en el diseño
`MAX_BYTES = 64 * 1024` aparece en el mensaje al usuario («supera el tamaño máximo de 65536 bytes»), y `MAX_ALIAS = 10` también. Ninguno de los dos valores está en business-rules.md, en domain-entities.md ni en el plan de generación. La tarea pide que haya límites, pero no fija sus valores. Deben quedar documentados en R-V3 (o en el artefacto que corresponda) y referenciados desde ahí. Esto es la invariante 8 de mi contrato.

### F-05 · AMARILLO · LectorDescriptorTest.java `aliasRecursivo` · La prueba no demuestra lo que dice su nombre
`nombre: &a [*a]` falla porque `nombre` es una secuencia («debe ser un texto»), no porque se detecte la recursión. El código tampoco la detecta: `extra: &a [*a]` solo da `DESCRIPTOR_CAMPO_DESCONOCIDO`. Hoy es seguro, porque todos los campos válidos son escalares o mapas de escalares, pero conviene afirmar el mensaje o documentar por qué basta con eso.

### F-06 · AMARILLO · LectorDescriptor.java:155-158 · Código muerto
`if (!OBLIGATORIOS.contains(nombre)) continue;` nunca se cumple, porque la lista literal repite `OBLIGATORIOS`. Además, `claveDeEtiquetaInvalida` solo comprueba `m.get(0)`, sin mirar el tamaño ni el campo.

Lo que funciona en modo seguro, con mis entradas:
- **Etiquetas:** `!!java.net.URL`, `!!javax.script.ScriptEngineManager` en la raíz, la forma verbatim `!<tag:…>`, `%TAG` con prefijo, `!!binary`, una etiqueta en la clave y una etiqueta en un campo desconocido dan todos `DESCRIPTOR_INVALIDO`.
- **Alias:** las «mil carcajadas» (con 10 y con 30 alias) devuelven `Fallo` por superar el límite de alias.
- **Formato del YAML:** varios documentos y el merge `<<` devuelven `Fallo`; con BOM se lee bien.
- **Por `leer`:** un archivo que no es ZIP, un directorio y bytes que no son UTF-8 devuelven `Fallo`; una bomba ZIP (48 KB que se inflan a 50 MB) se corta a 64 KB; un archivo en una subcarpeta da `DESCRIPTOR_SIN_ARCHIVO`.
- **Ida y vuelta:** con texto multilínea, espacios dobles y `#`/`:`, funciona.

## Tareas candidatas (defectos reales fuera de alcance)
- Los mensajes de error de `leer` incluyen `e.getMessage()` con la ruta absoluta del sistema de archivos. Conviene revisarlo cuando el lector se exponga por la API (U04).

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r1/ca1.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r1/ca3.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r1/ca4.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r1/hostil.txt (todas las entradas hostiles; fuente en `src/Hostil.java`)
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/t06r1/hondo.txt (umbral de profundidad; fuente en `src/Hondo.java`)

```
VEREDICTO: NO-VERDE
NARANJA|LectorDescriptor.java:85-104|YAML anidado de 4 KB lanza StackOverflowError (sin límite de profundidad)
NARANJA|LectorDescriptor.java:87-91|setAllowDuplicateKeys/RecursiveKeys inertes con Composer: claves duplicadas aceptadas
NARANJA|LectorDescriptor.java:195-229|Etiqueta del escalar ignorada: nulo en descripcion/etiquetas pasa como texto "~"/"null"
NARANJA|LectorDescriptor.java:38-39|Límites 64 KB y 10 alias incrustados, visibles y sin respaldo en el diseño
INFORME: revisiones/U01-T06/ronda-1.md
```

No escribí el archivo `revisiones/U01-T06/ronda-1.md`, porque mis herramientas son de solo lectura; el informe completo es este mensaje.
