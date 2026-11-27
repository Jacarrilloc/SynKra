# Ronda 1 — U04-T06

VEREDICTO: NO-VERDE

Corrí yo los cuatro criterios y los cuatro pasan. La atomicidad, la ruta final, R-C9 y la concurrencia también resisten con mis sondas. Lo que bloquea es un solo NARANJA: la regla de que las claves de etiqueta de la petición usen el patrón de U01 sin copiarlo (decisión 5) no está atada por ninguna prueba. Una copia del patrón que admite un guion al final sobrevive a toda la suite. Además dejo cuatro AMARILLO.

## Comprobaciones previas
- **Hash de la tarea:** `9397b8ad71b341398dc53a55129451c873179afa`. Coincide.
- **Commits:** HEAD es `40d0ab3` y la base `origin/main` es `7807d8b`. Son dos commits: `1641ede` (código) y `40d0ab3` (bitácora).
- **Worktree:** `git status --short` salió vacío antes y después.
- **Sondas y mutaciones:** se hicieron en copias `git archive HEAD` del scratchpad.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C4 y R-C7 a R-C10 | `./gradlew :backend:test --rerun --tests '*SubirPluginTest'` + `grep` del XML | pasa: `BUILD SUCCESSFUL`, `tests="28" skipped="0" failures="0" errors="0"`. El XML es de mi corrida (`timestamp` 13:55:20Z) y tarda 3,56 s, con SQLite y disco reales. Están todos los casos que pide la lista. |
| 2 | Descriptor leído con U01 | los dos `grep` | pasa: la llamada está en `SubidaPlugin.java:102` y la segunda línea imprime `sin copia`. |
| 3 | Backend y fronteras | `./gradlew :backend:test --rerun :backend:check :verificador:check` + bucle | pasa: `BUILD SUCCESSFUL` y 16 XML del backend con 0 saltos, 0 fallos y 0 errores. `InterfazCatalogoTest` da 3/3 y `FronterasModulosTest` (Modulith) 3/3. Los 12 XML de `verificador` dan 0/0/0. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 10 archivos, todos bajo `catalogo/**`, las pruebas y la bitácora. Que se toque `RecepcionArtefactoTest` está autorizado por la decisión 7. La salida de la bitácora (111 líneas) se tomó antes del commit de la bitácora, como ella misma dice; ahora son 128. |

## Las desviaciones declaradas
1. **`SubidaPlugin` package-private y `AUTOR` package-private: aceptable.** No sale del módulo, y Modulith e `InterfazCatalogoTest` pasan.
2. **`throws IOException`: aceptable.** Así ya es `RecepcionArtefacto.recibir`. Separa el fallo de disco (500) de los rechazos (`ExcepcionApi`), y M35 lo ata.
3. **Validación de las etiquetas de la petición: aceptable en su forma.**
   - `campo="etiquetas"` es el mismo que usa U01 para una clave inválida.
   - El valor vacío se acepta, igual que en U01, que solo exige un str.
   - El orden por clave lo ata M25.
   - La reutilización del patrón queda sin atar: ver F-01.
4. **Recortes a 200 caracteres y a 50 choques: aceptable.** Es coherente con C-19 y con `recortar` de U04-T04. M26, M27, M38 y M39 caen.
5. **`TransactionTemplate` con pool de 1: aceptable y comprobada.** El acoplamiento queda en F-02.

## Verificación pedida (con mis sondas; `sonda.log`, `sonda2.log`)
- **El 404 no lee el flujo.** La prueba usa un `InputStream` que lanza al leer. M01 y M02 (el 404 después de `recibir`) caen.
- **Motivos de U01.** Salen sin traducir, en su orden y con el tope de 50 más el aviso de omitidos. M03 a M08 caen.
- **R-C8, R-C9 y R-C10.**
  - R-C9 compara el sha256 leído de vuelta del archivo, la fila completa y las etiquetas.
  - Todo esto cae con alguna mutación: M09 a M13, M18 a M22, M37 y M43, que es R-C8 después de R-C9.
- **Atomicidad.** Inyecté un fallo en cada paso. En los cinco quedan 0 filas, ningún archivo regular y nada en `.tmp`:
  - antes de mover: el padre es un archivo y sale `FileAlreadyExistsException`;
  - después de mover: `JdbcAggregateTemplate` simulado que lanza;
  - en el insert del plugin: trigger `raise(abort)` sobre `cat_plugin`;
  - en el insert de la etiqueta: trigger sobre `cat_etiqueta`;
  - en el commit: un `PlatformTransactionManager` que hace rollback y lanza `TransactionSystemException`.

  Solo quedan directorios vacíos (F-03).
- **La ruta final.**
  - U01 rechaza con 422 todos los nombres y versiones hostiles que probé, antes de llegar a la ruta: `../x`, `..`, `a/b`, `A`, un NUL, `1.0.0/../../x`, `../1.0.0`, `1.0.0-rc.1`, `1.0.0+b` y `01.0.0`. `Version` solo admite `X.Y.Z` y `NOMBRE` es `[a-z0-9]+(-[a-z0-9]+)*`, así que no hay ninguna ruta que salga de la raíz.
  - Subir la 1.1.0 no pisa la 1.0.0: los dos sha256 coinciden.
  - `.tmp` y el destino están en el mismo `FileStore`.
- **Concurrencia.** Hice 40 rondas de dos subidas simultáneas de la misma versión con bytes distintos, sincronizadas con una barrera. Salen 40 éxitos y 40 respuestas 409, y en ninguna ronda el sha256 de la fila difiere del del disco.
- **C-44.**
  - Los nombres exactamente repetidos se rechazan (M34 cae).
  - `a/` con `a`, `A.class` con `a.class`, `synkra-plugin.yaml/` con `synkra-plugin.yaml` y `Synkra-Plugin.yaml` se aceptan.
  - Eso no es el defecto de C-44: `ZipFile.getEntry` prefiere la coincidencia exacta, y la sonda lee el descriptor correcto. Un JAR con solo `synkra-plugin.yaml/` da `DESCRIPTOR_SIN_ARCHIVO`. Queda como tarea candidata.

## Hallazgos
### F-01 · NARANJA · `SubidaPlugin.java:168` y `SubirPluginTest.java:480-498` · la regla de reutilizar el patrón de U01 no está atada
- **M23** cambia `Descriptor.NOMBRE` por una copia que admite un guion al final, `[a-z0-9]+(-[a-z0-9]+)*-?`. **Sobrevive** a las 91 pruebas de `SubirPluginTest` y `RecepcionArtefactoTest`.
- La prueba solo usa `Pais`, `-x` y `null`, y la tarea dice que el revisor vigila la copia con mutaciones (C-37).
- La línea `assertThat(Descriptor.NOMBRE.matcher("-x").matches()).isFalse()` prueba U01, no `SubidaPlugin`.
- Para cerrarlo hacen falta claves que solo el patrón real distingue, como `a-`, `a--b`, `á` y `a_b`. Lo mejor es una prueba paramétrica que, para cada clave, compare el resultado de `subirPlugin` con `Descriptor.NOMBRE.matcher(clave).matches()`.

### F-02 · AMARILLO · `SubidaPlugin.java:118-155` · la exclusión entre subidas depende del pool de `BaseDeDatos`, y si falla, borra el archivo del otro
- Con `setMaximumPoolSize(4)` en una copia, la sonda de concurrencia da `NoSuchFileException`: la fila del ganador queda, y su archivo lo borró el perdedor. El perdedor reemplaza el destino con `rename`, falla por PK, y su `catch` borra `destino` (`sonda-pool4.log`).
- Hoy está a salvo, y solo lo vigila de forma indirecta `BaseDeDatosTest.elPoolTieneUnaSolaConexion`, que también cae con ese cambio. Ninguna prueba de esta tarea ata la carrera.
- Una prueba determinista con dos hilos y una barrera, como mi sonda, la convertiría en evidencia.

### F-03 · AMARILLO · `SubidaPlugin.java:128` · directorios vacíos huérfanos
- En los cinco fallos inyectados de después de `createDirectories` quedan `artefactos/<core>/<nombre>/` vacíos.
- No son archivos ni filas, pero se acumulan.

### F-04 · AMARILLO · `SubidaPlugin.java:129` · `ATOMIC_MOVE` sin atar
- **M14**, que cambia `ATOMIC_MOVE` por `REPLACE_EXISTING`, sobrevive. En el mismo volumen se comporta igual.
- Yo comprobé que el destino y `.tmp` están en el mismo volumen. Pero ninguna prueba fijaría que el movimiento sigue siendo atómico.

### F-05 · AMARILLO · `SubidaPlugin.java:163` y el Javadoc de `CatalogoApi.subirPlugin` · un mapa de etiquetas nulo da NPE (500), y el Javadoc contradice al código
- `subirPlugin(core, jar, null)` lanza `NullPointerException: ... "deLaPeticion" is null`. La salida está en `sonda.log`.
- El Javadoc dice «claves y valores no nulos», pero el código sí valida las claves y los valores nulos con un 422.
- Conviene alinearlos: tratar `null` como un mapa vacío o rechazarlo con un 422.

## Mutaciones (44 mutantes, `mutaciones.txt`)
- **Caen 41.** Entre ellos están las 12 sondas que declara la bitácora, aunque mi numeración es distinta.
- **Sobreviven 3:**
  - M14 (F-04);
  - M23 (F-01);
  - M42, que quita la comprobación de nulos de `obtener`. Es **equivalente**: el repositorio devuelve vacío y sigue saliendo el 404.

## Tareas candidatas (defectos reales fuera de alcance)
- **`a/` junto a `a` se acepta.** Pasa también con `synkra-plugin.yaml/` junto a `synkra-plugin.yaml`. El JAR se lee bien, pero la extracción en el init container choca entre el directorio y el archivo. Va con U07, junto a C-46.
- **Los nombres que solo difieren en mayúsculas se aceptan.** En Linux es inocuo, y solo importa si algún día se extrae en un sistema de archivos que no distingue mayúsculas.
- **Los valores de etiqueta de la petición no tienen tope de longitud.** Se guardan enteros en `cat_etiqueta`. Va con U04-T07, con el multipart.
- **C-13 sigue abierta,** como declara la bitácora: `e.getMessage()` puede llegar sin filtrar al motivo.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/sonda.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/sonda2.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/sonda-pool4.log
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/mutaciones.txt
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/scripts/mut.py
- /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/sonda/backend/src/test/java/io/synkra/catalogo/SondaRevisorTest.java

```
VEREDICTO: NO-VERDE
NARANJA|SubidaPlugin.java:168, SubirPluginTest.java:480|la reutilización de Descriptor.NOMBRE para las claves de la petición no está atada (M23 sobrevive)
AMARILLO|SubidaPlugin.java:118-155|la exclusión entre subidas depende del pool de 1; con un pool mayor, el perdedor borra el archivo del ganador
AMARILLO|SubidaPlugin.java:128|quedan directorios vacíos huérfanos tras un fallo después de createDirectories
AMARILLO|SubidaPlugin.java:129|ATOMIC_MOVE sin atar (M14 sobrevive)
AMARILLO|SubidaPlugin.java:163, CatalogoApi.subirPlugin|un mapa de etiquetas nulo da NPE (500), y el Javadoc contradice la validación de nulos
INFORME: revisiones/U04-T06/ronda-1.md
```
