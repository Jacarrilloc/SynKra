# U05-T02: Guardia de namespace (segunda capa, D13)

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-13 (contribuye)
**Depende de:** U05-T01

---

## Alcance

**Dentro** (una línea, concreta):

> Una `GuardiaNamespace` en `io.synkra.kubernetes` que todo método del adaptador llama **antes** de hablar con la API, y que solo deja pasar namespaces `synkra-<país>` con `<país>` de dos letras minúsculas.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- La lista de países permitidos (R-D1): es de U07-T02. La guardia solo comprueba la forma.
- `construir`, `aplicar`, `estado`, `contextoFallo` y `leerPais` (T03 a T06): aquí basta con un método mínimo del adaptador que use la guardia, para probarla.
- Cambios fuera de `kubernetes/**`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T02)
- `aidlc-docs/inception/application-design/application-design.md` (D13)
- `tareas/U05-T01-cliente-kubernetes.md` y `backend/src/main/java/io/synkra/kubernetes/`

---

## Decisiones del orquestador

1. **Forma permitida.** Exactamente `^synkra-[a-z]{2}$`. Cualquier otra cosa se rechaza con una excepción propia del módulo (no `IllegalArgumentException` genérica), con el namespace en el mensaje, recortado a 63 caracteres.
2. **Antes de la red.** La guardia corre antes de construir cualquier petición de Fabric8. La prueba lo demuestra leyendo de vuelta el número de peticiones del servidor simulado.
3. **Sin atajos.** No hay forma de saltarse la guardia: ningún método público del adaptador recibe un `KubernetesClient` ni un namespace sin pasar por ella.

---

## Criterios de aceptación

- [ ] **CA-1**: la guardia rechaza sin hacer peticiones
  ```bash
  ./gradlew :backend:test --rerun --tests '*GuardiaNamespaceTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*GuardiaNamespaceTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Casos mínimos:
  - rechazados con **cero** peticiones registradas en el servidor simulado: `default`, `kube-system`, `synkra`, `synkra-`, `synkra-PE`, `synkra-per`, `synkra-p`, `synkra-pe ` (espacio), `synkra-pe/x`, `../synkra-pe`, la cadena vacía y `null`;
  - aceptados y con al menos una petición registrada: `synkra-pe` y `synkra-co`.

- [ ] **CA-2**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `kubernetes/**` (main y test), la bitácora y `revisiones/U05-T02/`.

---

## Plan de pruebas

- `GuardiaNamespaceTest` parametrizada, con el servidor simulado del T01.

**Rojo primero:** prueba antes que guardia, en una copia o con la prueba sola, sin `git stash` en el worktree.
