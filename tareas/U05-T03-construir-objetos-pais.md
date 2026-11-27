# U05-T03: `construir`: los objetos del país

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-12, H-15
**Depende de:** U05-T02

---

## Alcance

**Dentro** (una línea, concreta):

> Un método `construir(PlanPais)` en `AdaptadorKubernetes` que, **sin hablar con la API**, arma los tres objetos del país (Deployment, ConfigMap y Service) en `synkra-<país>`, pasando primero por la guardia de namespace, y una prueba que los compara con un YAML esperado.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Aplicar los objetos al servidor (U05-T04), leer su estado (T05) o leer el país (T06).
- El script `init-descarga/descargar.sh` y su imagen (T07 y T08). Aquí solo se nombra la imagen del init y su comando.
- Calcular qué plugins van (eso lo decide U07 con el verificador). `PlanPais` llega ya resuelto.
- Cambios fuera de `kubernetes/**` y de sus recursos de prueba.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T03)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (recursos, volúmenes, cuenta de servicio, URL del catálogo)
- `aidlc-docs/inception/application-design/component-methods.md` (`AdaptadorKubernetes`)
- `backend/src/main/java/io/synkra/kubernetes/` (adaptador y guardia de T01 y T02)

---

## Decisiones del orquestador

1. **`PlanPais`** es un record público en `kubernetes.api`: `pais` (dos letras), `imagenCore` (con digest `@sha256:`), y una lista de `PluginPlan(nombre, version, sha256)`. Valida en el constructor: país de dos letras minúsculas, imagen con digest y sha256 de 64 hexadecimales en minúsculas.
2. **Nombres fijos.** Deployment `synkra-core`, ConfigMap `synkra-core-config`, Service `synkra-core`, todos en `synkra-<país>` y con la etiqueta `app.kubernetes.io/managed-by: synkra`.
3. **Deployment**, según U08:
   - una réplica; imagen del core por digest (nunca por tag);
   - init container `descarga` con la imagen `synkra-init-descarga`, `SYNKRA_URL=http://synkra-backend.synkra.svc:8080` y recursos 32-64 Mi y 50-100m;
   - el core con recursos 256-512 Mi y 250-500m, y readiness en `/actuator/health/readiness`;
   - `emptyDir` montado en `/plugins` en los dos contenedores, y el ConfigMap en `/config`;
   - `automountServiceAccountToken: false`, contenedores sin root y sin escalada de privilegios.
4. **ConfigMap.** `plugins-activos.yaml` (nombre y versión de cada plugin) y `artefactos.txt` (una línea `<sha256> <nombre>-<version>.jar` por plugin). El orden de las líneas es estable (por nombre).
5. **Comparación.** La prueba serializa los objetos a YAML con Fabric8 y los compara con `src/test/resources/kubernetes/pais-pe-esperado.yaml`, sin depender del orden de claves.
6. **Literales.** Los recursos, rutas y nombres van en constantes con nombre, no repetidos en el código.

---

## Criterios de aceptación

- [ ] **CA-1**: los objetos coinciden con el YAML esperado, sin peticiones a la API
  ```bash
  ./gradlew :backend:test --rerun --tests '*ConstruirObjetosPaisTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ConstruirObjetosPaisTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 6, sin saltos ni fallos. Cubren: el YAML esperado de `synkra-pe` con dos plugins; un país sin plugins (`artefactos.txt` vacío); una imagen sin digest rechazada por `PlanPais`; un país inválido rechazado por la guardia; que `construir` no hace ninguna petición al servidor simulado (lectura de vuelta del conteo); y que la imagen del core en el Deployment es exactamente la del plan.

- [ ] **CA-2**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega cada línea tal cual.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat origin/main...HEAD
  ```
  Esperado: `kubernetes/**` (main, test y `src/test/resources/kubernetes/`), la bitácora y `revisiones/U05-T03/`. Se anota el sha sobre el que se tomó.

---

## Plan de pruebas

- `ConstruirObjetosPaisTest` con el YAML esperado escrito a mano a partir de U08, no generado por el propio código.

**Rojo primero:** el YAML esperado y la prueba antes del código, en una copia o con la prueba sola, sin `git stash`.

---

## Notas

- La máquina tiene contextos `kind` reales en `~/.kube/config`. Ninguna prueba los usa: solo el servidor simulado, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos.
