# Ronda 1 — U05-T06

VEREDICTO: NO-VERDE

Los tres criterios pasan con mi propia ejecución. Siguen en pie dos NARANJA. El primero: la defensa de etiquetas no tiene ninguna prueba que la sostenga, porque el mutante sobrevive. El segundo: el formato de la unión por nombre de jar y las claves `nombre`/`version` se repiten como literales, cuando la decisión 2 pide reutilizar las constantes de `ConstructorObjetosPais`.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object` da `e94a10dc07aa29b842e2f2601efe3b0cfde155be`. Coincide.
- **HEAD:** `4b65649`.
- **Base:** `547f8b8`.
- **Worktree:** limpio al terminar (`git status --short` vacío). Las sondas y el mutante los corrí sobre una copia hecha con `git archive HEAD` en mi scratchpad, nunca en el worktree.
- **Gradle:** HOME, KUBECONFIG y `-Duser.home` apuntaban a un directorio temporal mío, con GRADLE_USER_HOME=/root/.gradle. No usé kubectl ni ningún clúster.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | LeerPaisTest ≥ 7, sin saltos | `./gradlew :backend:test --rerun --tests '*LeerPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 6s`, `tests="12" skipped="0" failures="0" errors="0"`. Duración por caso entre 0,06 y 1,77 s, así que las pruebas corrieron de verdad contra el servidor simulado. |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 1m`, EXIT=0. Hay 29 XML y los 29 tienen `skipped="0" failures="0" errors="0"`. Los conteos coinciden con `verde-ca2.txt`. |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 547f8b8...HEAD` | pasa: `4b65649`, 16 archivos. Todo cae en `kubernetes/**` (main, test y `src/test/resources/kubernetes/`) o en `bitacoras/`. La excepción es `backend/build.gradle.kts`, que el orquestador ya aceptó por arbitraje. No hay desborde. |

La evidencia del codificador cuadra con el diff actual. `rojo-1.txt` muestra que la compilación falla por los símbolos que este diff introduce, y los conteos de verde coinciden con los míos.

## Puntos a juzgar expresamente

**(1) Decisión 6 (C-81): el formato que escribe `construir` es el que acepta el core. Se cumple.** Hice una sonda (`SondaRevisorTest`, solo en la copia) que replica literalmente la lógica de `ArranquePlugins.leerActivos` de U02-T05:
- CoreSchema y el mismo `etiquetaNoPermitida`, con la etiqueta implícita para escalares sin comillas.
- Lista en la raíz, `m.size()==2` y `instanceof String`.
- `Descriptor.NOMBRE` del verificador.

Le pasé la salida de `construir` con nombres y versiones difíciles: `pago-movil|1.0.0-rc.1`, `x2|10.0.0+b.5`, `true|1.0` y `null|0.0.1`. El core los acepta todos como texto, gracias a las comillas, y `[]` da una lista vacía. `LectorConfigMapPais` devuelve exactamente lo mismo. Resultado: `tests="3" skipped="0" failures="0"`. No hay inyección por comillas, porque `PlanPais.SEGURO` excluye `"` y `\`.

**(2) Mutante de la defensa de etiquetas. Sobrevive.** Lo detallo en F-01.

**(3) Unión por `nombre-version.jar`. No hay unión silenciosa.** La clave se calcula desde cada par `(nombre, version)` del YAML, se consume con `remove` y al final se exige que no quede ninguna línea sobrante. Eso obliga a que la relación entre los dos archivos sea biyectiva. Lo comprobé con dos casos:
- Con el ConfigMap `a-1|2.0.0` + `a|1-2.0.0` y una sola línea `a-1-2.0.0.jar`, el lector lanza: `el plugin a 1-2.0.0 está en plugins-activos.yaml y no tiene línea a-1-2.0.0.jar`.
- Con un plan colisionante, `construir` escribe dos veces `a-1-2.0.0.jar`, y `leerPais` lanza `línea mal formada o repetida`.

Con las reglas del core (nombre sin puntos y versión semver) la colisión es imposible. Con la regla más laxa de `PlanPais.SEGURO` sí es posible; lo dejo como candidata.

## Hallazgos

### F-01 · NARANJA · LeerPaisTest.java `yamlConEtiquetaDeTipoJavaLanzaSinConstruirObjetos` / LectorConfigMapPais.java:126-131 · La defensa de etiquetas no tiene prueba con dientes
En la copia sustituí `Tag etiqueta = etiquetaNoPermitida(nodo, new IdentityHashMap<>());` por `Tag etiqueta = null;`. LeerPaisTest sigue en verde: `tests="12" skipped="0" failures="0" errors="0"`.

La prueba pasa igual porque CoreSchema hace fallar `Load` con una etiqueta desconocida, y ese fallo se envuelve en la misma excepción. La aserción solo mira la clase y `"pe"`.

Con el mutante, los mensajes cambian:
- `!!javax...`: de `etiqueta de tipo '...ScriptEngineManager' no permitida` a `no es un YAML válido o no es seguro`.
- `!!set`: de `etiqueta ... set` a `cada entrada debe ser {nombre, version}`.

Es decir, unas 40 líneas de defensa que pide la decisión 3 pueden desaparecer sin que nada lo detecte. Arreglo esperado: que la prueba afirme `hasMessageContaining("etiqueta")` y, mejor aún, que añada un caso con una etiqueta estándar no permitida (`!!set`, `!!binary`) que solo esa defensa nombra.

### F-02 · NARANJA · Decisión 2 · Las claves del YAML y el formato del nombre de jar se repiten como literales
Barrido de la clase completa:
- `ConstructorObjetosPais.java:111`: `"- nombre: \""` … `"\"\n  version: \""` (el escritor).
- `LectorConfigMapPais.java:149`: `m.get("nombre")` y `m.get("version")` (el lector).
- `ConstructorObjetosPais.java:114`: `p.nombre() + "-" + p.version() + ".jar\n"` (escritura de la clave de unión).
- `LectorConfigMapPais.java:56`: `a[0] + "-" + a[1] + ".jar"` (lectura de la misma clave).
- `LectorConfigMapPais.java:44`: `LINEA_ARTEFACTO` con `\\.jar`.

La decisión 2 dice: «Las constantes de nombres y claves se reutilizan de `ConstructorObjetosPais`, no se repiten como literales». El formato del jar es la clave de la unión entre los dos archivos. Si el escritor cambia y el lector no, cada plugin pasaría a lanzar «no tiene línea». Arreglo esperado: constantes para las claves `nombre`/`version` y una única función o constante para `nombre-version.jar`, todo en `ConstructorObjetosPais` y usado por los dos lados.

### F-03 · AMARILLO · LectorConfigMapPais.java:183-185 · Mensaje engañoso para un escalar sin comillas
Una edición a mano con `version: 1.0` sin comillas da `etiqueta de tipo 'tag:yaml.org,2002:float' no permitida`. El core, para el mismo caso, dice «ambos como texto». El rechazo es correcto; el mensaje confunde a quien editó a mano. Una solución es tratar como el core la etiqueta implícita de un escalar PLAIN y dejar que falle por `instanceof String`.

### F-04 · AMARILLO · LeerPaisTest.java `pluginsActivosQueEscribeConstruirEsUnaListaEnLaRaiz` · La prueba no usa el esquema del core
La prueba lee con `LoadSettings.builder().build()` (JsonSchema por defecto) y no con CoreSchema, que es lo que usa el core. Mi sonda confirma que con CoreSchema también pasa, pero la prueba no lo fija.

### F-05 · AMARILLO · LectorPais.leerPais · Falta la prueba del camino de error de la API
No hay caso que compruebe que un 403 o 500 en el GET acaba en `LecturaFallidaException`. El Javadoc lo promete.

## Tareas candidatas (defectos reales fuera de alcance)
- **Colisión de nombres de jar en `construir`:** `PlanPais.SEGURO` admite nombres con `.` y versiones no semver, así que dos plugins (`a-1|2.0.0` y `a|1-2.0.0`) producen el mismo `a-1-2.0.0.jar` y el init container pisaría un archivo con el otro. Habría que alinear `PlanPais` con `Descriptor.NOMBRE` y la regla semver del core, o rechazar la colisión en `construir`.
- **Topes repetidos entre backend y core:** `MAX_BYTES`, `MAX_ALIAS` y `MAX_PROFUNDIDAD` se repiten en los dos lados, igual que la lógica de `etiquetaNoPermitida`. Candidata a un lector compartido, por ejemplo en verificador. Está emparentada con C-78.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca1.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/ca2-xml.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/sonda.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/sonda2.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/mutante.txt
- Sonda (solo en la copia): /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/copia/backend/src/test/java/io/synkra/kubernetes/SondaRevisorTest.java

```
VEREDICTO: NO-VERDE
NARANJA|LeerPaisTest.java yamlConEtiquetaDeTipoJavaLanzaSinConstruirObjetos / LectorConfigMapPais.java:126|La defensa de etiquetas no tiene prueba: el mutante sin ella deja LeerPaisTest 12/12 en verde
NARANJA|ConstructorObjetosPais.java:111,114 y LectorConfigMapPais.java:44,56,149|Claves nombre/version y formato nombre-version.jar repetidos como literales (decisión 2)
INFORME: revisiones/U05-T06/ronda-1.md
```
