# Ronda 1 — U07-B

VEREDICTO: NO-VERDE

Los siete criterios pasan cuando los corro yo. Queda un NARANJA: una regla del contrato que ninguna prueba vigila. Lo demuestra una mutación que sobrevive.

Hash de la tarea verificado: `git hash-object tareas/U07-B-crear-plan.md` da `b4a557ba82a3eafeaf3924dbfd04a11d90c3962f` y coincide. La punta revisada es 9bc67ae. Entre 09439f4 (el código final) y 9bc67ae solo cambian archivos de `bitacoras/`, así que la evidencia del codificador sí pudo salir de este diff.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Verificación en vivo (R-D2) | el literal de CA-1 | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`, 4,7 s (contexto Spring real, no hay salto); Python `(0,)` y `(4,) (3,)` |
| 2 | Crear plan contra el simulador | el literal de CA-2 | pasa: `tests="9"` sin saltos; las tres salidas de Python son idénticas a las esperadas. Además leí `dep_plan` completa: 3 filas, `anonimo`, `pendiente` |
| 3 | PBT-03 | el literal de CA-3 | pasa: `tests="4"`, cuatro `tries = 1000`. Coberturas: 38 % (mismo nombre, otra versión o hash), 14 % (país vacío), 22 % (sin cambios) |
| 4 | JAR real en el puerto 18090 | el literal de CA-4 (puerto libre, HOME falso) | pasa: `201`×4, `200 RECHAZADA [PLUGIN kyc-pe 1.3.0, CORE synkra-core 3.0.0]`, `422 COMBINACION_RECHAZADA`, `422 PAIS_NO_PERMITIDO`, `503 KUBERNETES_NO_DISPONIBLE`, `(0,)` |
| 5 | Rojo primero y seis mutaciones | `git archive` de las puntas de rojo y de HEAD en copias, con mis propios `sed` | pasa: ver el detalle abajo |
| 6 | Suite completa y fronteras | el literal de CA-6 | pasa: `BUILD SUCCESSFUL`, 38 XML, ninguno con un conteo distinto de 0; `registro=1`, `internos=1`, `fabric8=1`, `escrituras=1` |
| 7 | Diff acotado | el literal de CA-7 | pasa: solo hay rutas de `despliegue/**` y de `bitacoras/U07-B*`; `A	backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java` exacto; las pruebas de U07-A no cambian |

**Detalle de CA-5:**
- **Rojos reproducidos en copias:**
  - en a71818b, `VerificacionVivoTest` da 14 de 15 fallos;
  - en 7a673ef, `CrearPlanTest` da 9 de 9;
  - en f3088ee, `CambiosPropiedades` da 4 de 4.
- **Pruebas sin cambios desde su rojo:** `git diff --stat <rojo> HEAD -- <prueba>` sale vacío en los tres casos.
- **Control sin mutar:** la copia de HEAD queda en verde.
- **Las seis mutaciones del criterio:** M1 a M6 dan `BUILD FAILED`, y cae la prueba de la clase dueña de cada regla:
  - M1: `s4DaLasDosAlternativasEnOrden`, S4 y S5;
  - M2: `s7DaCoreEquivocadoYS6PluginAusente` y S7;
  - M3: `unaCombinacionRechazadaDa422…`;
  - M4: `unPaisNoPermitidoDa422…`;
  - M5: cinco pruebas de `CrearPlanTest`;
  - M6: dos propiedades de `CambiosPropiedades`.
- **Mutaciones extra mías:**
  - M7: `sinCambios` sin `core.igual`. Muerde.
  - M9: el plan no se inserta. Muerde.
  - M10: la cobertura de país vacío al 50 %. Muerde, así que la cobertura de jqwik sí se comprueba.
  - **M8 sobrevive** (es F-01).

## Hallazgos

### F-01 · NARANJA · `PlanServicio.java:65` y `CrearPlanTest` · El orden por nombre de `resumen.plugins` y de `dep_plan.plugins` no tiene prueba
Las decisiones 6.3 y 7 piden que los plugins del resumen y la columna `plugins` vayan ordenados por nombre. Borré en una copia la línea `.sorted(Comparator.comparing(PluginResumen::nombre).thenComparing(PluginResumen::version))` y la corrida dio `BUILD SUCCESSFUL`, con 28 de 28 en verde (`m8.txt`).

No es una mutación equivalente. `Seleccion` no ordena: en `verificador/.../Seleccion.java` solo hay un `List.copyOf`. Por eso el resumen sale en el orden de la petición.

La causa es que el único caso con dos plugins, `co`, ya los pide en orden (`kyc-co`, `kyc-pe`). `CalculoCambios` sí ordena sus listas, y la propiedad lo cubre; el resumen y la columna `plugins` no. Hace falta un caso que pida los plugins al revés y lea de vuelta `resumen.plugins` y `dep_plan.plugins`.

### F-02 · AMARILLO · `RutasPlanes.java:28` · `PlanDto` responde `Plan.PENDIENTE` en vez de `plan.estado()`
El orquestador me pidió que lo juzgara. **No esconde nada hoy**, por tres razones:
- `Plan.nuevo` siempre fija `PENDIENTE`, y esta tarea solo inserta.
- La prueba de `pe` afirma `estado` = `pendiente` en la respuesta.
- La lectura de vuelta de CA-2 afirma `pendiente` en `dep_plan`, que es lo que de verdad se guardó.

Además, la intención del grep de CA-6 (no llamar a las escrituras de U05) se respeta. Mi grep adicional sobre `despliegue` solo encuentra la inyección de `AdaptadorKubernetes`, y la única llamada a él es `leerPais`.

El costo es que la respuesta deja de derivarse de la fila guardada. Si una tarea posterior reutiliza `PlanDto.de` para otros estados, mentirá en silencio.

El defecto está en la especificación: el patrón `\.(estado)\(` choca con el accesor del record `Plan`. Al orquestador le recomiendo afinar el grep, por ejemplo con `kubernetes\.(aplicar|construir|estado|contextoFallo)\(` o con el nombre del campo, y entonces volver a `plan.estado()`. El codificador lo dejó documentado en la bitácora y en el javadoc, lo que es correcto.

### F-03 · AMARILLO · `bitacoras/U07-B-evidencia/ca5-4.txt` y `ca5-6.txt` · El `sed` pegado no se puede ejecutar tal como está escrito
`sed -i 's/A//' 's/B/…/' archivo`, sin `-e`, toma el segundo guion como nombre de archivo. El diff pegado demuestra que sí se aplicaron las dos sustituciones, así que el parche es válido como evidencia, pero el comando literal no reproduce la mutación. Conviene pegar el comando real, con `-e` o con `-f`. Mis mutaciones equivalentes (m4 y m6) muerden igual.

### F-04 · AMARILLO · `VerificacionVivoTest.nadaTocaDepPlanNiKubernetes` · `verifyNoInteractions` solo cubre las tres llamadas de esa prueba
`@MockitoBean` reinicia el mock después de cada prueba, así que «tras todas las llamadas» no queda probado al pie de la letra. El riesgo es bajo: `VerificacionVivo` y `RutasVerificacion` no reciben `AdaptadorKubernetes`, y el `(0,)` de Python cubre `dep_plan` al final de la suite.

### F-05 · AMARILLO · CA-7 · `bitacoras/U07-B-evidencia/` no está en la lista literal de rutas
CA-7 enumera solo `bitacoras/U07-B.md`. El orquestador presentó la carpeta de evidencia como parte de la bitácora, así que no lo cuento como desborde; queda anotado para que la plantilla del criterio la incluya.

## Tareas candidatas (defectos reales fuera de alcance)
- Afinar el grep «escrituras» de CA-6 (ver F-02) en las tareas siguientes de U07.

## Rutas de transcripciones largas
Todo está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-B-r1/`:
- criterios: `ca1.txt`, `ca2.txt`, `ca3.txt`, `ca4.txt`, `ca6.txt`;
- rojos: `rojo1.txt`, `rojo2.txt`, `rojo3.txt`;
- mutaciones: `m1.txt` a `m10.txt`, con sus `m*.sed`;
- script de mutación: `corre.sh`.

El worktree quedó limpio al terminar (`git status --short` vacío; solo queda `backend/.jqwik-database`, que está ignorado y es caché de jqwik). Las mutaciones se hicieron en copias de `git archive`, ya borradas.

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/despliegue/PlanServicio.java:65|El orden por nombre de resumen.plugins y dep_plan.plugins no tiene prueba (la mutación M8 sobrevive)
AMARILLO|backend/src/main/java/io/synkra/despliegue/RutasPlanes.java:28|PlanDto responde Plan.PENDIENTE para esquivar un grep demasiado amplio
AMARILLO|bitacoras/U07-B-evidencia/ca5-4.txt|El sed pegado sin -e no se puede ejecutar tal cual
AMARILLO|VerificacionVivoTest.java:nadaTocaDepPlanNiKubernetes|verifyNoInteractions solo cubre esa prueba
AMARILLO|CA-7|bitacoras/U07-B-evidencia/ fuera de la lista literal de rutas
INFORME: revisiones/U07-B/ronda-1.md
```
