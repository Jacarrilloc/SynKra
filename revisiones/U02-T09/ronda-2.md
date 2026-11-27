# Ronda 2 — U02-T09

VEREDICTO: VERDE

Volví a correr los cuatro criterios en `/home/julian/Escritorio/Proyectos/wt-m6r-U02-T09` (HEAD `1687d65`, JDK 21 aarch64 de Gradle) y todos pasan. Los cinco hallazgos de la ronda 1 quedan cerrados. Para F-01 no me quedé con el par falla-pasa del codificador: hice una mutación mía que esas pruebas antes no habrían detectado. No queda ningún ROJO ni NARANJA. No pude escribir el informe en disco porque no tengo herramientas de escritura, así que el orquestador debe guardar este texto como `revisiones/U02-T09/ronda-2.md`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del plugin y descriptor válido para U01 | `./gradlew :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-co:jar`, después el `grep` de los XML, `unzip -p` y `unzip -l \| grep -c` | **pasa**: BUILD SUCCESSFUL, `tests="17" skipped="0" failures="0" errors="0"`. Están todos los casos que pide el criterio: se aprueban 6, 7 y 10 dígitos. Se rechazan por longitud `12345`, `12345678901`, `""` y `1`. Se rechazan por no ASCII `12ab56`, `١٢٣٤٥٦`, `123 456`, `-123456` y `123456a`. También están la solicitud nula, el descriptor leído del JAR con `LectorDescriptor`, ServiceLoader, el nombre y los motivos distintos. El yaml trae `pais: co` y `coreCompatible: 2.x`. El conteo de `io/synkra/plugin/` da `0`. El JAR tiene 9 entradas: la clase, el archivo de services, el yaml y el manifest. |
| 2 | El core real lo carga y lo usa por HTTP | `./gradlew :core:bootJar :plugins:kyc-co:jar`. Luego `java -jar core-2.1.0.jar --server.port=18083 --synkra.core.config=$D/config --synkra.core.plugins=$D/plugins`, esperando con `timeout 90` a que la readiness responda | **pasa**: readiness `{"status":"UP"}`. `/plugins` da `[{"nombre":"kyc-co","version":"1.0.0"}]`. `1234567`, `123456` y `1234567890` dan `aprobado:true`. `12ab` y `12345678901` dan `aprobado:false` con el motivo de longitud. `12ab56` y `١٢٣٤٥٦` dan `aprobado:false` con el motivo "solo admite dígitos ASCII". El log dice `Plugins cargados: [kyc-co]`. Maté el proceso, el puerto 18083 quedó libre y borré el temporal. |
| 3 | El resto del build sigue pasando | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | **pasa**: BUILD SUCCESSFUL en 29 s. Conteos de los XML (tests/skipped/failures/errors): verificador 178/0/0/0, plugin-api 4/0/0/0, core 54/0/0/0. |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat d99cf36...HEAD` | **pasa**: sha `1687d65`, 15 archivos y 381 inserciones. Solo hay un archivo fuera de `plugins/kyc-co/**`, `bitacoras/U02-T09*` y `revisiones/U02-T09/`: `settings.gradle.kts`, con una sola línea, `+include("plugins:kyc-co")`. |

**Mutación propia para F-01.** Extraje `1687d65` con `git archive` a un `mktemp -d`. Ahí cambié el retorno de la rama no ASCII para que devolviera `MOTIVO_LONGITUD`, que es justo el defecto que la ronda 1 decía que nadie notaría. Resultado: `17 tests completed, 5 failed`, las cinco de `rechazaPorNoAscii`. Borré el temporal.

**Par falla-pasa del codificador.** `rojo-r2.txt` muestra 13 fallos de 17. Eso es lo que da un stub que siempre devuelve `false` con un motivo fijo: fallan las 3 de aprobación, las 4 de longitud, las 5 de no ASCII y la de solicitud nula. Las 4 que pasan son las que no dependen de la regla. Es coherente con el código actual.

**Edad de la evidencia.** El código cambió por última vez en `d72da9a`. Los commits que vienen después (`9216337` y `1687d65`) solo tocan `bitacoras/` y `revisiones/`. `ca4.txt` dice "HEAD al medir: 9216337" y anota el sha de código `d72da9a`. Va un commit atrás de HEAD, lo cual es inevitable porque commitear la evidencia mueve HEAD. Es compatible con el diff actual. Mi propia corrida da `1687d65`.

**Tiempos.** Son pruebas unitarias puras, de 0 a 30 ms cada una. La del descriptor depende del JAR construido: corrió en 0.03 s y no se saltó.

## Cierre de los hallazgos de la ronda 1
- **F-01 · NARANJA · cerrado.** La prueba se separó en `rechazaPorLongitud`, `rechazaPorNoAscii` y `rechazaSolicitudNulaSinLanzar`, cada una con `assertEquals` contra la constante de motivo. La mutación de arriba confirma que ahora sí distinguen los motivos.
- **F-02 · AMARILLO · cerrado.** Los motivos son ahora constantes con nombre (`MOTIVO_*`) en `PluginKycCo.java:15-20`.
- **F-03 · AMARILLO · cerrado.** Hay un comentario de defensa en la rama del documento nulo (`PluginKycCo.java:39`).
- **F-04 · AMARILLO · cerrado.** La prueba del descriptor comprueba `coreCompatible().texto()` igual a `2.x` (`KycCoTest.java:91`).
- **F-05 · AMARILLO · cerrado.** `ca4.txt` anota el sha, con la salvedad de edad explicada arriba.

## Hallazgos
### F-06 · AMARILLO · plugins/kyc-co/src/test/java/io/synkra/kycco/KycCoTest.java:58-67 · `losMotivosSonDistintos` deja fuera dos constantes
El conjunto que compara solo tiene `MOTIVO_LONGITUD`, `MOTIVO_NO_DIGITOS` y `MOTIVO_SOLICITUD_NULA`. No incluye `MOTIVO_DOCUMENTO_NULO` ni `MOTIVO_VALIDO`. Además, la rama de documento nulo sigue sin prueba, porque no se puede llegar a ella. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Estado del worktree
Quedó limpio: `git status --short` sale vacío. Borré todos los temporales (la copia de la mutación y el `$D` de CA-2). El core está muerto y el puerto 18083 libre.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2-ca1.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2-ca2-core.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2-ca3.log

```
VEREDICTO: VERDE
AMARILLO|plugins/kyc-co/src/test/java/io/synkra/kycco/KycCoTest.java:58-67|losMotivosSonDistintos no incluye MOTIVO_DOCUMENTO_NULO ni MOTIVO_VALIDO
INFORME: revisiones/U02-T09/ronda-2.md
```
