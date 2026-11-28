# Ronda 1 — U02-T09

VEREDICTO: NO-VERDE

Los cuatro criterios pasan cuando los corro yo, el rojo primero se reproduce y no hay desborde de alcance. Lo que bloquea es un NARANJA: las pruebas de rechazo no comprueban que el motivo diga qué falló (Decisión 2), y por eso pasaron también contra el stub.

## Criterios de aceptación, verificados por mí
Todo corrió en `/home/julian/Escritorio/Proyectos/wt-m6r-U02-T09`, con HEAD en `67ddc62` y JAVA_HOME en el JDK 21 aarch64 de Gradle.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas del plugin y descriptor válido para U01 | `./gradlew :plugins:kyc-co:test --rerun :plugins:kyc-co:check :plugins:kyc-co:jar`, luego el `grep` del XML, `unzip -p` y `unzip -l \| grep -c` | **pasa**: BUILD SUCCESSFUL, `tests="14" skipped="0" failures="0" errors="0"`. El yaml trae `version: 1.0.0`, `coreCompatible: 2.x` y `pais: co`. El conteo de `io/synkra/plugin/` da `0`. El JAR solo lleva `PluginKycCo.class`, el archivo de services, el yaml y el manifest. `runtimeClasspath` da "No dependencies" (Decisión 1). |
| 2 | El core real lo carga y lo usa por HTTP | `./gradlew :core:bootJar :plugins:kyc-co:jar`, `java -jar core-2.1.0.jar --server.port=18083 --synkra.core.config=$D/config --synkra.core.plugins=$D/plugins` y espera con `timeout 90` hasta que `/actuator/health/readiness` responde | **pasa**: readiness `{"status":"UP"}`; `/plugins` devuelve `[{"nombre":"kyc-co","version":"1.0.0"}]`; `1234567` da `aprobado:true`; `12ab` da `aprobado:false` con el motivo de longitud. Probé además `12ab56` y `١٢٣٤٥٦`: los dos se rechazan con "solo admite dígitos ASCII". El log dice `Plugins cargados: [kyc-co]`. Maté el proceso, el puerto quedó libre y borré el temporal. |
| 3 | El resto del build sigue pasando | `./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check` | **pasa**: BUILD SUCCESSFUL en 28 s. Conteos sumados de los XML: verificador 178/0/0/0, plugin-api 4/0/0/0 y core 54/0/0/0 (tests/skipped/failures/errors). |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat d99cf36...HEAD` | **pasa**: sha `67ddc62`, 12 archivos y 270 inserciones. Fuera de `plugins/kyc-co/**` y `bitacoras/U02-T09*` solo está la línea `+include("plugins:kyc-co")` de `settings.gradle.kts`. No hay nada en `core/`, `verificador/`, `plugin-api/` ni `backend/`. |

**Rojo primero, reproducido por mí.** Extraje el commit `84ada75` con `git archive` a un directorio de `mktemp -d` y corrí `./gradlew :plugins:kyc-co:test --rerun`: `14 tests completed, 5 failed` (las 3 de aprobación, la del descriptor y la de ServiceLoader), BUILD FAILED. Coincide con `rojo.txt`. Entre el rojo y el verde la prueba solo cambió el `package` (de `io.synkra.plugin.kycco` a `io.synkra.kycco`), así que las aserciones no se aflojaron.

**Edad de la evidencia.** `ca1.txt` y `rojo.txt` entraron con el código. Los commits posteriores a `8daa777` (`8ee941b` y `67ddc62`) solo tocan `bitacoras/`. La evidencia es compatible con el diff actual.

**Tiempos.** Las 14 pruebas son unitarias puras, entre 0 y 34 ms. La única que depende de algo externo, el JAR construido, lo tiene garantizado por `dependsOn(jar)` y se ejecuta (0.034 s). No hay saltos.

## Hallazgos

### F-01 · NARANJA · plugins/kyc-co/src/test/java/io/synkra/kycco/KycCoTest.java:35-48 · Las pruebas de rechazo no comprueban qué dice el motivo
La Decisión 2 pide que, al rechazar, el `motivo` diga qué falló. `rechazaConMotivo` y `rechazaSolicitudNulaSinLanzar` solo comprueban `assertFalse(r.motivo().isBlank())`. Prueba de que no discriminan: contra el stub de `84ada75`, que devuelve siempre `"sin implementar"`, esos 8 casos **pasaron**. Las 5 que fallaron eran las de aprobación, descriptor y ServiceLoader. Es decir, las pruebas de rechazo nunca estuvieron en rojo y seguirían en verde si el motivo fuera fijo o equivocado. Ejemplo: si `١٢٣٤٥٦` se rechazara por longitud en vez de por no ASCII, ninguna prueba lo notaría. La implementación sí lo hace bien (lo vi por HTTP), pero la decisión no tiene cobertura. Lo que pido: separar por clase de fallo (longitud, no ASCII, nula) y comprobar el motivo de cada una, por ejemplo con `@CsvSource` o comparando contra constantes de motivo expuestas por la clase.

### F-02 · AMARILLO · PluginKycCo.java:29,33,38,43,45 · Motivos como literales sueltos
Los cinco mensajes visibles para el usuario son literales en línea. Los límites sí son constantes. Si se resuelve F-01 con constantes de motivo, esto queda resuelto de paso.

### F-03 · AMARILLO · PluginKycCo.java:32-34 · Rama de documento nulo inalcanzable
`SolicitudKyc` ya hace `requireNonNull(documento)` en su constructor, así que `d == null` es código muerto. El codificador lo anota bien en la bitácora y en el Javadoc de la prueba. Se puede quedar como defensa, pero conviene un comentario en el código. Lo pedido en la Decisión 2 (que no lance) queda cubierto con la solicitud nula.

### F-04 · AMARILLO · KycCoTest.java:68-72 · La prueba del descriptor no comprueba `coreCompatible`
Comprueba `nombre`, `version`, `core` y `etiquetas`, pero no `coreCompatible: 2.x`. Hoy lo cubre el `unzip -p` del CA-1, que es manual.

### F-05 · AMARILLO · bitacoras/U02-T09-evidencia/ca4.txt · Falta el sha del CA-4
El CA-4 pide anotar el sha y `ca4.txt` solo tiene el `--stat`. La bitácora dice "ver el final del informe/commit". Lo comprobé yo: `67ddc62`.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Estado del worktree
Lo dejé limpio: `git status --short` sale vacío y solo aparecen los ignorados `.gradle/`, `build/` y `verificador/.jqwik-database`. Borré los temporales (`mktemp -d` de CA-2 y la copia del rojo), y el core de CA-2 quedó muerto con el puerto 18083 libre.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca1.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.log

No escribí el informe en disco porque no tengo herramientas de escritura. El orquestador debe guardar este texto como `revisiones/U02-T09/ronda-1.md`.

```
VEREDICTO: NO-VERDE
NARANJA|plugins/kyc-co/src/test/java/io/synkra/kycco/KycCoTest.java:35-48|Las pruebas de rechazo no comprueban qué dice el motivo (pasaron contra el stub)
INFORME: revisiones/U02-T09/ronda-1.md
```
