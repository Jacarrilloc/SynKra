# Plan de tareas: U11 Endurecimiento (Should)

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian. Unidad **Should**: cada historia se puede cortar por separado (R9). Sin Functional Design.

Raíz del proyecto. Añade `deploy/base/politicas/`, `compose.yaml`, el escaneo con Trivy y la
carpeta de GitOps del módulo 8. Resumen en `aidlc-docs/construction/U11-endurecimiento/code/`.

## Contexto

- **Historias dueñas:** H-32, H-33, H-34.
- **Depende de:** U08 (manifiestos, imágenes y `make install`).
- **Límite:** todo se prueba en el `kind` local; la reconciliación con GitOps apunta a este mismo
  repositorio y solo a `deploy/overlays/kind` (AUTONOMIA-01).

## Tareas

- [ ] **U11-T01 · Pod Security `restricted`**
  Etiquetas `pod-security.kubernetes.io/enforce=restricted` en `synkra` y `synkra-*`; los pods de SynKra y la plantilla del pod del país cumplen (sin privilegios, `seccompProfile: RuntimeDefault`, `capabilities.drop: [ALL]`).
  **Criterio:** `kubectl get ns -L pod-security.kubernetes.io/enforce` muestra `restricted` en los tres y `./scripts/e2e-api.sh` sigue terminando con 0. · H-32

- [ ] **U11-T02 · NetworkPolicy *deny-by-default***
  Política que niega todo en cada namespace y permisos explícitos: Ingress → front y backend; front → backend; init container de los países → `synkra-backend:8080`.
  **Criterio:** `kubectl get netpol -A` lista la política por defecto en los tres namespaces; `kubectl run prueba -n default --rm -i --image=curlimages/curl -- curl -m 5 http://synkra-backend.synkra.svc:8080` falla por *timeout*. · H-32

- [ ] **U11-T03 · Límites de recursos por namespace**
  LimitRange y ResourceQuota en `synkra-*`.
  **Criterio:** `kubectl get limitrange,resourcequota -n synkra-co` lista ambos objetos. · H-32

- [ ] **U11-T04 · `compose.yaml` de desarrollo**
  Backend con volumen para `/data`, las 6 prácticas del módulo 3 (versión fija de imágenes, sin root, salud, límites, red propia, variables sin secretos en claro).
  **Criterio:** `docker compose up -d && curl -s localhost:8080/actuator/health | jq -r .status` = `UP`. · H-33

- [ ] **U11-T05 · Escaneo con Trivy**
  `make escanear` sobre las cuatro imágenes.
  **Criterio:** `trivy image --severity CRITICAL --exit-code 1 <imagen>` termina con 0 en las cuatro. · H-33

- [ ] **U11-T06 · Instalación de SynKra con GitOps**
  Flux en el `kind` local con una `Kustomization` que reconcilia `deploy/overlays/kind` de este repositorio. No toca los namespaces de los países: los productos los sigue desplegando SynKra sin Git.
  **Criterio:** `flux get kustomizations` muestra `synkra` en `Ready=True` y el backend y el front quedan con la revisión del repositorio. · H-34

- [ ] **U11-T07 · Resumen**
  **Criterio:** `aidlc-docs/construction/U11-endurecimiento/code/summary.md` contiene la salida real de los comandos de T01 a T06. · H-32

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-32 | T01, T02, T03, T07 |
| H-33 | T04, T05 |
| H-34 | T06 |

**Terminada cuando:** los criterios de H-32, H-33 y H-34 se cumplen en el `kind` local, o la historia que se cortó queda anotada en `DECISIONES.md`.
