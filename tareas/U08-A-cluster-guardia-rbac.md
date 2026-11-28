# U08-A: clúster `kind-synkra`, registro local, guardia de contexto, namespaces y RBAC

**Unidad:** U08: `Instalación y límite`
**Tareas del plan que agrupa:** U08-T01, U08-T02, U08-T03 y U08-T04
**Historias que implementa:** H-26 (T01, T02, T03), H-27 (T04); contribuye a H-13 (primera capa del límite, que cierra U08-C con `make verificar-limite`)
**Depende de:** U01 a U07 (terminadas). Sale de `main` `8306ee7`. Corre **en paralelo con U08-B**, con el reparto de archivos de la decisión 1: esta tarea es la **dueña del `Makefile`** y de `deploy/kind/`. No usa nada de U08-B.

---

## Alcance

**Dentro** (una línea, concreta):

> `deploy/kind/cluster.yaml` (control plane y worker, `80` y `443` en `127.0.0.1`), el registro `kind-registry` en `127.0.0.1:5001` conectado a la red `kind` y configurado en los nodos, el kubeconfig propio `.kube/kind-synkra.yaml` (ignorado por git), la guardia de contexto `scripts/guardia-contexto.sh` en todo objetivo del `Makefile` que toque el clúster, y `deploy/base/` con los namespaces `synkra`, `synkra-co` y `synkra-pe` y el RBAC de `synkra/deployer` (un `Role` y un `RoleBinding` por país, ningún `ClusterRole` ni `ClusterRoleBinding`), aplicados y leídos de vuelta en `kind-synkra`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier clúster que no sea `kind-synkra` (AUTONOMIA-01). En esta máquina corren `gitops2` y `m08-gitops1` (de otros cursos) y hay contenedores parados `gitops2-registry`, `gitops1-registry`, `gitops2-gitea` y `gitops1-gitea`: **ninguna orden los lista, los toca, los para ni los borra**. Prohibidos `kind get clusters`, `kind delete clusters`, `docker ps -a` sin filtro de nombre, `kubectl config get-contexts` sobre `~/.kube/config` y todo `kubectl` sin `--kubeconfig` (o sin `KUBECONFIG` apuntando al archivo del proyecto). Nada lee ni escribe `~/.kube/config` (la única excepción es el `stat` de no interferencia de CA-8, que no lo abre).
- `sudo`, `/etc/hosts`, `kubectl port-forward` hacia fuera de `127.0.0.1`, y publicar puertos en `0.0.0.0`.
- Lo de U08-B: `deploy/base/backend/`, `deploy/overlays/`, los `Dockerfile` y `.dockerignore`, `scripts/imagenes.sh`, `scripts/verificar-imagenes.sh`, `scripts/verificar-backend.py`, `scripts/certificado-tls.sh`. No se construye ni se sube ninguna imagen de SynKra.
- Lo de U08-C: Ingress NGINX, el secreto TLS, `make imagenes`, el `docker push`, `kubectl apply -k deploy/overlays/kind`, `make verificar-limite` y el `make install` completo. Aquí `install` solo llega hasta `aplicar-base` (decisión 6); U08-C lo amplía.
- Lo de U08-D y U08-E: `scripts/e2e-api.sh`, el formato de error y el resumen.
- Cualquier cambio en `backend/`, `core/`, `init-descarga/`, `plugins/`, `verificador/`, `plugin-api/`, en los scripts existentes (`setup-linux.sh`, `doctor.sh`, `control-*.sh`, `verificar-grafo.py`) o en `aidlc-docs/`, `CANDIDATAS.md`, `unidades-y-tareas.md`, el plan de U08 y `loop/`.
- `PodSecurity` (etiquetas `pod-security.kubernetes.io/*`), `NetworkPolicy`, `ResourceQuota` y `LimitRange`: no están en el diseño de U08. Si hacen falta, son candidata.
- El `ConfigMap` `local-registry-hosting` de `kube-public` (es solo documentación del registro para otras herramientas): no se crea.
- Descargar herramientas (`yq`, `kubeconform`, `shellcheck`…) o imágenes nuevas: todo lo que hace falta ya está en la caché de Docker (decisión 3).

---

## Archivos de contexto

- `loop/CONTINUAR.md` («Lo que sigue», punto 2: agrupación y reglas de U08)
- `aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md` (U08-T01 a T04) y `unidades-y-tareas.md` (sección U08)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (fila «Límite (P2)», «Lo que la instalación crea y lo que no») y `deployment-architecture.md` (diagrama y «Orden de `make install`»)
- `Makefile` y `.gitignore` de `8306ee7` (los objetivos `cluster`, `cluster-delete` y `status` actuales usan el kubeconfig global y `kind get clusters`: se reescriben, decisión 5)
- `backend/src/main/java/io/synkra/kubernetes/AdaptadorFabric8.java` (`existeNamespace`: `GET` del namespace del país; `aplicar`: server-side apply de ConfigMap, Service y Deployment con `fieldManager` `synkra`) y `LectorPais.java` (`get` y `list` de Deployments, ConfigMaps y Pods; `list` de `events` del grupo `""`; `pods/log` con `tailingLines`). De ahí salen los verbos de la decisión 8.
- `backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java` y `CuentaServicio.java` (el backend solo usa el token de su cuenta de servicio; nunca un kubeconfig)
- `backend/src/main/java/io/synkra/kubernetes/GuardiaNamespace.java` (segunda capa: solo `synkra-<dos letras>`)
- `CANDIDATAS.md` (C-75: server-side apply sin probar contra un servidor real, se verá en U08-C con el RBAC de esta tarea)
- Plantillas de estilo: `tareas/U07-G-fronteras-resumen.md` y `tareas/U06-A-registro-base.md`

---

## Decisiones del orquestador

1. **Reparto de archivos con U08-B** (corren a la vez; ninguna ruta se comparte):
   - **U08-A (esta):** `Makefile`, `.gitignore`, `deploy/kind/cluster.yaml`, `deploy/base/kustomization.yaml`, `deploy/base/namespaces.yaml`, `deploy/base/rbac.yaml`, `scripts/guardia-contexto.sh`, `scripts/registro-local.sh`, `scripts/verificar-base.py`, `bitacoras/U08-A.md`, `bitacoras/U08-A-evidencia/` y `revisiones/U08-A/`.
   - **U08-B:** `deploy/base/backend/`, `deploy/overlays/kind/`, los Dockerfile y `.dockerignore` de las imágenes, y sus scripts. U08-B no toca el `Makefile` ni `.gitignore`; esta tarea añade a `.gitignore` también la línea que U08-B necesita (`/.tls/`, decisión 4).
   - **Lo que une las dos lo hace U08-C:** añadir `- backend` a `deploy/base/kustomization.yaml`, cambiar el overlay a `../../base` y los objetivos `imagenes`, `ingress`, `tls` e `install` completo. Esta tarea **no** deja huecos ni líneas comentadas para U08-C.
2. **Nombres fijos.**
   - Clúster `kind` `synkra`; contexto `kind-synkra`; nodos `synkra-control-plane` y `synkra-worker`.
   - Kubeconfig del proyecto: `$(CURDIR)/.kube/kind-synkra.yaml` en el `Makefile` (variable `KUBECONFIG_SYNKRA`, con `?=`), y el `Makefile` exporta `KUBECONFIG_SYNKRA` y hace `export KUBECONFIG := $(KUBECONFIG_SYNKRA)` para que ningún `kubectl` ni `kind` de una receta caiga en `~/.kube/config`. Además, cada `kubectl` y cada `kind` de las recetas y scripts lleva `--kubeconfig "$KUBECONFIG_SYNKRA"` explícito.
   - El nombre del clúster **no** es una variable: `synkra` va escrito en las recetas. Se quita `CLUSTER ?= synkra` (con él, `make cluster-delete CLUSTER=gitops2` borraría otro clúster). `COUNTRY ?= co` se queda.
   - Registro: contenedor `kind-registry`, con la etiqueta `io.synkra.registro=true`, `--restart=always`, publicado en `127.0.0.1:5001` → `5000`, conectado a la red Docker `kind`. En los nodos, `localhost:5001` resuelve a `http://kind-registry:5000` por `/etc/containerd/certs.d/localhost:5001/hosts.toml`.
   - Namespaces `synkra`, `synkra-co` y `synkra-pe`; ServiceAccount `deployer` en `synkra`; en cada país, `Role` `synkra-deployer` y `RoleBinding` `synkra-deployer`.
   - Etiquetas: los tres namespaces llevan `app.kubernetes.io/part-of: synkra`; `synkra-co` lleva `synkra.io/pais: co` y `synkra-pe` lleva `synkra.io/pais: pe`; `synkra` **no** lleva `synkra.io/pais` (no es un país). El ServiceAccount, los Role y los RoleBinding llevan `app.kubernetes.io/part-of: synkra`.
3. **Imágenes, solo de la caché local y por digest** (no se descarga nada; esta máquina es `aarch64`, `linux/arm64`, y las dos están en la caché con su índice multi-arquitectura):
   - nodo: `kindest/node@sha256:3489c7674813ba5d8b1a9977baea8a6e553784dab7b84759d1014dbd78f7ebd5` en los dos nodos de `cluster.yaml`. El codificador comprueba y pega en la bitácora qué versión es (`docker run --rm --entrypoint cat kindest/node@sha256:3489c7… /kind/version`); se espera `v1.37.x`. Si no lo es, se anota y se sigue con esa imagen (no se descarga otra).
   - registro: `registry@sha256:1be55279f18a2fe1a74edf2664cac61c1bea305b7b4642dab412e7affdcb3e33`.
   - Si `kind create cluster` o `docker run` intentan descargar (la imagen no está), el codificador **para** y lo reporta.
4. **`.gitignore`.** Se añaden, al final y con un comentario `# U08: kubeconfig del proyecto y certificados locales`, las líneas `/.kube/` y `/.tls/`. No se cambia ninguna línea existente.
5. **`Makefile`** (se reescribe; se conservan `help`, `setup`, `doctor` y `COUNTRY`). Objetivos, todos con su `## ayuda` y en `.PHONY`:
   - `guardia`: corre `scripts/guardia-contexto.sh`.
   - `cluster`: `scripts/guardia-contexto.sh --antes-de-crear`; si `kind get kubeconfig --name synkra` falla (no existe), `kind create cluster --name synkra --config deploy/kind/cluster.yaml --kubeconfig "$KUBECONFIG_SYNKRA" --wait 180s`; si existe, `kind export kubeconfig --name synkra --kubeconfig "$KUBECONFIG_SYNKRA"`. Después, `scripts/guardia-contexto.sh` (completa) y `kubectl wait --for=condition=Ready nodes --all --timeout=180s`. Es idempotente: si el clúster existe, no lo recrea. Crea `.kube/` con `0700` si falta.
   - `registro`: `guardia` y `scripts/registro-local.sh`.
   - `aplicar-base`: `guardia` y `kubectl --kubeconfig "$KUBECONFIG_SYNKRA" apply -k deploy/base`.
   - `install`: en este orden `scripts/guardia-contexto.sh --antes-de-crear` (primera línea: si el kubeconfig apunta a otro contexto, sale **antes** de crear o exportar nada), `cluster`, `registro` y `aplicar-base`. U08-C lo amplía.
   - `status`: `guardia`, y `get nodes -o wide` y `get pods -A` con `--kubeconfig`.
   - `cluster-delete`: `kind delete cluster --name synkra --kubeconfig "$KUBECONFIG_SYNKRA"` y, si `kind-registry` existe **y** tiene la etiqueta `io.synkra.registro=true`, `docker rm -f kind-registry`. Si existe sin la etiqueta, no lo toca y lo dice. No pasa por la guardia de contexto (borra por nombre, y el nombre es fijo).
   - Ningún objetivo usa `kind get clusters` ni un nombre de clúster que no sea `synkra`.
6. **`scripts/guardia-contexto.sh`** (`bash`, `set -euo pipefail`; lee la ruta de `KUBECONFIG_SYNKRA`, con `$PWD/.kube/kind-synkra.yaml` por defecto). En este orden, y cada rechazo sale con código 1 y una línea en stderr que empieza por el texto exacto entre comillas:
   1. Si la ruta, resuelta con `realpath -m`, es `$HOME/.kube/config`: `kubeconfig no permitido: <ruta>`. No abre el archivo.
   2. Si el archivo no existe: con `--antes-de-crear`, sale con 0 (`sin kubeconfig todavía: lo crea make cluster`); sin la opción, `falta el kubeconfig <ruta>: corre make cluster`.
   3. `kubectl --kubeconfig <ruta> config current-context`; si falla o está vacío: `contexto no permitido: sin contexto actual en <ruta>`; si no es `kind-synkra`: `contexto no permitido: <contexto> (solo kind-synkra) en <ruta>`.
   4. El servidor del contexto actual (`config view --minify -o jsonpath='{.clusters[0].cluster.server}'`) se compara con el de `kind get kubeconfig --name synkra` (el mismo `jsonpath` sobre esa salida). Si el clúster `synkra` no existe: con `--antes-de-crear`, sale con 0; sin la opción, `contexto no permitido: el clúster kind synkra no existe`. Si los servidores difieren: `contexto no permitido: kind-synkra apunta a <servidor> y el clúster kind synkra está en <servidor>`.
   5. Si todo cuadra: `contexto permitido: kind-synkra (<servidor>)` en stdout y 0.
   Todo fallo de un comando externo (`kubectl`, `kind`, `realpath`) da su línea de error con el comando y el código, nunca un error mudo de `set -e`.
7. **`scripts/registro-local.sh`** (`bash`, `set -euo pipefail`, con `trap` de limpieza):
   - Si `kind-registry` existe (`docker inspect kind-registry`): sin la etiqueta `io.synkra.registro=true`, sale con 1 (`kind-registry existe y no es de SynKra: no se toca`); con ella, lo arranca si está parado.
   - Si no existe: **antes** de crearlo, registra en el `trap` que lo creó esta corrida; lo crea con la imagen de la decisión 3; si un paso posterior de la misma corrida falla, el `trap` lo borra (solo el que creó esta corrida).
   - `docker network connect kind kind-registry` si no está conectado (se mira con `docker inspect -f '{{json .NetworkSettings.Networks}}'`).
   - Para cada nodo de `kind get nodes --name synkra` (solo ese clúster): `mkdir -p /etc/containerd/certs.d/localhost:5001` y escribe `hosts.toml` con `[host."http://kind-registry:5000"]`. Si el `config.toml` del nodo no tiene `config_path = "/etc/containerd/certs.d"`, se añade a `cluster.yaml` el `containerdConfigPatches` que da la guía de registro local de kind para esa versión de containerd (el codificador pega en la bitácora el `grep config_path` del nodo y la fuente del parche).
   - Espera a `curl -sf http://127.0.0.1:5001/v2/` (con `timeout`); si no responde, sale con 1 y el mensaje.
8. **RBAC (`deploy/base/rbac.yaml`).** Lo que de verdad usa el backend (contexto: `AdaptadorFabric8`, `LectorPais`):
   - `apiGroups: ["apps"]`, `resources: ["deployments"]`: `get`, `list`, `watch`, `create`, `update`, `patch` (server-side apply es `patch`, y crear con apply pide además `create`).
   - `apiGroups: [""]`, `resources: ["configmaps", "services"]`: los mismos seis verbos.
   - `apiGroups: [""]`, `resources: ["pods", "pods/log", "events"]`: `get`, `list`, `watch`.
   - `apiGroups: [""]`, `resources: ["namespaces"]`, `resourceNames: ["synkra-<país>"]` (el propio): `get`. Es lo que pide `existeNamespace`. Kubernetes trata el `GET` de un namespace como pedido **dentro** de ese namespace, así que un `Role` en `synkra-co` lo autoriza sin permiso de clúster. CA-6 lo demuestra con el token real.
   - **Nunca:** `delete`, `deletecollection`, `secrets`, `*` en verbos, recursos o grupos, `escalate`, `bind`, `impersonate`. Ningún `ClusterRole` ni `ClusterRoleBinding` en `deploy/base`. El `Role` `synkra-deployer` es idéntico (mismas reglas, mismo orden) en `synkra-co` y en `synkra-pe` salvo el `resourceNames` del namespace.
   - El ServiceAccount no fija `automountServiceAccountToken` (el Deployment del backend de U08-B lo pide en `true`).
   - Desviación del diseño que se anota en la bitácora: `infrastructure-design.md` no nombra `namespaces` ni `get`; se añade por `existeNamespace`.
9. **`deploy/base/kustomization.yaml`**: `apiVersion: kustomize.config.k8s.io/v1beta1`, `kind: Kustomization`, `resources: [namespaces.yaml, rbac.yaml]`, sin `namespace:` global (cada objeto lleva el suyo) y sin generadores.
10. **`scripts/verificar-base.py`** (se corre `kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py`; PyYAML 6 del sistema): comprueba, contra la decisión 2 y la 8, la lista **exacta** de objetos (`kind`, `namespace`, `name`), las etiquetas, las reglas de cada `Role` (exactas: grupos, recursos, verbos, `resourceNames`), los `subjects` y el `roleRef` de cada `RoleBinding`, y que no haya `Cluster*`, `*`, `delete` ni `secrets`. Imprime `OK base: <n> objetos` y sale con 0, o una línea `FALLA <regla>: <detalle>` por cada fallo y sale con 1. Entrada vacía o YAML inválido: `FALLA entrada: …` y 1.
11. **`cluster.yaml`**: `kind: Cluster`, `apiVersion: kind.x-k8s.io/v1alpha4`, `name: synkra`; nodo `control-plane` con `labels: {ingress-ready: "true"}` y `extraPortMappings` `80→80` y `443→443`, `protocol: TCP`, `listenAddress: "127.0.0.1"`; un nodo `worker`; los dos con la imagen de la decisión 3. Sin `networking` personalizado.
12. **Qué queda arriba al terminar.** Lo esperado: **`kind-synkra` y `kind-registry` se quedan corriendo** para U08-C, con `deploy/base` aplicado y `.kube/kind-synkra.yaml` en el worktree de esta tarea. U08-C, desde su propio worktree, lo recupera con `make cluster` (que exporta el kubeconfig sin recrear). Lo que sí se limpia al terminar: las copias de `mktemp -d`, los kubeconfig falsos, el token de CA-6 (caduca a los 10 minutos) y la imagen de prueba de CA-3 en los nodos y en Docker local (`crictl rmi`, `docker rmi`); la capa queda en el registro, que no permite borrar, y se anota. Para borrar todo a mano: `make cluster-delete`.
13. **Bitácora en `bitacoras/U08-A.md`**, evidencia en `bitacoras/U08-A-evidencia/` (un `caN.sh` versionado y su `caN.txt` con fecha, sha, comando y salida completa), y el revisor en `revisiones/U08-A/`. El revisor corre los CA contra el mismo `kind-synkra` (no lo borra salvo en CA-7, que lo vuelve a dejar arriba).

---

## Criterios de aceptación

En todos: `KC=$PWD/.kube/kind-synkra.yaml` desde la raíz del worktree; todo `kubectl` lleva `--kubeconfig "$KC"` (o uno falso en `mktemp -d`), nunca el global.

- [ ] **CA-1**: U08-T01. `make cluster` crea `kind-synkra` con dos nodos `Ready`, y es idempotente
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml
  make cluster; echo "salida=$?"
  kubectl --kubeconfig "$KC" config current-context
  stat -c '%a' "$KC" .kube; git check-ignore -v "$KC"
  kubectl --kubeconfig "$KC" get nodes -o json | python3 -I -c "import json,sys; d=json.load(sys.stdin); print(sorted((n['metadata']['name'], [c['status'] for c in n['status']['conditions'] if c['type']=='Ready'][0], n['metadata']['labels'].get('ingress-ready','-'), n['status']['nodeInfo']['architecture']) for n in d['items']))"
  docker port synkra-control-plane
  A=$(docker inspect -f '{{.State.StartedAt}}' synkra-control-plane); make cluster; echo "salida=$?"; B=$(docker inspect -f '{{.State.StartedAt}}' synkra-control-plane); [ "$A" = "$B" ] && echo "no-recreado"
  make -n cluster-delete CLUSTER=gitops2 | grep -c gitops2
  grep -nE 'kind get clusters|CLUSTER' Makefile; echo "variable=$?"
  ```
  Esperado: `salida=0` las dos veces; `kind-synkra`; `600` y `700`; `git check-ignore` nombra la línea `/.kube/` de `.gitignore`; `[('synkra-control-plane', 'True', 'true', 'arm64'), ('synkra-worker', 'True', '-', 'arm64')]`; `docker port` da `80/tcp -> 127.0.0.1:80` y `443/tcp -> 127.0.0.1:443` (más el puerto de la API en `127.0.0.1`), nada en `0.0.0.0`; `no-recreado`; `0` (el nombre `gitops2` no llega a ninguna receta); `variable=1`.

- [ ] **CA-2**: U08-T01. El registro local sirve en `127.0.0.1:5001` y está en la red `kind`
  ```bash
  make registro; echo "salida=$?"
  docker inspect -f '{{index .Config.Labels "io.synkra.registro"}} {{.Config.Image}} {{.HostConfig.RestartPolicy.Name}} {{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' kind-registry
  docker port kind-registry
  curl -sf http://127.0.0.1:5001/v2/; echo " v2=$?"
  for n in synkra-control-plane synkra-worker; do docker exec "$n" cat /etc/containerd/certs.d/localhost:5001/hosts.toml; done
  make registro; echo "salida=$?"
  ```
  Esperado: `salida=0` las dos veces (la segunda no crea otro contenedor: el `Id` de `docker inspect -f '{{.Id}}' kind-registry` antes y después es el mismo, se pega); `true registry@sha256:1be55279… always` y las redes incluyen `kind`; `5000/tcp -> 127.0.0.1:5001` y nada en `0.0.0.0`; `{} v2=0`; en cada nodo, `[host."http://kind-registry:5000"]`.

- [ ] **CA-3**: U08-T01. Un nodo saca de verdad una imagen de `localhost:5001` (sin crear ningún objeto de Kubernetes)
  ```bash
  docker tag registry@sha256:1be55279f18a2fe1a74edf2664cac61c1bea305b7b4642dab412e7affdcb3e33 localhost:5001/synkra-prueba-registro:1
  docker push localhost:5001/synkra-prueba-registro:1; echo "push=$?"
  curl -sf http://127.0.0.1:5001/v2/synkra-prueba-registro/tags/list
  for n in synkra-control-plane synkra-worker; do docker exec "$n" crictl pull localhost:5001/synkra-prueba-registro:1; echo "$n pull=$?"; done
  for n in synkra-control-plane synkra-worker; do docker exec "$n" crictl rmi localhost:5001/synkra-prueba-registro:1; done; docker rmi localhost:5001/synkra-prueba-registro:1
  ```
  Esperado: `push=0`; `{"name":"synkra-prueba-registro","tags":["1"]}`; `pull=0` en los dos nodos (`Image is up to date for sha256:…`). Después, las dos `rmi` limpian (se pega su salida).

- [ ] **CA-4**: U08-T02. La guardia rechaza todo kubeconfig que no sea el de `kind-synkra`, sin crear ni aplicar nada
  Guardar como `bitacoras/U08-A-evidencia/ca4.sh` y correrlo desde la raíz con `kind-synkra` arriba:
  ```bash
  KC=${KC:-$PWD/.kube/kind-synkra.yaml}     # en las copias de CA-8 se pasa el del worktree
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  falso() { printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "https://127.0.0.1:1"}\ncontexts:\n- name: %s\n  context: {cluster: c, user: u}\ncurrent-context: %s\nusers:\n- name: u\n  user: {token: falso}\n' "$1" "$1"; }
  falso otro > "$D/otro.yaml"
  falso kind-synkra > "$D/mismo-nombre.yaml"
  mkdir -p "$D/casa/.kube"; falso otro > "$D/casa/.kube/config"
  antes=$(kubectl --kubeconfig "$KC" get ns,sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}')
  sha_otro=$(sha256sum "$D/otro.yaml")
  for obj in install cluster registro aplicar-base status guardia; do
    KUBECONFIG_SYNKRA="$D/otro.yaml" make "$obj" >"$D/out" 2>&1; echo "$obj otro salida=$?"; grep -c '^contexto no permitido: otro (solo kind-synkra)' "$D/out"
  done
  KUBECONFIG_SYNKRA="$D/mismo-nombre.yaml" make install >"$D/out" 2>&1; echo "mismo-nombre salida=$?"; grep -c '^contexto no permitido: kind-synkra apunta a https://127.0.0.1:1' "$D/out"
  HOME="$D/casa" KUBECONFIG_SYNKRA="$D/casa/.kube/config" make guardia >"$D/out" 2>&1; echo "global salida=$?"; grep -c '^kubeconfig no permitido:' "$D/out"
  KUBECONFIG_SYNKRA="$D/no-existe.yaml" make guardia >"$D/out" 2>&1; echo "falta salida=$?"; grep -c '^falta el kubeconfig' "$D/out"
  [ "$sha_otro" = "$(sha256sum "$D/otro.yaml")" ] && echo "falso-intacto"
  despues=$(kubectl --kubeconfig "$KC" get ns,sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}')
  [ "$antes" = "$despues" ] && echo "nada-aplicado"
  KUBECONFIG_SYNKRA="$KC" make guardia; echo "real salida=$?"
  ```
  Esperado: para cada uno de los seis objetivos, `salida=` distinto de 0 y `1`; `mismo-nombre salida=` distinto de 0 y `1`; `global salida=` distinto de 0 y `1` (con un `HOME` falso: el caso del kubeconfig global nunca abre el `~/.kube/config` real, ni siquiera con la mutación «guardia-global» de CA-8); `falta salida=` distinto de 0 y `1`; `falso-intacto` (ni `cluster` ni `install` exportaron nada encima del kubeconfig falso); `nada-aplicado`; con el kubeconfig real, `contexto permitido: kind-synkra (https://127.0.0.1:<puerto>)` y `real salida=0`. La bitácora pega además `make -n install` con el falso para mostrar que la guardia es la primera receta.

- [ ] **CA-5**: U08-T03 y U08-T04. `deploy/base` tiene los tres namespaces y el RBAC, sin permisos de clúster
  ```bash
  kubectl kustomize deploy/base | grep -c 'kind: Namespace'
  kubectl kustomize deploy/base | grep -cE 'kind: Cluster(Role|RoleBinding)'
  kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "salida=$?"
  kubectl kustomize deploy/base | python3 -I -c "import sys,yaml; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in yaml.safe_load_all(sys.stdin) if o))"
  grep -nE "delete|secrets|'\*'|\"\*\"|- \*" deploy/base/*.yaml; echo "prohibidos=$?"
  ```
  Esperado: `3`; `0`; `OK base: 8 objetos` y `salida=0`; `[('Namespace', '-', 'synkra'), ('Namespace', '-', 'synkra-co'), ('Namespace', '-', 'synkra-pe'), ('Role', 'synkra-co', 'synkra-deployer'), ('Role', 'synkra-pe', 'synkra-deployer'), ('RoleBinding', 'synkra-co', 'synkra-deployer'), ('RoleBinding', 'synkra-pe', 'synkra-deployer'), ('ServiceAccount', 'synkra', 'deployer')]`; `prohibidos=1`.

- [ ] **CA-6**: U08-T03 y U08-T04. Aplicado en `kind-synkra`, leído de vuelta, y el límite comprobado con la cuenta real
  Guardar como `bitacoras/U08-A-evidencia/ca6.sh`:
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml
  make aplicar-base; echo "salida=$?"
  kubectl --kubeconfig "$KC" get ns synkra synkra-co synkra-pe -o jsonpath='{range .items[*]}{.metadata.name} {.metadata.labels.synkra\.io/pais}{"\n"}{end}'
  kubectl --kubeconfig "$KC" get role synkra-deployer -n synkra-co -o json | python3 -I -c "import json,sys; print(json.dumps(json.load(sys.stdin)['rules'], sort_keys=True))"
  kubectl --kubeconfig "$KC" get clusterrolebindings -o json | python3 -I -c "import json,sys; print([b['metadata']['name'] for b in json.load(sys.stdin)['items'] if any(s.get('kind')=='ServiceAccount' and s.get('name')=='deployer' and s.get('namespace')=='synkra' for s in b.get('subjects') or [])])"
  SA=system:serviceaccount:synkra:deployer
  for p in "create deployments.apps -n synkra-co" "patch deployments.apps -n synkra-pe" "update configmaps -n synkra-co" "create services -n synkra-pe" "get pods --subresource=log -n synkra-co" "list events -n synkra-pe" "watch pods -n synkra-co" "get namespaces/synkra-co -n synkra-co" \
           "delete deployments.apps -n synkra-co" "create pods -n synkra-co" "get secrets -n synkra-co" "create deployments.apps -n synkra" "create deployments.apps -n default" "create deployments.apps -n kube-system" "create namespaces" "list namespaces" "get namespaces/default -n default" "get namespaces/synkra-pe -n synkra-co"; do
    printf '%s: ' "$p"; kubectl --kubeconfig "$KC" auth can-i $p --as="$SA" 2>/dev/null; done
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  T=$(kubectl --kubeconfig "$KC" create token deployer -n synkra --duration=10m)
  S=$(kubectl --kubeconfig "$KC" config view --minify -o jsonpath='{.clusters[0].cluster.server}')
  kubectl --kubeconfig "$KC" config view --minify --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > "$D/ca.crt"
  printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "%s", certificate-authority: "%s"}\ncontexts:\n- name: deployer\n  context: {cluster: c, user: deployer}\ncurrent-context: deployer\nusers:\n- name: deployer\n  user: {token: "%s"}\n' "$S" "$D/ca.crt" "$T" > "$D/deployer.yaml"
  kubectl --kubeconfig "$D/deployer.yaml" get namespace synkra-co -o name; echo "propio=$?"
  kubectl --kubeconfig "$D/deployer.yaml" get namespace default -o name; echo "ajeno=$?"
  kubectl --kubeconfig "$D/deployer.yaml" get deployments -n synkra-co; echo "lista-pais=$?"
  kubectl --kubeconfig "$D/deployer.yaml" get deployments -n synkra; echo "lista-synkra=$?"
  kubectl --kubeconfig "$D/deployer.yaml" auth whoami -o jsonpath='{.status.userInfo.username}'; echo
  ```
  Esperado: `salida=0`; `synkra ` (sin país), `synkra-co co`, `synkra-pe pe`; las reglas son exactamente las de la decisión 8 con `resourceNames: ["synkra-co"]`; `[]` (ningún `ClusterRoleBinding` nombra a `synkra:deployer`); `yes` en las ocho primeras preguntas y `no` en las diez siguientes; con el token real del ServiceAccount (kubeconfig temporal sin certificado de cliente), `namespace/synkra-co` y `propio=0`, `Forbidden` y `ajeno=1`, `No resources found` y `lista-pais=0`, `Forbidden` y `lista-synkra=1`, y `system:serviceaccount:synkra:deployer`. El kubeconfig temporal con el token se borra en el `trap`; el token no se pega en la evidencia (se pega `T=<redactado>`). Si `kubectl auth can-i` responde distinto que el token real en las dos preguntas sobre `namespaces/<nombre>` (kubectl avisa que `namespaces` no es de namespace y puede ignorar `-n`), se pegan las dos respuestas con el aviso literal y manda la prueba con el token; no se cambia el RBAC para que `can-i` cuadre.

- [ ] **CA-7**: U08-T01 sobre un `kind` nuevo. `make cluster-delete` solo borra lo de SynKra, y `make install` lo rehace desde cero
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml
  m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
  docker inspect -f '{{.Name}} {{.State.Running}}' gitops2-control-plane m08-gitops1-control-plane
  make cluster-delete; echo "borrar=$?"
  kind get kubeconfig --name synkra >/dev/null 2>&1; echo "existe=$?"
  docker inspect kind-registry >/dev/null 2>&1; echo "registro=$?"
  time make install; echo "install=$?"
  kubectl --kubeconfig "$KC" get nodes --no-headers | awk '{print $1, $2}'
  kubectl --kubeconfig "$KC" get ns synkra synkra-co synkra-pe --no-headers | awk '{print $1, $2}'
  curl -sf http://127.0.0.1:5001/v2/; echo " v2=$?"
  docker inspect -f '{{.Name}} {{.State.Running}}' gitops2-control-plane m08-gitops1-control-plane
  m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
  ```
  Esperado: las dos líneas de `docker inspect` (antes y después) son iguales, `/gitops2-control-plane true` y `/m08-gitops1-control-plane true` (solo se lee su estado, por nombre); `borrar=0`, `existe=1`, `registro=1`; `install=0` con el tiempo real anotado; `synkra-control-plane Ready` y `synkra-worker Ready`; los tres namespaces `Active`; `{} v2=0`; `kubeconfig-global-intacto` (ni `kind create` ni `kind delete` escribieron en `~/.kube/config`; el `stat` no abre el archivo). Al terminar, **el clúster queda arriba** (decisión 12) y CA-6 se vuelve a correr sobre él (se pega).

- [ ] **CA-8**: rojo primero. Cada regla nueva muerde, demostrado en copias
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   guardia-nombre:    la comparación con kind-synkra del paso 3 de guardia-contexto.sh se cambia para aceptar cualquier contexto
  #   guardia-servidor:  se quita el paso 4 (comparación de servidores)
  #   guardia-global:    se quita el paso 1 (rechazo de ~/.kube/config)
  #   receta-sin-guardia: aplicar-base pierde su dependencia de guardia
  #   nombre-variable:   vuelve CLUSTER ?= synkra y cluster-delete usa --name $(CLUSTER)
  #   rbac-delete:       el Role de synkra-co gana el verbo delete en deployments
  #   rbac-clusterrole:  rbac.yaml gana un ClusterRole y un ClusterRoleBinding para deployer
  #   ns-etiqueta:       synkra-pe pierde synkra.io/pais
  sed -i '<mutación>' "$D/<ruta>"
  W=$PWD; (cd "$D" && KC="$W/.kube/kind-synkra.yaml" bash "$W/bitacoras/U08-A-evidencia/ca4.sh")   # mutaciones de guardia y de recetas
  (cd "$D" && make -n cluster-delete CLUSTER=gitops2 | grep -c gitops2)               # nombre-variable
  (cd "$D" && kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py)   # mutaciones de deploy/base
  rm -rf "$D"
  ```
  Esperado: con cada una de las ocho mutaciones (una copia por mutación), su comprobación cae: `ca4.sh` imprime un `0` en la línea del caso afectado (o `salida=0` donde se esperaba otro código), `make -n` da `1` o más, o `verificar-base.py` imprime su `FALLA <regla>` y sale con 1. La bitácora pega la mutación, la línea que cayó y, para `ca4.sh`, comprueba antes y después que `nada-aplicado` sigue saliendo (ninguna mutación llegó a aplicar en `kind-synkra`; si alguna lo hizo, `make aplicar-base` desde el worktree lo restaura y se anota). Además, la bitácora pega el rojo de antes de implementar: `ca4.sh` y `verificar-base.py` (ya escritos, en una copia de `8306ee7` con solo ellos añadidos) fallan (`make: *** No rule to make target 'guardia'` y `FALLA entrada`). Sin `git stash` ni cambios en el worktree.

- [ ] **CA-9**: los scripts fallan con mensaje claro y el `trap` limpia lo que creó (sin borrar el clúster)
  Guardar como `bitacoras/U08-A-evidencia/ca9.sh` y correrlo con `kind-synkra` arriba:
  ```bash
  W=$PWD; KC=$W/.kube/kind-synkra.yaml
  bash -n scripts/guardia-contexto.sh scripts/registro-local.sh; echo "sintaxis=$?"
  grep -n 'set -euo pipefail' scripts/guardia-contexto.sh scripts/registro-local.sh
  grep -n -e 'trap' -e 'docker run' scripts/registro-local.sh
  [ "$(docker inspect -f '{{index .Config.Labels "io.synkra.registro"}}' kind-registry)" = true ] && docker rm -f kind-registry
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  sed -i 's|docker network connect kind kind-registry|docker network connect red-que-no-existe kind-registry|' "$D/scripts/registro-local.sh"
  (cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/registro-local.sh); echo "salida=$?"
  docker inspect kind-registry >/dev/null 2>&1; echo "quedo=$?"
  rm -rf "$D"
  make registro; echo "restaurado=$?"
  ```
  Esperado: `sintaxis=0`; las dos líneas de `set -euo pipefail`; el `trap` (y el registro del contenedor en él) aparece en una línea anterior al `docker run`; con la mutación, `salida=1` y una línea de error que nombra `docker network connect` y su código; `quedo=1` (el `trap` borró el contenedor que esa corrida creó); `restaurado=0`, y después CA-2 y CA-3 vuelven a pasar (se pegan). Ninguna orden de este criterio borra ni recrea `kind-synkra`.

- [ ] **CA-10**: el diff queda acotado
  Guardar como `bitacoras/U08-A-evidencia/ca10.sh`; `ca10.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat 8306ee7...HEAD
  git diff --name-only 8306ee7...HEAD | grep -v -E '^(Makefile|\.gitignore|deploy/kind/cluster\.yaml|deploy/base/kustomization\.yaml|deploy/base/namespaces\.yaml|deploy/base/rbac\.yaml|scripts/guardia-contexto\.sh|scripts/registro-local\.sh|scripts/verificar-base\.py|bitacoras/U08-A\.md|bitacoras/U08-A-evidencia/.*|revisiones/U08-A/.*)$'; echo "fuera=$?"
  git diff 8306ee7...HEAD -- .gitignore | grep -c '^-[^-]'
  git ls-files .kube .tls | wc -l
  ```
  Esperado: `fuera=1` (solo esas rutas); `0` (en `.gitignore` solo se añaden líneas); `0` (ni el kubeconfig ni certificados entran en git). El sha impreso es el del padre del commit de `ca10.txt`.

---

**Rojo primero:** `bitacoras/U08-A-evidencia/ca4.sh` y `scripts/verificar-base.py` se escriben y se commitean **antes** que la guardia, el `Makefile` nuevo y `deploy/base` (ese commit no trae la implementación; la bitácora pega su sha y su `git show --stat`), y se pega su fallo en una copia de ese commit. Después, CA-8 demuestra que cada regla muerde con el código terminado. Al cerrar, `kind-synkra` y `kind-registry` quedan arriba para U08-C (decisión 12).
