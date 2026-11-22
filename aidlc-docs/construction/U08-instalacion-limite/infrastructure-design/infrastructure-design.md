# U08 · Infrastructure Design

Respuestas 1A a 5A de `../../plans/U08-instalacion-limite-infrastructure-design-plan.md`. Todo
corre en un `kind` local; no hay nube (ejercicio académico).

| Categoría | Decisión |
|---|---|
| **Entorno** | `kind` (1 control plane + 1 worker), `extraPortMappings` 80/443; registro local `kind-registry` en `localhost:5001` conectado a la red `kind`; es el registro permitido (H-31) |
| **Cómputo** | Una réplica de cada carga. Peticiones y límites: backend 256-512 Mi y 250-500m; front 128-256 Mi y 100-250m; core de país 256-512 Mi y 250-500m; init 32-64 Mi y 50-100m |
| **Almacenamiento** | PVC `synkra-datos` de 1 Gi `ReadWriteOnce`, StorageClass `standard` (local-path de `kind`), montado en `/data` del backend (`synkra.db` y `artefactos/`). Deployment del backend con `strategy: Recreate` (RNF-07). Pods de país: `emptyDir` en `/plugins` y el ConfigMap en `/config` |
| **Mensajería** | N/A: no hay colas ni eventos; la vigilancia es una tarea interna del backend (D8) |
| **Red** | Services `synkra-backend:8080` y `synkra-front:8080` (ClusterIP); Ingress NGINX con host `synkra.local`: `/api` → backend, `/` → front, TLS con certificado autofirmado generado por `make install`. El init container usa siempre `http://synkra-backend.synkra.svc:8080` (sin Ingress). Respaldo: `kubectl port-forward` |
| **Monitoreo** | `kubectl logs` y `/actuator/health`; Prometheus es Could (fuera) |
| **Compartido / aislamiento** | Un namespace por país (`synkra-co`, `synkra-pe`), creados en la instalación (S1). Los pods de país usan la `ServiceAccount` `default` con `automountServiceAccountToken: false`: el core no habla con la API de Kubernetes |
| **Límite (P2)** | `ServiceAccount` `synkra/deployer`, usada por el backend. Un `Role` `synkra-deployer` en cada `synkra-<país>` (Deployments, ConfigMaps, Services: get, list, watch, create, update, patch; Pods, Events y `pods/log`: get, list, watch) y su `RoleBinding`. **Ningún `ClusterRole` ni `ClusterRoleBinding`** para esa cuenta |
| **Cómo se instala** | Kustomize: `deploy/base/` (namespaces, RBAC, PVC, backend, Services) y `deploy/overlays/kind/` (imágenes del registro local, Ingress); U09 añade `deploy/base/front/` y U11 `deploy/base/politicas/`. `make install` construye y sube imágenes y aplica `kubectl apply -k deploy/overlays/kind`. El módulo 8 reconcilia esa misma carpeta con GitOps (H-34) |

## Lo que la instalación crea y lo que no

| Crea | No crea |
|---|---|
| Namespaces `synkra`, `synkra-co`, `synkra-pe` | Nada en `default` ni en `kube-system` |
| `ServiceAccount`, `Role` y `RoleBinding` por país | Ningún permiso de clúster para `synkra/deployer` |
| PVC, Deployments y Services de SynKra; Ingress | Los Deployments de los países (los crea SynKra al confirmar) |

## Cumplimiento

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple: la instalación se aplica solo al `kind` local, a pedido de quien corre `make install`; `make verificar-limite` solo pregunta (`kubectl auth can-i`) |
| AUTONOMIA-02 | Cumple: cada decisión tiene su comprobación en el plan de tareas de U08 |
