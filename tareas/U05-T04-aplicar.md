# U05-T04: `aplicar` con server-side apply

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-12
**Depende de:** U05-T03

---

## Alcance

**Dentro** (una línea, concreta):

> Un método `aplicar(ObjetosPais)` en `AdaptadorKubernetes` que manda los tres objetos de `construir` al namespace del país con **server-side apply** y el *field manager* `synkra`, pasando antes por la guardia.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Esperar a que el pod quede listo, leer su estado o sus logs (U05-T05).
- Borrar objetos, crear namespaces o tocar otros namespaces: los namespaces los crea la instalación (U08).
- Reintentos, colas o cualquier orquestación del despliegue (U07).
- Cambios fuera de `kubernetes/**`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T04)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (permisos del `Role` `synkra-deployer`)
- `backend/src/main/java/io/synkra/kubernetes/` (adaptador, guardia y constructor de T01 a T03)
- `CANDIDATAS.md` (C-70 y C-73)

---

## Decisiones del orquestador

1. **Server-side apply.** Con `serverSideApply()` de Fabric8 y `fieldManager("synkra")`, con `force` activado para que SynKra sea el dueño de sus campos. El orden es ConfigMap, Service y Deployment.
2. **Solo los verbos del `Role`.** Aplicar usa `patch` (y `get` si Fabric8 lo necesita). No usa `create` aparte, ni `delete`, ni nada a nivel de clúster.
3. **Guardia.** El namespace se toma de los objetos y pasa por la guardia antes de la primera petición. Si los tres objetos no tienen el mismo namespace, se rechaza sin ninguna petición.
4. **Errores.** Un error de la API se envuelve en una excepción propia del módulo, que nombra el objeto que falló. Si falla el segundo, el primero no se deshace; lo dice la bitácora.

---

## Criterios de aceptación

- [ ] **CA-1**: los tres objetos quedan en el servidor simulado, con su *field manager* (lectura de vuelta)
  ```bash
  ./gradlew :backend:test --rerun --tests '*AplicarTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*AplicarTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 5, sin saltos ni fallos. Las pruebas, contra el servidor simulado en modo CRUD:
  - después de `aplicar`, leen de vuelta del servidor el Deployment, el ConfigMap y el Service en `synkra-pe`, y comprueban que `metadata.managedFields` tiene `manager: synkra` con `operation: Apply`;
  - aplicar dos veces el mismo plan deja los mismos objetos (idempotente);
  - aplicar un plan con otro plugin cambia el ConfigMap y la imagen del Deployment como se espera;
  - objetos con namespaces distintos se rechazan sin ninguna petición;
  - las peticiones registradas son solo `PATCH` (y `GET`), nunca `DELETE` ni `POST` a otro namespace.

- [ ] **CA-2**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat origin/main...HEAD
  ```
  Esperado: `kubernetes/**`, la bitácora y `revisiones/U05-T04/`. Se anota el sha.

---

## Plan de pruebas

- `AplicarTest` con `KubernetesMockServer` en modo CRUD. Si el modo CRUD no registra `managedFields`, la prueba lee el `PATCH` registrado (tipo `application/apply-patch+yaml` y parámetro `fieldManager=synkra`) y lo dice la bitácora.

**Rojo primero:** la prueba antes que `aplicar`, en una copia o con la prueba sola, sin `git stash`.

---

## Notas

- La máquina tiene contextos `kind` reales en `~/.kube/config`. Ninguna prueba los usa: solo el servidor simulado, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos.
