# Plan de Code Generation — U-deploy

**Estado:** Part 1 (Planning). Este plan es la fuente única de verdad para la Parte 2. En este
trabajo **no se ejecuta la Parte 2**: no se escribe ningún archivo de código ni de configuración.
**Etapas omitidas por instrucción del usuario:** NFR Requirements, NFR Design e Infrastructure
Design de esta unidad (Functional Design ya estaba omitida). Todo el diseño de despliegue sale
de las historias, los requisitos, el PRD y las decisiones ya aprobadas, más los valores por
defecto de «Decisiones tomadas sin etapa de diseño», que se corrigen al aprobar este plan.

## Contexto de la unidad

| Campo | Valor |
|---|---|
| Propósito | Imágenes, base de Kustomize, overlays `co` y `pe`, `make deploy`, endurecimiento del clúster, GitOps con Argo CD y Job del agente |
| Historias (9) | US-2.1, US-2.11, US-2.12, US-2.13, US-2.14, US-2.15, US-2.16, US-2.17, US-2.18 |
| Requisitos | RF-20 a RF-25, RF-37, RF-60, RF-62, RF-63; NFR-03, NFR-07, NFR-11, NFR-12 |
| Depende de | `U-core` (JAR ejecutable), `U-plugins` (JAR de plugin), `U-agent` (`bin/synkra`, `bin/synkra-askpass`, `deploy/agent-identity/`) |
| Propietario de | `images/`, `deploy/base`, `deploy/overlays`, `deploy/security`, `deploy/gitops`, `deploy/agent-job`, objetivos `images`, `deploy` y `status` del `Makefile` |
| Orden | Imágenes → base y overlays → despliegue → seguridad → Job → GitOps → escaneo y documentación |

**Convención de comandos.** Se ejecutan en la raíz del repositorio, en Linux o WSL2, con el clúster
`kind-synkra` creado por `make cluster`. Los pasos marcados **[PERSONA]** requieren credenciales
que el agente no tiene: el agente nunca ejecuta `kubectl apply` ni `helm` (AUTONOMIA-01).
`make deploy`, `make argocd-install` y `make agent-job` los ejecuta una persona.

## Decisiones tomadas sin etapa de diseño (corrígelas al aprobar)

**NFR (de requisitos y de la unidad `U-agent`):**

| # | Decisión por defecto |
|---|---|
| D1 | Despliegue de un país en menos de 600 s con el clúster ya creado (NFR-07) |
| D2 | Pods endurecidos: no root, sistema de archivos raíz de solo lectura, sin escalada de privilegios, todas las capacidades eliminadas, perfil `seccomp` `RuntimeDefault` (NFR-12) |
| D3 | Recursos del kernel: solicitud de 100 m de CPU y 256 MiB; límite de memoria de 512 MiB |
| D4 | Escaneo de imágenes con Trivy: cero hallazgos CRITICAL en las tres imágenes (NFR-AG-MT3) |

**Infraestructura:**

| # | Decisión por defecto |
|---|---|
| D5 | Tres imágenes locales, cargadas en kind con `kind load docker-image` (sin registro, requisitos Q11-A): `synkra/core:dev`, `synkra/plugins:dev`, `synkra/agent:dev`. `imagePullPolicy: IfNotPresent` |
| D6 | `synkra/core`: base distroless para Java 21 en su variante no root, usuario numérico 65532, con el JAR ejecutable de `core`; sin JAR de plugin (US-2.11) |
| D7 | `synkra/plugins` (única, requisitos Q7-A de unidades): base mínima con `cp` (BusyBox estática), sin shell interactivo, usuario no root; contiene `/plugins/co-kyc.jar` y `/plugins/pe-kyc.jar` |
| D8 | `synkra/agent`: JRE 21 mínimo con `git` y `gh`, usuario no root, con el JAR del agente, `bin/synkra` y `bin/synkra-askpass`. No es distroless porque el agente necesita `git` y `gh` |
| D9 | Kernel: un `Deployment` de una réplica y un `Service` ClusterIP en el puerto 8080; init container de `synkra/plugins` que copia solo `/plugins/${PLUGIN_ID}.jar` a un volumen `emptyDir` montado en `/plugins` del kernel; `SYNKRA_COUNTRY` y `SYNKRA_PLUGINS_DIR` por `ConfigMap` generado por Kustomize en cada overlay |
| D10 | Probes sobre `/actuator/health/liveness` y `/actuator/health/readiness` (las expone `U-core`, paso 10) |
| D11 | Namespaces `synkra-co`, `synkra-pe` y `synkra` (este último lo crea `U-agent`); etiquetas de Pod Security `restricted` en los tres, aplicadas con un parche en esta unidad sin editar los archivos de `U-agent` |
| D12 | `NetworkPolicy` `agent-egress` en `synkra`: egreso denegado por defecto para los pods del agente, con permisos solo hacia el DNS del clúster y TCP 443 hacia rangos de IP de GitHub (de `api.github.com/meta`) y de `api.anthropic.com` (resueltos al generar). Los rangos se generan con un script y se versionan; pueden quedar desactualizados |
| D13 | Secret `agent-secrets` en `synkra` creado por una persona con `kubectl create secret generic ... --from-file`; montado como volumen de solo lectura en `/secrets` con modo `0400`; las variables de entorno apuntan a archivos (`ANTHROPIC_API_KEY_FILE`, `AGENT_TOKEN_FILE`). Nunca `secretKeyRef` ni el valor en el repositorio |
| D14 | Job del agente en `synkra`: ServiceAccount `agent` sin montaje del token, `restartPolicy: Never`, `backoffLimit: 0`, `activeDeadlineSeconds: 300`; init container que clona el repositorio con `git clone --depth 1` en un volumen `emptyDir` y copia `cambios/<id>.yaml` desde un `ConfigMap` que crea la persona; PVC `agent-state` (100 MiB) compartido por los Jobs para el presupuesto, el log y los informes (`SYNKRA_STATE_DIR`, `SYNKRA_LOG_DIR` y `SYNKRA_REPORT_DIR` en el PVC), porque `propose` debe leer el informe que escribió un `analyze` anterior |
| D15 | Los Jobs se llaman `agent-analyze` y `agent-propose`; `make agent-job` borra el anterior del mismo nombre antes de crear el nuevo |
| D16 | Argo CD instalado en el namespace `argocd` a partir del manifiesto oficial de una versión fija (la estable más reciente al generar). Dos `Application` (`synkra-co`, `synkra-pe`) sobre `main` del repositorio `Jacarrilloc/SynKra`, con sincronización automática y `selfHeal` |
| D17 | Se asume que el repositorio es legible por Argo CD y por el init container sin credenciales (público). Si es privado, hace falta una credencial de solo lectura y este plan debe ampliarse |
| D18 | `kindnet` puede no hacer cumplir las `NetworkPolicy`. El paso 14 lo comprueba; si no se cumplen, se informa y se pide a la persona decidir un CNI que sí las aplique |

## Interfaces y contratos que entrega

| Entrega | Consumidor | Contrato |
|---|---|---|
| `make images`, `make deploy COUNTRY=xx`, `make agent-job` | Personas | Objetivos del `Makefile` |
| `deploy/overlays/{co,pe}` | Personas, Argo CD | Renderizan con `kubectl kustomize` sin error |
| `deploy/agent-job/` | Personas | Job reproducible `agent-analyze` y `agent-propose` |
| `deploy/gitops/` | Argo CD | Dos aplicaciones, una por país |

## Estructura de código (rutas exactas)

```
images/core/Dockerfile        images/plugins/Dockerfile        images/agent/Dockerfile
deploy/base/{kustomization,deployment,service}.yaml
deploy/overlays/co/{kustomization,namespace}.yaml     deploy/overlays/pe/...
deploy/agent-job/base/{kustomization,networkpolicy,pvc,psa-patch}.yaml
deploy/agent-job/job/{kustomization,job-analyze,job-propose}.yaml
deploy/security/egress-cidrs.yaml                     (generado)
deploy/gitops/{application-co,application-pe}.yaml    deploy/gitops/README.md
deploy/README.md
scripts/{gen-egress-cidrs,verify-countries,verify-hardening}.sh
Makefile                       (objetivos images, deploy, status, scan, agent-job, argocd-install)
scripts/{doctor.sh,setup-linux.sh}   (se añaden argocd y trivy)
aidlc-docs/construction/U-deploy/code/summary.md
```

## Pasos de generación

Cada paso termina con el comando que demuestra que está hecho (AUTONOMIA-03).

- [ ] **Paso 1 — Herramientas y entorno (US-2.1).**
  Añadir `argocd` y `trivy` a `scripts/doctor.sh` y `scripts/setup-linux.sh` (versiones fijas al generar). Confirmar que `make doctor` y `make cluster` siguen funcionando. *Historia US-2.1.*
  *Verificación:* `make doctor` termina en `0`, `./scripts/doctor.sh | grep -cE "argocd|trivy"` imprime `2`, y `make cluster && kubectl get nodes --context kind-synkra | grep -c " Ready"` imprime al menos `1`.

- [ ] **Paso 2 — Imagen del kernel (D6).**
  `images/core/Dockerfile` y `.dockerignore`: etapa de compilación con Maven y etapa final distroless no root, usuario 65532, solo el JAR ejecutable. *Historia US-2.11.*
  *Verificación:* `docker build -f images/core/Dockerfile -t synkra/core:dev .` termina en `0`; `docker inspect synkra/core:dev --format '{{.Config.User}}'` imprime un valor que no es `root` ni `0`; `docker create --name t synkra/core:dev && docker export t | tar -t | grep -cE "(co|pe)[^/]*\.jar$"; docker rm t` imprime `0` para los JAR de plugin.

- [ ] **Paso 3 — Imagen única de plugins (D7).**
  `images/plugins/Dockerfile`: BusyBox no root con los JAR de `U-plugins` copiados a `/plugins/co-kyc.jar` y `/plugins/pe-kyc.jar`. *Historia US-2.11 y Q7-A.*
  *Verificación:* `docker build -f images/plugins/Dockerfile -t synkra/plugins:dev .` termina en `0` y `docker run --rm synkra/plugins:dev ls /plugins` imprime los dos JAR.

- [ ] **Paso 4 — Imagen del agente (D8).**
  `images/agent/Dockerfile`: JRE 21 mínimo, `git`, `gh`, usuario no root, JAR del agente, `bin/synkra` y `bin/synkra-askpass`. *Historia US-2.18.*
  *Verificación:* `docker build -f images/agent/Dockerfile -t synkra/agent:dev .` termina en `0` y `docker run --rm synkra/agent:dev --help | grep -c -e analyze -e propose` imprime `2`.

- [ ] **Paso 5 — Objetivo `make images` y carga en kind.**
  Construir las tres imágenes y cargarlas con `kind load docker-image --name synkra`.
  *Verificación:* `make images` termina en `0` y `docker exec synkra-control-plane crictl images | grep -c "synkra/"` imprime `3`.

- [ ] **Paso 6 — Base de Kustomize del kernel (D2, D3, D9, D10).**
  `deploy/base`: `Deployment`, `Service`, init container, volúmenes `emptyDir` (plugins y `/tmp`), probes, `securityContext` completo, recursos, `automountServiceAccountToken: false`. *Historias US-2.4 (criterio de probes), US-2.12.*
  *Verificación:* `kubectl kustomize deploy/base | grep -cE "readinessProbe|livenessProbe"` imprime `2`, y `kubectl kustomize deploy/base | grep -cE "runAsNonRoot: true|readOnlyRootFilesystem: true|allowPrivilegeEscalation: false"` imprime al menos `3`.

- [ ] **Paso 7 — Overlays `co` y `pe` (D9, D11).**
  Un overlay por país: namespace con etiquetas de Pod Security `restricted`, `ConfigMap` generado con `SYNKRA_COUNTRY` y `PLUGIN_ID`. Sin imágenes ni ramas por país. *Historias US-2.12, US-2.15.*
  *Verificación:* `kubectl kustomize deploy/overlays/co >/dev/null && kubectl kustomize deploy/overlays/pe >/dev/null`; `kubectl kustomize deploy/overlays/pe | grep -c "kind: Deployment"` imprime `1`; `kubectl kustomize deploy/overlays/pe | grep -c "initContainers"` imprime `1`; `git branch -a | grep -Ei "origin/(co|pe)$"` no imprime nada.

- [ ] **Paso 8 — `make deploy COUNTRY=xx` (NFR-07).**
  Objetivo que valida `COUNTRY`, aplica el overlay y espera `rollout status` (300 s de tope), e imprime el tiempo. Objetivo `status` ya existe. *Historia US-2.13.*
  *Verificación [PERSONA]:* `time make deploy COUNTRY=pe` termina en `0` en menos de 600 s, `kubectl get pods -n synkra-pe` muestra el pod `Running` y `1/1`, y `kubectl get cm -n synkra-pe -o yaml | grep -cw "pe"` imprime al menos `1`.

- [ ] **Paso 9 — Verificación del país activo (US-2.14).**
  `scripts/verify-countries.sh`: abre un `port-forward` por namespace y comprueba con `curl` que `/plugins` lista solo el plugin de su país y que la petición KYC de otro país da 404. *Historia US-2.14.*
  *Verificación:* `bash -n scripts/verify-countries.sh`; con `co` y `pe` desplegados, `scripts/verify-countries.sh` termina en `0` e imprime `PASS` dos veces.

- [ ] **Paso 10 — Endurecimiento de pods (D2, D11).**
  `scripts/verify-hardening.sh`: comprueba la etiqueta `pod-security.kubernetes.io/enforce` en los tres namespaces y el `securityContext` de los pods. *Historia US-2.15.*
  *Verificación [PERSONA]:* `kubectl get ns synkra-co -o jsonpath='{.metadata.labels.pod-security\.kubernetes\.io/enforce}'` imprime `restricted`, `kubectl get pod -n synkra-co -o jsonpath='{..securityContext}'` contiene `runAsNonRoot` y `readOnlyRootFilesystem`, y `scripts/verify-hardening.sh` termina en `0`.

- [ ] **Paso 11 — Base del Job: parche de Pod Security, PVC y NetworkPolicy (D11, D12, D14).**
  `deploy/agent-job/base`: incluye `deploy/agent-identity/` sin editarla, un parche que etiqueta el namespace `synkra` como `restricted`, el PVC `agent-state` y la `NetworkPolicy` `agent-egress`. *Historias US-2.15, US-2.16.*
  *Verificación:* `kubectl kustomize deploy/agent-job/base | grep -cE "kind: (NetworkPolicy|PersistentVolumeClaim|Namespace|ServiceAccount)"` imprime `4`; `kubectl kustomize deploy/agent-job/base | grep -cE "kind: (Role|ClusterRole|RoleBinding|ClusterRoleBinding)"` imprime `0`.

- [ ] **Paso 12 — Rangos de egreso (D12).**
  `scripts/gen-egress-cidrs.sh`: consulta `api.github.com/meta` y resuelve `api.anthropic.com`, y escribe `deploy/security/egress-cidrs.yaml`, que la `NetworkPolicy` consume. Documenta que los rangos pueden desactualizarse. *Historia US-2.16.*
  *Verificación:* `bash -n scripts/gen-egress-cidrs.sh`; `scripts/gen-egress-cidrs.sh && grep -c "cidr:" deploy/security/egress-cidrs.yaml` imprime al menos `2`.

- [ ] **Paso 13 — Secret por archivo (D13).**
  Documentar en `deploy/README.md` el comando para crear `agent-secrets` y asegurar que ningún manifiesto lleva el valor ni `secretKeyRef`. *Historia US-2.15.*
  *Verificación:* `grep -rc "secretKeyRef" deploy | grep -v ":0"` no imprime nada; `grep -rcw "ANTHROPIC_API_KEY" deploy | grep -v ":0"` no imprime nada; `grep -r "ANTHROPIC_API_KEY_FILE" deploy | wc -l` imprime al menos `1`.

- [ ] **Paso 14 — Aplicación de la política de egreso y prueba negativa [PERSONA].**
  Aplicar la base del Job y comprobar que un pod con las etiquetas del agente no alcanza un destino no permitido. *Historia US-2.16.*
  *Verificación:* `kubectl describe networkpolicy -n synkra agent-egress` lista solo el DNS del clúster y los rangos de GitHub y de la API de Anthropic, y `kubectl run egress-test -n synkra --rm -i --restart=Never --labels=app.kubernetes.io/name=synkra-agent --image=busybox -- wget -T 5 -qO- https://example.com` termina distinto de cero. Si termina en `0` (la política no se cumple), el paso falla y se informa D18.

- [ ] **Paso 15 — Job del agente (D14, D15).**
  `deploy/agent-job/job`: plantillas `agent-analyze` y `agent-propose` con la ServiceAccount `agent`, init container de clonado, volúmenes, `securityContext` y variables por archivo. Objetivo `make agent-job ACTION=analyze|propose CHANGE=<id>`: crea el `ConfigMap` del cambio, borra el Job anterior del mismo nombre y aplica el nuevo. *Historia US-2.18.*
  *Verificación [PERSONA]:* `kubectl kustomize deploy/agent-job/job | grep -c "serviceAccountName: agent"` imprime `2`; tras `make agent-job ACTION=analyze CHANGE=2026-001`, `kubectl get job -n synkra agent-analyze -o jsonpath='{.status.succeeded}'` imprime `1` y `kubectl get pod -n synkra -l job-name=agent-analyze -o jsonpath='{.items[0].spec.serviceAccountName}'` imprime `agent`.

- [ ] **Paso 16 — Argo CD (D16, D17).**
  `make argocd-install` (aplica el manifiesto de versión fija en `argocd`) y `deploy/gitops/application-co.yaml` y `application-pe.yaml`; `deploy/gitops/README.md` con `argocd login --core`. *Historia US-2.17.*
  *Verificación [PERSONA]:* `make argocd-install && kubectl apply -f deploy/gitops/` y luego `argocd app list` muestra `synkra-co` y `synkra-pe` con `Synced` y `Healthy`; `argocd app get synkra-pe --refresh -o json | jq -r .status.sync.revision` imprime el mismo valor que `git rev-parse origin/main`.

- [ ] **Paso 17 — Escaneo de imágenes (D4).**
  Objetivo `make scan` con `trivy image --severity CRITICAL --exit-code 1` sobre las tres imágenes.
  *Verificación:* `make scan` termina en `0`. Si una imagen base trae un hallazgo CRITICAL sin corrección, el paso falla y se informa al usuario con el nombre del paquete, sin relajar el umbral por cuenta propia.

- [ ] **Paso 18 — Regla de contenido de los manifiestos.**
  Comprobar que ningún manifiesto contiene comandos de fusión, `kubectl apply` ejecutado por el agente ni referencias a imágenes por país.
  *Verificación:* `grep -rniE "kubectl apply|helm" images deploy/agent-job deploy/base | grep -v "README" | wc -l` imprime `0`, y `kubectl kustomize deploy/overlays/co | grep -c "synkra/core:dev"` imprime `1` (la misma imagen que `pe`).

- [ ] **Paso 19 — README de despliegue.**
  `deploy/README.md`: orden de uso (`make cluster`, `make images`, `make deploy COUNTRY=xx`, `make agent-job`), creación del Secret y del PVC, diagrama de la arquitectura de despliegue y limitaciones (egreso por rangos, CNI, repositorio público).
  *Verificación:* `grep -cE "make cluster|make images|make deploy|make agent-job" deploy/README.md` imprime al menos `4` y `grep -c "kindnet" deploy/README.md` imprime al menos `1`.

- [ ] **Paso 20 — Resumen de la unidad.**
  `aidlc-docs/construction/U-deploy/code/summary.md` (solo Markdown): archivos creados, pasos a historias y estado de las decisiones D1 a D18.
  *Verificación:* `test -s aidlc-docs/construction/U-deploy/code/summary.md && echo OK` imprime `OK`.

- [ ] **Paso 21 — Comprobación final de la unidad.**
  *Verificación [PERSONA]:* `make doctor && make images && make scan && time make deploy COUNTRY=co && time make deploy COUNTRY=pe && scripts/verify-countries.sh && scripts/verify-hardening.sh && git status --porcelain | grep -vE "^( M|\?\?) (images/|deploy/|scripts/|Makefile|aidlc-docs/)"` termina con la última parte sin imprimir nada.

## Cobertura de historias

| Historia | Pasos |
|---|---|
| US-2.1 | 1 |
| US-2.11 | 2, 3 |
| US-2.12 | 6, 7 |
| US-2.13 | 8 |
| US-2.14 | 9 |
| US-2.15 | 7, 10, 11, 13 |
| US-2.16 | 11, 12, 14 |
| US-2.17 | 16 |
| US-2.18 | 4, 15 |

Las nueve historias aparecen en al menos un paso. Criterios compartidos: US-2.4 (probes) en el paso 6; US-1.12 (ServiceAccount del agente) usa el namespace que crea `U-agent`; US-2.18 usa la ServiceAccount de `U-agent`.

## Enmiendas propuestas a historias aprobadas (se aplican al aprobar este plan)

1. **US-2.15, criterio 3:** `grep -c ANTHROPIC_API_KEY` coincidiría con la variable `ANTHROPIC_API_KEY_FILE`. Se reemplaza por `grep -cw ANTHROPIC_API_KEY`, que no coincide con el nombre que termina en `_FILE`.
2. **US-2.16, criterio 1:** «solo los destinos de Anthropic y GitHub» omite el DNS del clúster, que es imprescindible. Se amplía a «los destinos de Anthropic y GitHub y el DNS del clúster».

## Secciones estándar que no aplican

| Sección estándar | Motivo |
|---|---|
| Business Logic, API Layer, Repository Layer, Frontend, Migraciones | Esta unidad no tiene lógica de negocio ni persistencia |

## Cumplimiento de reglas

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. Ningún paso hace que el agente ejecute `git merge`, `gh pr merge`, `git push` a `main`, `kubectl apply` ni `helm install`; `make deploy`, `make argocd-install` y `make agent-job` los ejecuta una persona. El Job usa la ServiceAccount `agent`, sin permisos de escritura, y el paso 18 lo comprueba |
| AUTONOMIA-02 | Cumple. Ninguna tarea llama al modelo; el Job usa el proveedor configurado y el presupuesto del PVC |
| AUTONOMIA-03 | Cumple. Los 21 pasos nombran su comando de verificación |

## Resumen del alcance

21 pasos; 3 Dockerfile, aproximadamente 20 manifiestos YAML, 3 scripts, 6 objetivos del
`Makefile`, 2 archivos de entorno modificados y 2 README. Sin código Java.
