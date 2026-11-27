# Ronda 1 — U05-T02

VEREDICTO: NO-VERDE

El código cumple los tres criterios cuando los corro yo, y las cuatro mutaciones pedidas o propuestas se comportaron como debían salvo una (AMARILLO). El veredicto es NO-VERDE por un solo motivo: la evidencia de CA-2 en la bitácora no pudo haber salido de este diff, y en la bitácora no hay ninguna evidencia de CA-3.

Comprobaciones previas: hash de la tarea `617c30473945d41e87c6ab35e674d367a765acef`, coincide. HEAD `f890728` en `m6r/U05-T02`, un commit sobre `origin/main`. El worktree estaba limpio antes de la revisión y sigue limpio después (`git status --short` vacío). Todas las ejecuciones usaron `HOME`, `user.home` (vía `JAVA_TOOL_OPTIONS`) y `KUBECONFIG` apuntando a archivos falsos del scratchpad. No usé `kubectl` ni ningún clúster.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La guardia rechaza sin hacer peticiones; tests ≥ 10, sin saltos | `./gradlew :backend:test --rerun --tests '*GuardiaNamespaceTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="19" skipped="0" failures="0" errors="0"`, suite de 3,6 s (servidor simulado real). Aparecen los 12 casos de rechazo que pide la tarea más 4 extra (`synkra-pe\n`, `Synkra-pe`, ` synkra-pe`, `synkra-p1`), y los aceptados `synkra-pe` y `synkra-co` |
| 2 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + bucle de grep sobre los XML | pasa: `BUILD SUCCESSFUL in 21s`, **24** XML, todos con `skipped="0" failures="0" errors="0"` |
| 3 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 5 archivos bajo `kubernetes/**` (main y test) más `bitacoras/U05-T02.md`, 169 inserciones |

## Mutaciones (todas en copias del scratchpad, nunca en el worktree)
| Mutación | Resultado |
|---|---|
| M1: aflojar la regex a `synkra-[a-zA-Z]{2,3}` y usar `find()` en lugar de `matches()` | **muerta**: 7 de 19 fallan (`synkra-PE`, `synkra-per`, `synkra-pe `, `synkra-pe/x`, `../synkra-pe`, `synkra-pe\n`, ` synkra-pe`) |
| M2: mover la guardia después de la llamada a Fabric8 (tragando la excepción del cliente) | **muerta**: 13 de 19 fallan porque el contador de peticiones sube. Sobreviven `null`, `""` y ` synkra-pe`, porque Fabric8 ya valida esos nombres en el cliente; los demás casos sí detectan la mutación |
| M3: recortar a 64 en lugar de 63 | **muerta**: falla `recortaElNamespaceEnElMensaje()` |
| M4: `[a-z]{2}` cambiado a `\p{Ll}{2}` (cualquier minúscula Unicode) | **sobrevive**: 19 de 19 pasan (ver F-02) |

## Juicio sobre lo pedido
- **`existeNamespace(String)` como método mínimo**: es adecuado. Una línea de guardia y una lectura `namespaces().withName(ns).get()`. No adelanta nada de T03 a T06.
- **`NamespaceNoPermitidoException` en `api`**: está bien ubicada. Es una excepción propia, no una `IllegalArgumentException` genérica, es pública en el paquete `@NamedInterface("api")` y la documenta el `@throws` de la interfaz.
- **Decisión 3 (sin atajos)**: se cumple en el adaptador. `AdaptadorFabric8` y su único constructor son de paquete. Los únicos métodos públicos son `disponible()`, que no recibe namespace, y `existeNamespace(String)`, cuya primera instrucción es `GuardiaNamespace.exigir`. `GuardiaNamespace` es `final`, de paquete y con constructor privado. Queda una vía externa al adaptador, anterior a este diff (ver tareas candidatas).
- **Guardia antes de la red, leída de vuelta**: la prueba compara `servidor.getRequestCount()` antes y después, tanto al rechazar como al aceptar. Es una lectura de vuelta real, no un código de salida, y M2 lo confirma.
- **Recorte a 63**: está bien hecho (`hasMessageContaining("x"*63)` y `hasMessageNotContaining("x"*64)`) y M3 lo confirma. El 63 es una constante con nombre (`MAXIMO`, el límite de etiqueta DNS), no un literal suelto.

## Hallazgos
### F-01 · ROJO · bitacoras/U05-T02.md:33-35 · Evidencia de CA-2 imposible para este diff y CA-3 sin evidencia
- La bitácora dice: «Todos los XML de backend: skipped="0" failures="0" errors="0" (**19 archivos**; el de la guardia con 19 pruebas)».
- Con el comando de CA-2 este diff produce **24** XML. `origin/main` tiene 23 clases de prueba (`git ls-tree -r --name-only origin/main backend/src/test | grep -c 'Test.java\|Propiedades.java'` → 23), y este diff añade una.
- Con este diff no hay forma de obtener 19 archivos. Según el invariante 3, eso es ROJO por sí solo.
- Además, la evidencia de CA-2 está parafraseada y no pegada: falta la salida del bucle de grep.
- De CA-3 (`git diff --stat`) no hay ninguna salida en la bitácora. Sigue sin verificar por parte del codificador.
- Cómo se cierra: pegar en la bitácora la salida real y fresca del bucle de CA-2 y del `--stat` de CA-3. El código no necesita cambios por este hallazgo.

### F-02 · AMARILLO · backend/src/test/java/io/synkra/kubernetes/GuardiaNamespaceTest.java:51-53 · Falta un caso de letras minúsculas no ASCII
- La implementación es correcta: `[a-z]` en Java es solo ASCII, y el Javadoc de la guardia dice «minúsculas ASCII».
- Sin embargo, ninguna prueba lo fija: M4 (`\p{Ll}{2}`) sobrevive.
- Un caso como `synkra-pé` o `synkra-ñu` en el `@ValueSource` cerraría el hueco.

### F-03 · AMARILLO · backend/src/main/java/io/synkra/kubernetes/api/NamespaceNoPermitidoException.java:13-15 · Mensaje con `null` sin probar
- La rama `namespace == null → "null"` y la presencia del namespace corto en el mensaje (por ejemplo `synkra-PE`) no se comprueban.
- Solo se prueba el recorte del caso largo.

## Tareas candidatas (defectos reales fuera de alcance)
- **El bean `KubernetesClient` permite saltarse la guardia.** `ConfiguracionKubernetes` (de U05-T01) lo publica en el contexto de Spring. Cualquier módulo puede inyectarlo y llamar a la API sin pasar por la guardia. `FronterasModulosTest` no prohíbe usar `io.fabric8` fuera de `io.synkra.kubernetes`, pese a lo que dice el `package-info` («único sitio donde se importa io.fabric8»). La decisión 3 de esta tarea se limita al adaptador y se cumple, así que esto no bloquea aquí. Se puede cerrar con una regla de arquitectura (ArchUnit o Modulith) o dejando de exponer el cliente como bean público.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca1.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/ca2.log
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut1.log (M1)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut2.log (M2)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut3.log (M3)
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/mut4.log (M4)

Una anomalía del scratchpad: en la primera copia (`copia/`), después de M2 desaparecieron los tres archivos nuevos y no encontré la causa. M3 y M4 se repitieron en una copia nueva, `copia2/`. Comprobé que el worktree real no se vio afectado: HEAD `f890728`, archivos presentes, estado limpio.

```
VEREDICTO: NO-VERDE
ROJO|bitacoras/U05-T02.md:33-35|Evidencia de CA-2 imposible para este diff (19 XML frente a 24 reales) y CA-3 sin evidencia
INFORME: revisiones/U05-T02/ronda-1.md
```
