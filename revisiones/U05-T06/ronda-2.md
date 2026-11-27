# Ronda 2 — U05-T06

VEREDICTO: NO-VERDE

Los tres criterios pasan con mi propia ejecución, y F-01 a F-05 quedan cerrados: cada mutante que corrí lo detecta su prueba. Queda un NARANJA nuevo, y lo introdujo justamente el arreglo de F-03. El lector del backend ya no rechaza una etiqueta explícita `!!null` (ni `!!int`, `!!float` o `!!bool`) sobre un escalar sin comillas. Con un ConfigMap editado a mano con `!!null facturacion` en la raíz, `leerPais` devuelve una lectura válida con cero plugins. Para ese mismo archivo, el core se niega a arrancar.

Comprobaciones previas:
- **Hash de la tarea:** `git hash-object` da `e94a10dc07aa29b842e2f2601efe3b0cfde155be`. Coincide.
- **HEAD y base:** HEAD `59d03fd`, base `547f8b8`.
- **Gradle:** HOME, KUBECONFIG y `-Duser.home` apuntaban a mi scratchpad, con GRADLE_USER_HOME=/root/.gradle. No usé kubectl ni ningún clúster.
- **Copia:** los mutantes y las sondas los corrí solo en una copia hecha con `git archive HEAD` en mi scratchpad.
- **Worktree:** `git status --short` vacío al terminar.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | LeerPaisTest ≥ 7, sin saltos | `./gradlew :backend:test --rerun --tests '*LeerPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 18s`, `tests="18" skipped="0" failures="0" errors="0"`. Cada caso tarda entre 0,019 y 1,977 s: las pruebas corrieron de verdad contra el servidor simulado. |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 1m`, EXIT=0. Hay 29 XML, todos con `skipped="0" failures="0" errors="0"`, 418 tests en total. Los conteos por clase son idénticos a los de `verde-ca2-r2.txt`. |
| 3 | Diff acotado | `git rev-parse --short HEAD; git diff --stat 547f8b8...HEAD` | pasa: `59d03fd`, 22 archivos. Lo único fuera de `kubernetes/**`, `bitacoras/U05-T06*` y `revisiones/U05-T06/` es `backend/build.gradle.kts`, aceptado por arbitraje (C-78). No hay desborde. |

La evidencia de la ronda 2 cuadra con el diff actual: `verde-ca1-r2.txt` da 18/18, `mutante-2.txt` da 4 fallos y `rojo-2.txt` da 1 fallo, y yo obtuve exactamente esos números.

## Formato que escribe `construir`, frente al lector del core
Se sigue aceptando. Hice una sonda (`SondaR2Test`, solo en la copia) que replica literalmente la lógica de `ArranquePlugins.leerActivos` y su `etiquetaNoPermitida` de `/home/user/wt-m6r-U02-T05/core/src/main/java/io/synkra/core/ArranquePlugins.java`.
- **Con plugins:** le pasé la salida de `construir` con valores difíciles: `pago-movil|1.0.0-rc.1`, `x2|10.0.0+b.5`, `true|1.0`, `null|0.0.1` y `a.b|007`. Resultado: `CORE ok 5`, con todos los valores como texto. El backend devuelve los mismos cinco.
- **Sin plugins:** `[]` da `CORE ok 0` y una lectura vacía.

El cambio a constantes no altera ni un byte del formato: las constantes tienen los mismos valores que los literales de la ronda 1.

## Estado de los hallazgos de la ronda 1
| Id | Estado | Cómo lo comprobé |
|---|---|---|
| F-01 NARANJA | **cerrado** | Mutante `Tag etiqueta = null;` en la copia: `18 tests completed, 4 failed`. Lo detectan `!!java` y los tres casos nuevos con `!!set` y `!!binary`. |
| F-02 NARANJA | **cerrado** | No quedan literales `"nombre"`, `"version"` ni `.jar` en `kubernetes/` fuera de las constantes. El escritor, el lector y `LINEA_ARTEFACTO` usan `CLAVE_NOMBRE`, `CLAVE_VERSION`, `SUFIJO_JAR` y `nombreJar`. |
| F-03 AMARILLO | **cerrado, pero con regresión** (ver F-06) | Mutante que vuelve a `t.equals(Tag.STR) ? null : t`: `versionSinComillas...` falla (1 de 18). El mensaje ahora dice «ambos como texto». |
| F-04 AMARILLO | **cerrado** | La prueba del formato lee con `CoreSchema`. |
| F-05 AMARILLO | **cerrado** | Mutante en la copia: `envuelve` deja de convertir la `KubernetesClientException`. Fallan `[1] codigo=403` y `[2] codigo=500` (2 de 18). |

## Hallazgos nuevos

### F-06 · NARANJA · LectorConfigMapPais.java:212 (arreglo de F-03) · Se aceptan etiquetas explícitas de escalar, y la raíz `!!null` se lee como «sin plugins»
La línea es `return t.equals(Tag.STR) || (e.isPlain() && ESCALARES_IMPLICITOS.contains(t)) ? null : t;`.

El fallo es que deja pasar cualquier escalar sin comillas con etiqueta INT, FLOAT, BOOL o NULL, incluida una etiqueta **explícita** que no coincide con la que el esquema le daría al valor. El core hace la comparación correcta: la etiqueta debe ser igual a `getScalarResolver().resolve(valor, true)`.

Lo que dio mi sonda con `LectorConfigMapPais.leer`, frente a la réplica del core:
- `!!null facturacion\n` (con `artefactos.txt` vacío):
  - CORE: `rechaza: etiqueta tag:yaml.org,2002:null`.
  - BACK: `ok LecturaPais[... plugins=[]]`.
- `!!null x\n`: mismo resultado.
- `version: !!null 1.0.0`:
  - CORE: «etiqueta».
  - BACK: «ambos como texto». La etiqueta ya no se nombra.
- `version: !!float x`:
  - CORE: «etiqueta».
  - BACK: «no es un YAML válido».

En la ronda 1 los cuatro casos se rechazaban por etiqueta. El primero rompe la decisión 3 («sin etiquetas de tipo… no se inventa una lectura parcial») y el escenario de H-19. Alguien edita el ConfigMap a mano, el pod no arranca, y `leerPais` informa a U06 y U07 que el país corre sin plugins.

Arreglo esperado: la misma regla que el core, es decir, aceptar un escalar PLAIN solo si su etiqueta es la que el esquema resuelve para ese valor, más una prueba con `!!null x` en la raíz que exija la excepción.

### F-07 · AMARILLO · ConstructorObjetosPais.java (escritor de `artefactos.txt`) / LectorConfigMapPais.java:44 · El separador de `artefactos.txt` sigue duplicado
El espacio entre el sha256 y el jar se escribe como `" "` en el escritor y se lee como `"([^ ]+) ("` en el lector. Es de la misma clase que F-02, aunque no es una clave ni un nombre, así que no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- C-87 (colisión de nombres de jar) y C-88 (topes repetidos) ya las registró el orquestador; no las repito.
- **Ampliación de C-88:** la lógica de `etiquetaNoPermitida` ya se ha separado de la del core. F-06 es justamente esa divergencia, lo que refuerza la idea de un lector seguro compartido.

## Rutas de transcripciones largas
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca1.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca2.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/ca2-xml.txt
- /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/sonda.txt
- Mutantes: /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/m1.txt (F-01), /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/m2.txt (F-03), /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/m3.txt (F-05)
- Sonda (solo en la copia): /tmp/claude-0/-home-user/311bec3f-8f32-5470-935c-db8e1e6d6a2d/scratchpad/r2/copia/backend/src/test/java/io/synkra/kubernetes/SondaR2Test.java

```
VEREDICTO: NO-VERDE
NARANJA|LectorConfigMapPais.java:212|El arreglo de F-03 acepta etiquetas explícitas !!null/!!int/!!float/!!bool en escalares sin comillas: «!!null x» en la raíz se lee como cero plugins, y el core lo rechaza
INFORME: revisiones/U05-T06/ronda-2.md
```
