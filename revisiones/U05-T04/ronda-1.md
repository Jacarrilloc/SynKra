# Ronda 1 — U05-T04

VEREDICTO: NO-VERDE

Hash de la tarea: `git hash-object tareas/U05-T04-aplicar.md` = 33e2a2cb488a1a08c7e71a5c1b0d7cd7f113c08f, coincide. HEAD revisado: `a280041`. Diff: `git diff ceb914c...HEAD`, 5 archivos, +392. Gradle corrido con `HOME`, `KUBECONFIG` y `-Duser.home` apuntando a un directorio temporal del scratchpad y `GRADLE_USER_HOME=/root/.gradle`. No usé ningún clúster.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | `AplicarTest` ≥ 5 pruebas, sin saltos ni fallos, lectura de vuelta | `./gradlew :backend:test --rerun --tests '*AplicarTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 39s`, `tests="7" skipped="0" failures="0" errors="0"`. El XML es de mi corrida (timestamp 2026-11-27T20:31:54Z). Los tiempos por prueba van de 0,07 a 1,6 s, así que hay servidor real detrás. La lectura de vuelta es del PATCH registrado, por la vía alterna (ver más abajo). |
| 2 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 50s`, 27 XML, 379 pruebas, ninguna con skipped, failures ni errors distinto de 0 |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat ceb914c...HEAD` | pasa: `a280041`. Solo cambian `backend/src/{main,test}/java/io/synkra/kubernetes/**` y `bitacoras/U05-T04.md`. Contra `origin/main...HEAD` aparecen además `aidlc-docs/audit.md`, `loop/README.md` y `tareas/*`, que vienen de la rama de tareas y no de los commits del codificador. La base `ceb914c` la fijó el orquestador. |

**Sobre la vía alterna (lo que pidió el orquestador).** Comprobé la afirmación del codificador en el bytecode de `kubernetes-server-mock-7.9.0`. `crud/PatchHandler` devuelve `404` cuando el objeto no existe y solo sabe aplicar JSON patch (`zjsonpatch`). No escribe `managedFields` en ninguna parte. La letra de CA-1 (leer `managedFields` con `manager: synkra, operation: Apply`) no se puede cumplir con este simulado, y el plan de pruebas de la tarea autoriza justo esta alternativa: leer el PATCH registrado con `application/apply-patch+yaml` y `fieldManager=synkra`.

Las pruebas leen de vuelta, en cada petición:
- el tipo de contenido;
- `fieldManager=synkra` y `force=true`;
- el cuerpo, comparado como árbol YAML contra el objeto construido;
- el orden ConfigMap, Service, Deployment;
- que todo va a `/namespaces/synkra-pe/`;
- que no hay ni `DELETE` ni `POST`.

**Juicio:** es evidencia suficiente para CA-1 dentro de lo que permite la tarea, y la bitácora declara el desvío. Lo que queda sin probar es el comportamiento real del servidor: crear con apply y rellenar `managedFields`. Va como tarea candidata.

## Hallazgos

### F-01 · NARANJA · `AdaptadorFabric8.java:52` y `AplicarTest.java:112,186,221` · El *field manager* `"synkra"` es un literal suelto en el punto de decisión
`fieldManager("synkra")` se escribe inline. Ese valor lo ve el operador en `metadata.managedFields` y define a quién pertenecen los campos en el clúster. Si cambia, SynKra entra en conflicto consigo mismo. El módulo ya trabaja con constantes con nombre (`ConstructorObjetosPais.GESTOR = "synkra"`, `NOMBRE_CORE`, `NOMBRE_CONFIG`), pero el diff mete el valor nuevo sin nombre.

Barrido de la clase: la prueba repite el mismo literal en tres sitios (`fieldManager=synkra` en las líneas 112, 186 y 221) y los nombres `"synkra-core"` y `"synkra-core-config"` en `arranca()` (línea 58 y siguientes), en vez de usar las constantes del constructor. Se arregla con una constante con nombre para el *field manager*, usada en `aplicar` y en las aserciones.
```
$ grep -n '"synkra"\|fieldManager=synkra' backend/src/main/java/io/synkra/kubernetes/*.java backend/src/test/java/io/synkra/kubernetes/AplicarTest.java
AdaptadorFabric8.java:52: ...fieldManager("synkra").forceConflicts().serverSideApply();
ConstructorObjetosPais.java:30:    static final String GESTOR = "synkra";
AplicarTest.java:112 / 186 / 221: ...fieldManager=synkra...
```

### F-02 · NARANJA · `AdaptadorFabric8.java:49-55` · Sin cliente, `aplicar` acusa a la API de un error que no ocurrió
El Javadoc de la clase dice que `cliente` es nulo si el pod no tiene cuenta de servicio, y `existeNamespace` sí trata ese caso. En `aplicar`, en cambio, `cliente.resource(...)` lanza `NullPointerException` dentro del `try`. El `catch (RuntimeException e)` lo convierte en `AplicacionFallidaException("ConfigMap/synkra-core-config")`, cuyo mensaje dice que «La API de Kubernetes rechazó o no pudo aplicar». Eso es falso: no hubo ninguna petición.

El mismo `catch` amplio también disfraza de fallo de la API cualquier error de programación, por ejemplo un `getMetadata()` nulo. Lo esperado: comprobar `disponible()` antes de la guardia o junto a ella, con un error propio y sin peticiones, y envolver solo `KubernetesClientException`. Hoy ninguna prueba cubre este caso.

### F-03 · NARANJA · `AplicarTest.java:194-214` (`siFallaElSegundoNombraElObjetoYElPrimeroSeQueda`) · La prueba no demuestra que «el primero se queda»
La prueba filtra solo los `PATCH` y comprueba que son 2 y que ninguno va a `/deployments/`. Si `aplicar` deshiciera el ConfigMap con un `DELETE` después del fallo, la prueba seguiría pasando, porque el `DELETE` queda fuera del filtro y el simulado le responde 404 sin que Fabric8 lance excepción. Aun así, la bitácora afirma «la prueba lo comprueba: 2 PATCH, ninguno al Deployment». La decisión 4 queda como afirmación sin evidencia. Lo esperado: comprobar que las peticiones registradas son exactamente dos `PATCH` y ningún otro método.

### F-04 · AMARILLO · `AplicarTest.java:27-30` · El Javadoc de la clase dice «modo CRUD» y no es así
La clase dice «contra el servidor simulado en modo CRUD», pero `new KubernetesMockServer(false)` sin `KubernetesCrudDispatcher` es el modo de expectativas. El comentario de `arranca()` sí lo explica; el de la clase lo contradice.

### F-05 · AMARILLO · `AplicarTest.java:87-93` · La lectura de peticiones depende de un tiempo fijo
`takeRequest(10, TimeUnit.MILLISECONDS)` en bucle sirve porque las peticiones son síncronas y ya están registradas. Sería más robusto leer exactamente `getRequestCount()` peticiones, en lugar de cortar por una espera de 10 ms.

**Estado del worktree al terminar:** limpio. `git status --short` sale vacío; solo aparecen ignorados `build/`, `.gradle/` y `backend/.jqwik-database`.

## Tareas candidatas (defectos reales fuera de alcance)
- **Prueba de apply contra un servidor real (`kind-synkra`, en U08 o en una prueba de integración).** Comprobaría que server-side apply crea los objetos que no existen y que `managedFields` muestra `manager: synkra, operation: Apply`. Con el simulado de Fabric8 7.9.0 eso no se puede verificar.
- **Reintentos de Fabric8 frente a U07.** El cliente de Fabric8 que configuró U05-T02 reintenta solo ante respuestas 5xx; la bitácora lo observó con un `500` que produjo 3 PATCH en vez de 2. Esos reintentos implícitos se cruzan con la orquestación de reintentos que le toca a U07, y conviene decidir allí si se desactivan o se acotan.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2.log
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2-xml.txt

```
VEREDICTO: NO-VERDE
NARANJA|AdaptadorFabric8.java:52|El field manager "synkra" es un literal suelto, repetido en tres sitios de la prueba
NARANJA|AdaptadorFabric8.java:49-55|Sin cliente, aplicar acusa a la API de un error que no ocurrió
NARANJA|AplicarTest.java:194-214|La prueba no demuestra que el primer objeto se queda
INFORME: revisiones/U05-T04/ronda-1.md
```
