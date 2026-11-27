# U05-T01: Cliente Fabric8 configurado con la cuenta del pod

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-12 (base)
**Depende de:** U03 (terminada)

---

## Alcance

**Dentro** (una línea, concreta):

> El módulo `io.synkra.kubernetes` (con `kubernetes.api` como única parte pública) y un bean `KubernetesClient` de Fabric8 que se configura **solo** con la cuenta de servicio del pod (token y CA montados) y nunca con el `kubeconfig` de la máquina.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- La guardia de namespace (U05-T02), `construir` (T03), `aplicar` (T04), `estado` y `contextoFallo` (T05), `leerPais` (T06).
- `init-descarga/` (T07 y T08).
- Cualquier llamada a un clúster real, incluido el `kind` local: las pruebas usan solo el servidor simulado de Fabric8 (AUTONOMIA-01).
- Cambios en `catalogo/`, `plataforma/` (salvo lo mínimo para registrar el módulo), `verificador/` o la raíz de Gradle.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T01)
- `aidlc-docs/inception/application-design/component-methods.md` (`AdaptadorKubernetes`)
- `aidlc-docs/construction/U08-instalacion-limite/` (cuenta de servicio y RBAC que usará el pod)
- `backend/build.gradle.kts`, `backend/src/main/java/io/synkra/SynkraApp.java`
- `backend/src/test/java/io/synkra/FronterasModulosTest.java`

---

## Decisiones del orquestador

1. **Versión.** Fabric8 Kubernetes Client en su última versión estable compatible con Spring Boot 3.5, fijada en `backend/build.gradle.kts` (sin rangos). El servidor simulado (`kubernetes-server-mock` o `kube-api-test`, el que esa versión ofrezca) solo en `testImplementation`.
2. **Origen de la configuración.** El cliente se arma con `Config` explícito:
   - `autoConfigure` apagado: no lee `~/.kube/config`, ni `KUBECONFIG`, ni variables `KUBERNETES_*` salvo las del pod.
   - En producción, token y CA salen de `/var/run/secrets/kubernetes.io/serviceaccount/` (la ruta se puede cambiar con `synkra.kubernetes.cuenta` para las pruebas) y la URL de `KUBERNETES_SERVICE_HOST`/`KUBERNETES_SERVICE_PORT`.
   - Si falta el token, el bean no se crea con credenciales de otro sitio: el módulo arranca deshabilitado y `kubernetes.api` informa que no hay clúster (sin excepción al arrancar el backend).
3. **Interfaz.** `kubernetes.api` expone una interfaz `AdaptadorKubernetes` todavía sin métodos de negocio (o solo `disponible()`). Los métodos llegan en T02 a T06.
4. **Fronteras.** `io.fabric8` solo se importa dentro de `io.synkra.kubernetes`. `FronterasModulosTest` sigue pasando con el módulo nuevo.

---

## Criterios de aceptación

- [ ] **CA-1**: el cliente se configura con la cuenta del pod, contra el servidor simulado
  ```bash
  ./gradlew :backend:test --rerun --tests '*ClienteKubernetesTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ClienteKubernetesTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 4, `skipped="0" failures="0" errors="0"`. Las pruebas afirman:
  - con un token y una CA de prueba en un `@TempDir`, el cliente llega al servidor simulado y la petición registrada lleva `Authorization: Bearer <ese token>` (lectura de vuelta del servidor simulado, no del código de salida);
  - con `KUBECONFIG` apuntando a un archivo con otro servidor y otro token, el cliente **no** los usa;
  - sin token, el backend arranca y `disponible()` es falso;
  - el cliente no hace ninguna petición al arrancar.

- [ ] **CA-2**: el backend arranca sin clúster y sin tocar el `kubeconfig` de la máquina
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  KUBECONFIG=/nonexistent java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  curl -s localhost:18080/actuator/health | python3 -I -c "import json,sys; print(json.load(sys.stdin)['status'])"
  kill $PID; wait $PID 2>/dev/null; rm -rf "$SYNKRA_DATA"
  ```
  Esperado: `UP`.

- [ ] **CA-3**: todo el backend sigue pasando, incluidas las fronteras
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rn "io.fabric8" backend/src/main/java --include=*.java | grep -v "/kubernetes/" || echo ninguno
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos en ningún XML, y `ninguno`.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `backend/build.gradle.kts`, `kubernetes/**` (main y test), lo mínimo de `application.properties`, la bitácora y `revisiones/U05-T01/`.

---

## Plan de pruebas

- `ClienteKubernetesTest` con el servidor simulado de Fabric8 y un `@TempDir` como cuenta de servicio.

**Rojo primero:** se escribe la prueba antes del bean y se registra el fallo de CA-1, en una copia o con la prueba sola, sin `git stash` en el worktree.

---

## Notas

- La máquina tiene contextos `kind` en `~/.kube/config`. Ninguna prueba ni criterio puede usarlos; si una prueba los alcanza, es un ROJO.
