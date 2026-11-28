# Plan de tareas: U08 Instalación y límite

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian. Diseño en `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/`.

Raíz del proyecto. Crea `deploy/` (Kustomize `base/` y `overlays/kind/`, `kind/cluster.yaml`), el
`Makefile` y `scripts/e2e-api.sh`. Resumen en `aidlc-docs/construction/U08-instalacion-limite/code/`.

## Contexto

- **Historias dueñas:** H-24, H-26, H-27, H-28. **Contribuye a:** H-13 (primera capa del límite).
- **Depende de:** U02 (imagen del core de ejemplo y plugins de prueba) y U07 (backend que despliega).
- **Dónde corre:** solo en un `kind` local creado por la misma tarea. **Ninguna tarea aplica nada
  a un clúster compartido** (AUTONOMIA-01); `make install` se niega a correr si el contexto actual
  de `kubectl` no es `kind-synkra`.

## Tareas

- [x] **U08-T01 · Clúster `kind` y registro local**
  `deploy/kind/cluster.yaml` (control plane + worker, `extraPortMappings` 80/443) y el contenedor `kind-registry` en `localhost:5001` conectado a la red `kind`.
  **Criterio:** `make cluster` termina con 0 y `kubectl get nodes --context kind-synkra` lista 2 nodos `Ready`. · H-26

- [x] **U08-T02 · Guardia de contexto**
  Todo objetivo del `Makefile` que toque el clúster comprueba `kubectl config current-context` = `kind-synkra`.
  **Criterio:** `kubectl config use-context otro && make install` sale con código distinto de 0 y el mensaje `contexto no permitido`, sin aplicar nada. · H-26

- [x] **U08-T03 · Namespaces (supuesto S1)**
  `deploy/base/namespaces.yaml`: `synkra`, `synkra-co`, `synkra-pe`, con la etiqueta `synkra.io/pais`.
  **Criterio:** `kubectl kustomize deploy/base | grep -c 'kind: Namespace'` = 3. · H-26

- [x] **U08-T04 · RBAC de `synkra:deployer`**
  ServiceAccount en `synkra`; un Role y un RoleBinding por namespace de país (Deployments, ConfigMaps, Services, Pods y eventos en lectura); **ningún** ClusterRole ni ClusterRoleBinding.
  **Criterio:** `kubectl kustomize deploy/base | grep -cE 'kind: Cluster(Role|RoleBinding)'` = 0. · H-27

- [x] **U08-T05 · PVC y Deployment del backend**
  PVC `synkra-datos` 1 Gi `ReadWriteOnce`, Deployment con `strategy: Recreate`, `/data` montado, `runAsNonRoot`, recursos y sondas de salud; Service `synkra-backend:8080`.
  **Criterio:** `kubectl kustomize deploy/base | yq 'select(.kind=="Deployment" and .metadata.name=="synkra-backend") | .spec.strategy.type'` = `Recreate`. · H-26

- [x] **U08-T06 · Overlay `kind`: imágenes e Ingress**
  `deploy/overlays/kind/` fija las imágenes de `localhost:5001` y añade el Ingress `synkra.local` (`/api` → backend, `/` → front) con el secreto TLS autofirmado.
  **Criterio:** `kubectl kustomize deploy/overlays/kind | kubeconform -strict -summary` termina sin errores. · H-26

- [x] **U08-T07 · Imágenes multi-stage sin root**
  Dockerfiles del backend, del core de ejemplo y del init container con etapa de build y etapa final mínima, `USER` numérico distinto de 0.
  **Criterio:** `make imagenes && for i in synkra-backend synkra-core-ejemplo synkra-init-descarga; do docker inspect -f '{{.Config.User}}' localhost:5001/$i; done` no imprime vacío, `root` ni `0`. · H-28

- [x] **U08-T08 · `make install`**
  Orden del diseño: clúster y registro, Ingress NGINX y certificado, imágenes, `kubectl apply -k deploy/overlays/kind`, espera de `rollout status`, `make verificar-limite`.
  **Criterio:** sobre un `kind` recién creado, `make install` termina con 0; `kubectl get pods -n synkra` muestra el backend `Running` y `kubectl get ns` lista `synkra-co` y `synkra-pe`. · H-26

- [x] **U08-T09 · `make verificar-limite`**
  Solo pregunta con `kubectl auth can-i --as=system:serviceaccount:synkra:deployer`: `yes` en `synkra-co` y `synkra-pe`; `no` en `default`, `kube-system`, `synkra` (Deployments) y `create namespaces`.
  **Criterio:** `make verificar-limite` termina con 0 e imprime las seis respuestas esperadas; si se añade a mano un ClusterRoleBinding de prueba, termina con 1. · H-27, H-13

- [x] **U08-T10 · Script de punta a punta por la API**
  `scripts/e2e-api.sh`: CU1 subir core y plugins, CU2 planear y confirmar en `synkra-co`, CU3 cambiar plugins, CU4 combinación incompatible rechazada (nada llega al clúster), CU5 fallo y vuelta atrás. Usa `curl` y `jq`, `set -euo pipefail`.
  **Criterio:** `./scripts/e2e-api.sh` termina con 0 después de `make install`; tras CU4, `kubectl get deploy -n synkra-co -o jsonpath='{..image}'` no cambió. · H-24

- [x] **U08-T11 · Formato único de error**
  **Criterio:** `./scripts/e2e-api.sh` comprueba que cada respuesta 4xx que provoca tiene `codigo`, `mensaje` y `detalles` (`jq -e 'has("codigo") and has("mensaje") and has("detalles")'`). · H-24

- [x] **U08-T12 · Resumen**
  **Criterio:** `aidlc-docs/construction/U08-instalacion-limite/code/summary.md` contiene la salida real de `make install`, `make verificar-limite` y `./scripts/e2e-api.sh`. · H-26

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-24 | T10, T11 |
| H-26 | T01, T02, T03, T05, T06, T08, T12 |
| H-27 | T04, T09 |
| H-28 | T07 |
| H-13 (contribuye) | T09 |

**Terminada cuando:** en un `kind` nuevo, `make install && make verificar-limite && ./scripts/e2e-api.sh` termina con 0.
