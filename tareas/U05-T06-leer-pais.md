# U05-T06: `leerPais`

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-19 (contribuye)
**Depende de:** U05-T05

---

## Alcance

**Dentro** (una línea, concreta):

> Un método `leerPais(String pais)` en `AdaptadorKubernetes` que lee del clúster el Deployment `synkra-core` y el ConfigMap `synkra-core-config` de `synkra-<país>` y devuelve lo que corre de verdad: la imagen del core con su digest y la lista de plugins con nombre, versión y sha256; si el país no tiene nada desplegado, devuelve vacío.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Comparar con el registro de despliegues y calcular `concuerda` (U06 y U07). Aquí solo se lee el clúster.
- El endpoint `GET /api/paises` (U07).
- Escribir, borrar o reparar objetos, aunque estén inconsistentes.
- Cambios fuera de `kubernetes/**` (salvo el YAML esperado de `ConstruirObjetosPaisTest` en `src/test/resources/kubernetes/`, por la decisión 6).

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T06)
- `aidlc-docs/inception/user-stories/stories.md` (H-19: el ConfigMap cambiado a mano)
- `aidlc-docs/inception/application-design/component-methods.md` (C09)
- `backend/src/main/java/io/synkra/kubernetes/` (`ConstructorObjetosPais`: formato de `plugins-activos.yaml` y `artefactos.txt`; `PlanPais`; `estado` y `contextoFallo` de U05-T05)
- `CANDIDATAS.md` (C-73)

---

## Decisiones del orquestador

1. **Tipo propio.** `LecturaPais` es un record en `kubernetes.api`, sin tipos de Fabric8: `pais`, `imagenCore` y `List<PluginLeido(nombre, version, sha256)>`. `leerPais` devuelve `Optional<LecturaPais>`: vacío si no existen ni el Deployment ni el ConfigMap.
2. **Fuente de cada dato.** La imagen del core sale del contenedor `core` del Deployment. Nombre y versión salen de `plugins-activos.yaml`; el sha256, de `artefactos.txt`. Los dos se unen por nombre. Las constantes de nombres y claves se reutilizan de `ConstructorObjetosPais`, no se repiten como literales.
3. **Contenido hostil o inconsistente.** El ConfigMap lo puede cambiar alguien a mano (H-19), así que no se confía en él: el YAML se lee con un lector seguro (sin etiquetas de tipo, con tope de tamaño), y si falta uno de los dos objetos, si un plugin aparece en un archivo y no en el otro, o si un valor no cumple el formato de `PlanPais`, `leerPais` lanza una excepción propia del módulo que nombra el país y lo que no cuadra. No se inventa una lectura parcial.
4. **País.** Se valida con la misma regla de `PlanPais` (dos letras minúsculas), se arma `synkra-<país>` y pasa por la guardia antes de la primera petición. Sin cliente, error propio y sin peticiones.
5. **Verbos.** Solo `GET`.
6. **Contrato de `plugins-activos.yaml` (C-81).** Hoy `construir` escribe un mapa con la clave `plugins:` y el core (U02-T04, decisión 2) exige una **lista en la raíz** de `{nombre, version}`; un pod real no arrancaría. El formato canónico es el del core: `construir` pasa a escribir la lista en la raíz (`[]` si no hay plugins), se actualiza el YAML esperado de `ConstruirObjetosPaisTest`, y `leerPais` lee ese mismo formato. El formato queda escrito en el Javadoc de `ConstructorObjetosPais.CLAVE_PLUGINS`.

---

## Criterios de aceptación

- [ ] **CA-1**: lectura de vuelta del clúster simulado
  ```bash
  ./gradlew :backend:test --rerun --tests '*LeerPaisTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*LeerPaisTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 7, sin saltos ni fallos. Casos, con objetos sembrados en el servidor simulado:
  - país desplegado: los objetos se siembran con lo que devuelve `construir` para un `PlanPais` conocido, y `leerPais` devuelve el mismo core y los mismos plugins (ida y vuelta);
  - país vacío: `Optional.empty()`;
  - ConfigMap cambiado a mano (otra versión de un plugin): `leerPais` devuelve la versión del ConfigMap, no la del plan;
  - un plugin en `plugins-activos.yaml` sin línea en `artefactos.txt`: excepción propia que lo nombra;
  - YAML con etiqueta `!!java...`: excepción propia, sin construir objetos Java;
  - Deployment sin ConfigMap: excepción propia;
  - país inválido (`PE`, `p`, `../x`) y sin cliente: rechazo con cero peticiones;
  - todas las peticiones registradas son `GET` a `/namespaces/synkra-<país>/`;
  - el `plugins-activos.yaml` que genera `construir` es una lista en la raíz con un mapa `{nombre, version}` por plugin (se lee con SnakeYAML Engine y se comprueba el tipo), y `[]` sin plugins.

- [ ] **CA-2**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `kubernetes/**` (main, test y `src/test/resources/kubernetes/`), la bitácora y `revisiones/U05-T06/`. Se anota el sha.

---

## Plan de pruebas

- `LeerPaisTest` con `KubernetesMockServer`, en modo CRUD si basta para `GET` (en este modo se pueden sembrar objetos con `create` del cliente de pruebas); si no, con expectativas, y la bitácora lo dice.

**Rojo primero:** la prueba antes que el código, en una copia o con la prueba sola, sin `git stash`.
