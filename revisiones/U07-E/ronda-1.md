# Ronda 1 — U07-E

VEREDICTO: NO-VERDE

Los siete criterios pasan con mis propias ejecuciones. Un solo hallazgo NARANJA bloquea: seis reglas de la decisión 2 no tienen ninguna prueba que muerda, y lo demuestran siete mutaciones que sobreviven. Hay además dos AMARILLO.

Comprobaciones previas:
- Hash de la tarea: 4252363a54786bae39c5ed75ae4ed6cd8c27c629, coincide.
- Worktree en 2d9d21f, limpio antes y después de la revisión.
- El diff es `39a242c...m6r/U07-E`.
- Entorno: JDK 21 de `~/.gradle/jdks`, solo el simulador, sin clúster, python3 -I, puerto 18094 libre al empezar y al terminar.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Vigilancia sin escrituras | Comandos literales de CA-1 (`--rerun`; la tarea de test se ejecutó) | **pasa**: `tests="8" skipped="0" failures="0" errors="0"`, 8 testcase (uno por país), las dos listas de Python exactas, `[(0, 0, 2)]` |
| 2 | Retomar tras reinicio y C-109 | Comandos literales de CA-2 | **pasa**: `tests="2"`, 0 fallos, `time="5.172"` (Awaitility real, no se saltó nada); las tres listas exactas; `sleep=1` |
| 3 | C-76, C-82, C-83, C-116; U05 y U06 igual | Comandos literales de CA-3 | **pasa**: Reintentos 3, Rollout 6, Eventos 3, Aplicando 1, EstadoYContexto 20, ClienteKubernetes 10, Aplicar 10, Historial 8, todas con 0/0/0; una sola línea `40: .withRequestRetryBackoffLimit(0)`; lista 41 a 45 exacta |
| 4 | El JAR real sin clúster | `bootJar` y luego el guion literal de CA-4 (con `$JAVA_HOME/bin/java`, mktemp en mi scratchpad) | **pasa**: `espera=0` (ya cerrado al primer sondeo); `91 fallido {… 'Venció el plazo …\nNo se pudo leer …'}`; `92 aplicando None`; `[(91,'confirmado'),(92,'confirmado'),(93,'vencido'),(94,'pendiente')]`; `[(91,'fallido'),(92,'aplicando')]`. El JAR no trae `config/application.properties` (`unzip -l … \| grep -c` = 0). No queda ningún proceso vivo |
| 5 | Rojo primero, cada regla muerde | 16 mutaciones mías en copias (`git archive 4869522` + `git init -q`) y el rojo reproducido | **pasa lo que pide el criterio**: las 9 mutaciones de la bitácora que repetí (02, 07, 09, 10, 11, 13, 15, 19 y la del simulador) dan BUILD FAILED en la prueba de su regla. Rojo: el main de 8118b23 con las pruebas **finales** da 22/23 en rojo (solo `conTodosLosCamposCoherentesEsListo` en verde, como declara la bitácora). Pero 7 mutaciones mías sobreviven: ver F-01 |
| 6 | Todo el backend; la vigilancia solo lee | Comandos literales de CA-6 | **pasa**: BUILD SUCCESSFUL, 57 XML todos 0/0/0; `aplicar/construir` solo en `ConfirmacionServicio.java`; solo `contextoFallo`, `disponible`, `estado` y `leerPais`; `registro-interno=1`, `tablas-reg=1`; scheduling solo en `TareasProgramadas.java`, con `2`; `vigilancia-tx=1`, `requiresNew=1`; `Reloj.java` y `RegistroServicio.java`; `synkra.programacion.activa=false` |
| 7 | Diff acotado | Comandos literales de CA-7 | **pasa**: solo las rutas permitidas (más `LecturasDePrueba.java`, ayudante nuevo `A` que la tarea admite); la tercera orden da `0`; nada de `plataforma` ni de `catalogo`. Estructura de commits correcta: 8118b23 rojo, 4869522 código, 169c28b evidencia, 2d9d21f solo `ca7.txt` y la bitácora |

Puntos que pediste revisar, todos bien:
- **R-D10, sin reintentos:** la mutación sobre `withRequestRetryBackoffLimit` tumba las 3 pruebas de Reintentos (un GET y un PATCH por fallo).
- **R-D12, retomar al arrancar:** quitar `@EnableScheduling` cae en Retomar. No hay estado en memoria: cada pasada parte de `registro.aplicando()`.
- **C-82:** una razón de fallo de cualquier pod gana a las réplicas listas. La regla de la generación muerde.
- **C-83:** las 3 pruebas de eventos caen con la mutación.
- **C-109:** quitar `vencerPendientes` cae.
- **C-115:** lo prueban `bo` y `py`.
- **C-116:** quitar el filtro de estado cae en Aplicando y en Vigilancia.
- **Programación:** apagada en pruebas (`config/` de test) y encendida por defecto en el JAR (CA-4).
- **Sin `sleep`:** no hay ningún `Thread.sleep` en las pruebas nuevas.
- **U05 intactas:** `EstadoYContextoTest` no está en el diff y da 20/20.

## Hallazgos

### F-01 · NARANJA · `VigilanciaDespliegues.java:62-65, 90, 102-106, 147-152` · Seis reglas de la decisión 2 sin una prueba que muerda
Es una sola clase de defecto, barrida entera. Las cinco primeras mutaciones dan `exit=0`, con las seis clases en verde:
- **A, aislamiento por fila**: borrar `catch (RuntimeException e)` y su `LOG.warn` (líneas 64-65) no hace caer nada.
- **F, solo la imagen**: `if (!d.imagen().equals(corre.imagenCore()))` cambiado a `if (false)` no hace caer nada.
- **G, solo los plugins**: `return confirmados.equals(...)` cambiado a `return true` no hace caer nada. Es lo más grave del grupo: un país que corre la imagen confirmada con **otros plugins o con otro sha** se daría por `listo` sin que ninguna prueba lo note. Va contra C-82 y contra la North Star. La mutación 02 de la bitácora muerde solo porque quita las dos comparaciones a la vez: `bo` difiere en imagen **y** en plugins.
- **I y J, contexto al plazo**: dejar fuera `"\n" + k.eventos()` (línea 102), o dejar en `""` el `logCore` y el `logInit` que vienen de `contextoFallo` (línea 106), no hace caer nada. Ningún caso vence con eventos o logs en el país.
- **B, desviación 4 del codificador (FALLIDO con contexto ilegible)**: cambiar `situacion = Situacion.SIN_LECTURA;` por `return false;` (la fila no se cerraría nunca) no hace caer nada.
- **C, `TransicionNoPermitidaException` se salta**: relanzarla tampoco hace caer nada. Hoy equivale casi del todo al `catch` general, así que es la de menor prioridad.

Transcripciones en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-E-r1/mut/{A,B,C,F,G,I,J}-*.txt`.

**Cómo probarlo sin cambiar la API del simulador.** `AdaptadorKubernetes` es una interfaz.
- F, G y una variante de solo-sha: bastan casos nuevos en `VigilanciaTest` con `SIM.siembra` + `SIM.marcaListo`:
  - imagen `a` con `kyc-co` (solo cambian los plugins);
  - imagen `b` con `kyc-pe` 1.3.0 (solo cambia la imagen);
  - imagen `a` con `kyc-pe` 1.3.0 y otro sha.
  Cada uno se registra con `registro.registrar`, como 950 y 951: 0 antes del plazo y `PLAZO\nNO_APLICADO` al plazo.
- A, B, I, J (y C si se quiere): una clase aparte, p. ej. `VigilanciaDobleTest`, con su propia carpeta y un `@TestBean AdaptadorKubernetes` que devuelve un doble que delega en `SIM.adaptador()` y sobrescribe por país:
  - (A) `leerPais("xx")` lanza `IllegalStateException`, y la fila más reciente de otro país igual se cierra en esa pasada;
  - (B) `contextoFallo("synkra-yy")` lanza `LecturaFallidaException` con `estado` FALLIDO: 0 y `aplicando` a los 30 s, `PLAZO\nSIN_LECTURA` al plazo;
  - (I y J) `estado` da EN_CURSO y `contextoFallo` devuelve `("lc","li","Warning X: y")`: al plazo, `("lc","li", PLAZO + "\nWarning X: y")`.

La regla B la inventó el codificador para cubrir un hueco de la especificación, y me parece correcta: el texto literal dejaría la fila abierta para siempre. Conviene que el orquestador la ratifique.

### F-02 · AMARILLO · `bitacoras/U07-E.md`, «Cambio (commit 4869522)» · No dice que el commit verde tocó pruebas
4869522 cambia `EstadoRolloutTest` (firma de `deployment` y los valores de la generación), `LecturasDePrueba` y `SimuladorKubernetes` (+24: el `remove` y los 400 a los logs). La bitácora solo enumera el main. No rompe el rojo: comprobé que las pruebas finales también caen contra el main de 8118b23 (`/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-E-r1/mut/rojo-final.txt`).

### F-03 · AMARILLO · Desviaciones 1 a 3 y 5, juzgadas
- **1, el `remove` en `fallaLecturas`:** aceptable. Son solo líneas añadidas y hacen falta: quitarlas tumba `ecSinLecturaNoCierraNadaHastaElPlazo` (`/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-E-r1/mut/D-sim-sin-remove.txt`). Los otros llamadores (`ConfirmarTest`, `CrearPlanTest`) siguen en verde dentro de los 57 XML.
- **2, los 400 a los logs no servidos:** aceptable, sigue el patrón de `logAunNoExiste`.
- **3, `observedGeneration` 0 con `generation` 1:** misma regla (`observada < generation`). La prueba afirma antes que la generación es 1, y la mutación R muerde.
- **5, el glob de CA-1:** es un defecto de la plantilla, no del codificador. Se puede sumar a C-122.

## Tareas candidatas (defectos reales fuera de alcance)
- El literal `"synkra-" + pais` ya está en tres sitios (`VigilanciaDespliegues:156`, `PaisPermitido:59`, `AdaptadorFabric8:109`). La vigilancia no puede usar `PaisPermitido`, porque `aplicando()` ignora `synkra.paises` a propósito. Unificar en `plataforma` junto con C-110.
- Una fila cuyo `leerPais` lanza siempre una `RuntimeException` no listada se registra y se salta en cada pasada, y nunca vence. Lo pide la decisión 2 tal como está escrita, pero esa fila queda `aplicando` para siempre. Valorar que el plazo la cierre también. Va con la regla B de F-01.
- La javadoc de `AdaptadorKubernetes` ya está registrada como C-124; no la repito.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-E-r1/`:
- Criterios: `ca1.txt`, `ca2.txt`, `ca3.txt`, `ca4.txt`, `ca6.txt`
- Mutaciones: `mut/*.txt` (A-J y K-T); guiones: `mut.sh`, `todas.sh`, `rojo.sh`, `ca4.sh`

No pude escribir el informe en `revisiones/U07-E/ronda-1.md`, porque no escribo en el worktree. Este mensaje es el informe completo para que el orquestador lo persista.

```
VEREDICTO: NO-VERDE
NARANJA|VigilanciaDespliegues.java:62-65,90,102-106,147-152|Seis reglas de la decisión 2 sin una prueba que muerda (aislamiento, solo imagen, solo plugins, contexto al plazo, FALLIDO con contexto ilegible, TransicionNoPermitida)
INFORME: revisiones/U07-E/ronda-1.md
```
