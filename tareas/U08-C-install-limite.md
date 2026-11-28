# U08-C: `make install` completo (Ingress NGINX, TLS, imágenes, backend) y `make verificar-limite`

**Unidad:** U08: `Instalación y límite`
**Tareas del plan que agrupa:** U08-T08 y U08-T09
**Historias que implementa:** H-26 (T08), H-27 (T09); contribuye a H-13 (T09 cierra la primera capa del límite) y a H-28 (`make imagenes` verifica las imágenes antes de subirlas)
**Depende de:** U08-A y U08-B (VERDES). Sale de la **unión** de sus dos puntas: la rama `m6r/U08-C` nace de `<punta VERDE de U08-A>` y su **primer commit** es `git merge --no-ff <punta VERDE de U08-B>` (decisión 1). Es la primera tarea que junta `deploy/base/backend` con `deploy/base` y la primera que aplica el backend en `kind-synkra`.

---

## Alcance

**Dentro** (una línea, concreta):

> La unión de U08-A y U08-B; `deploy/base` con `- backend` y el overlay `kind` sobre `../../base`; Ingress NGINX `controller-v1.15.1` copiado al repositorio (`deploy/ingress-nginx/`, con un parche que lo fija al nodo `ingress-ready`); el secreto TLS `synkra/synkra-local-tls`; `make imagenes` (construir, verificar y subir a `localhost:5001`); `make desplegar` (validación en el servidor, `apply -k deploy/overlays/kind`, `rollout status` y comprobación de que el pod corre el digest recién subido); `make verificar-limite` (seis preguntas `kubectl auth can-i --as=system:serviceaccount:synkra:deployer`); y `make install` con el orden del diseño, probado en un `kind-synkra` recién creado.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier clúster que no sea `kind-synkra` (AUTONOMIA-01). En las máquinas del equipo hay otros clústeres `kind` de otros cursos y sus contenedores: **ninguna orden los lista, los nombra, los toca, los para ni los borra**. Prohibidos `kind get clusters`, `kind delete clusters`, `docker ps` o `docker ps -a` sin un filtro de nombre propio de SynKra, `kubectl config get-contexts` sobre `~/.kube/config` y todo `kubectl` sin `--kubeconfig` (o sin `KUBECONFIG` apuntando al archivo del proyecto). Nada lee ni escribe `~/.kube/config` (la única excepción es el `stat` de CA-4, que no lo abre).
- `sudo`, `/etc/hosts`, `kubectl port-forward` y publicar puertos fuera de `127.0.0.1`. Los nombres se resuelven con `curl --resolve synkra.local:443:127.0.0.1`.
- El script de punta a punta, los planes y despliegues de país, el formato de error y el resumen (U08-D y U08-E). Esta tarea **no crea ningún Deployment en `synkra-co` ni en `synkra-pe`**: solo comprueba que el backend arranca y lee los países.
- Cambios en el código Java, en `application.properties`, en los Dockerfile, en `init-descarga/`, en `core/`, en `plugins/`, en los scripts de U08-A (`guardia-contexto.sh`, `registro-local.sh`) y de U08-B (`verificar-imagenes.sh`, `certificado-tls.sh`), y en `deploy/base/namespaces.yaml`, `deploy/base/rbac.yaml`, `deploy/base/backend/` y `deploy/kind/cluster.yaml`. Si algo de eso está roto, se para y se reporta.
- `aidlc-docs/`, `CANDIDATAS.md`, `unidades-y-tareas.md`, el plan de U08 y `loop/`.
- El front (U09): ni `synkra-front` ni la ruta `/` del Ingress.
- `PodSecurity`, `NetworkPolicy`, `ResourceQuota`, `LimitRange`, `cert-manager`, Helm y la Gateway API: no están en el diseño de U08.
- Cualquier descarga que no esté en la decisión 4.

---

## Archivos de contexto

- `loop/CONTINUAR.md` («Reglas del entorno» y «Lecciones de esta sesión para los despachos»)
- `aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md` (U08-T08 y U08-T09) y `unidades-y-tareas.md` (sección U08)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/deployment-architecture.md` («Orden de `make install`») e `infrastructure-design.md` (filas «Red», «Límite (P2)», «Cómo se instala» y «Lo que la instalación crea y lo que no»)
- `tareas/U08-A-cluster-guardia-rbac.md` y `tareas/U08-B-backend-ingress-imagenes.md` (decisiones y reparto; la sección «Pendiente para U08-C» de `bitacoras/U08-B.md` es la lista de esta tarea)
- De la unión: `Makefile`, `scripts/guardia-contexto.sh`, `scripts/registro-local.sh`, `scripts/verificar-base.py`, `deploy/base/`, `deploy/overlays/kind/`, `scripts/imagenes.sh`, `scripts/verificar-imagenes.sh`, `scripts/verificar-backend.py`, `scripts/certificado-tls.sh`
- `revisiones/U08-B/ronda-1.md` y `ronda-2.md`: F-01 (`docker run` sin `--pull=never` salía a la red), F-03 y F-06 (versiones repetidas como literales, y un chequeo que no podía fallar). **No repetirlos aquí**: ninguna etiqueta de versión se escribe a mano en el `Makefile` ni en los scripts nuevos; todas salen del mismo origen que usa `imagenes.sh`, y todo chequeo nuevo tiene que poder fallar (CA-8 lo demuestra).
- `CANDIDATAS.md`: C-75 (server-side apply contra un servidor real: aquí solo arranca el backend; lo cierra U08-D), C-129 (`/tmp` con `exec`: el `emptyDir` del pod real lo prueba), C-130 (`fs.inotify.max_user_instances`), C-132 (el `Id` de una imagen no se compara entre corridas)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (`IMAGEN_INIT_POR_DEFECTO`) y `backend/build.gradle.kts`, `core/build.gradle.kts` (origen de las versiones)
- Plantillas de estilo: `tareas/U08-A-cluster-guardia-rbac.md` y `tareas/U07-G-fronteras-resumen.md`

---

## Decisiones del orquestador

1. **La unión, sin reescribir historia.** Desde el clon del proyecto: `git worktree add ../wt-m6r-U08-C -b m6r/U08-C <punta VERDE de U08-A>`; dentro, `git merge --no-ff <punta VERDE de U08-B> -m "U08-C: une U08-B (<punta VERDE de U08-B>) sobre U08-A (<punta VERDE de U08-A>)" </dev/null`. Hoy las dos ramas no comparten ninguna ruta, así que no se espera conflicto. **Si hay conflicto**: `git merge --abort`, no se resuelve nada a mano y se para y se reporta. Prohibidos `rebase`, `cherry-pick` de las puntas, `push --force` y `git stash`. El sha del merge es la **base de esta tarea** (`M`): el CA de diff acotado lo usa, y la bitácora lo pega con `git log -1 --format='%H %P' M`.
2. **Reparto de archivos.** Esta tarea escribe solo:
   - `Makefile` (amplía el de U08-A; decisión 7);
   - `deploy/base/kustomization.yaml` (gana `- backend`) y `deploy/overlays/kind/kustomization.yaml` (`../../base/backend` pasa a `../../base`; se quita el comentario «U08-C lo cambia»);
   - `deploy/overlays/kind/backend-kind.yaml` (gana `imagePullPolicy: Always`, decisión 9);
   - `deploy/ingress-nginx/deploy.yaml`, `deploy/ingress-nginx/kustomization.yaml`, `deploy/ingress-nginx/controlador-kind.yaml` (decisión 3);
   - `scripts/imagenes.sh` (gana `--subir`, decisión 8), `scripts/verificar-base.py` y `scripts/verificar-backend.py` (sus listas exactas crecen con la unión, decisión 6);
   - nuevos: `scripts/secreto-tls.sh`, `scripts/desplegar.sh`, `scripts/verificar-limite.sh`;
   - `bitacoras/U08-C.md`, `bitacoras/U08-C-evidencia/`, `revisiones/U08-C/`.
3. **Ingress NGINX: cuál, de dónde y cómo entra al clúster.**
   - Versión: **`controller-v1.15.1`** (publicada el 2026-03-19; es la última del proyecto `kubernetes/ingress-nginx`, que quedó retirado después). Se anota en la bitácora que no habrá parches nuevos; cambiar de controlador es candidata, no de esta tarea.
   - Manifiesto: el de `kind` de esa etiqueta, desde la URL fija `https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml`, **descargado una sola vez** por el codificador con `curl -fsSL -o deploy/ingress-nginx/deploy.yaml <URL>` y comprobado con `sha256sum`: tiene que dar **`2a3ae008c8786431115502644e77ab398fdebfb721a5d1195ed3089cde3299df`** (679 líneas). Si no da eso, **no se usa**: se para y se reporta. El archivo se versiona **byte por byte igual** (sin comentarios añadidos: el origen y el sha van en `deploy/ingress-nginx/kustomization.yaml` y en la bitácora). Ninguna receta ni script lo vuelve a descargar.
   - Imágenes: el manifiesto ya las fija por digest del índice multi-arquitectura: `registry.k8s.io/ingress-nginx/controller:v1.15.1@sha256:594ceea76b01c592858f803f9ff4d2cb40542cae2060410b2c95f75907d659e1` y `registry.k8s.io/ingress-nginx/kube-webhook-certgen:v1.6.9@sha256:01038e7de14b78d702d2849c3aad72fd25903c4765af63cf16aa3398f5d5f2dd`. Las saca **containerd de los nodos de `kind`** desde `registry.k8s.io` al aplicar el manifiesto (no se hace `docker pull` ni `kind load`: `kind load` con referencias por digest no deja la imagen con el digest que pide el pod). El codificador pega en la bitácora `docker buildx imagetools inspect <ref>` de las dos (lee el índice, no descarga capas) para mostrar que traen `linux/amd64` y `linux/arm64`.
   - Cómo se carga: `deploy/ingress-nginx/kustomization.yaml` = `resources: [deploy.yaml]` y `patches: [{path: controlador-kind.yaml}]`. El parche (estratégico, sobre el Deployment `ingress-nginx/ingress-nginx-controller`) añade `nodeSelector: {ingress-ready: "true"}`. Motivo: el manifiesto de la v1.15.1 solo pide `kubernetes.io/os: linux` y publica `hostPort` 80 y 443; sin el parche el controlador puede caer en `synkra-worker`, cuyo 80/443 no está publicado en el host (el `extraPortMappings` de U08-A es del control plane, que ya tiene la etiqueta y el manifiesto ya tolera su `taint`). Se aplica con `kubectl apply -k deploy/ingress-nginx` y se espera con `rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=300s`.
   - Riesgo declarado: el nodo de U08-A es Kubernetes v1.36.1 y la v1.15.1 no lo nombra entre las versiones probadas. Si el controlador no queda `Ready` o el Ingress no enruta, se para y se reporta con `kubectl describe` y los eventos (no se cambia de versión por cuenta propia).
4. **Descargas permitidas** (todas se anotan en la bitácora; ninguna otra):
   - el manifiesto de la decisión 3, una vez;
   - las dos imágenes de Ingress NGINX por digest, que sacan los nodos en cada clúster nuevo;
   - `docker buildx imagetools inspect` de esas dos referencias (solo el índice);
   - si la caché de Docker no tiene alguna capa de los builds de U08-B: las bases por digest de U08-B (`alpine:3.20@sha256:d9e853e8…`, `eclipse-temurin:21-jre-alpine@sha256:5d7689f4…`) y los paquetes Alpine `shellcheck` y `curl` (las mismas que U08-B ya declaró);
   - las dependencias Maven que `./gradlew` ya tiene declaradas, solo si su caché no las tiene.
   El nodo `kindest/node@sha256:3489c767…` y el registro `registry@sha256:1be55279…` tienen que estar en la caché local (en las dos máquinas lo están); si `kind create cluster` o `docker run` intentan descargarlos, se para.
5. **Entorno y portabilidad.** Las tareas se corren en la máquina de David (`x86_64`, Docker `amd64`) o en la de Julian (`aarch64`, Docker `arm64`): **ningún criterio, script ni salida esperada lleva `amd64`, `arm64`, `x86_64` ni `aarch64` escritos**. Donde haga falta la arquitectura se compara con `docker version -f '{{.Server.Arch}}'`. La cabecera de cada `caN.txt` trae fecha, sha, `uname -m`, `docker version -f '{{.Server.Arch}}'` y `JAVA_HOME`.
   - `JAVA_HOME`: si el entorno ya lo trae y `"$JAVA_HOME/bin/java" -version` dice `21`, se usa; si no, el único directorio que case con `~/.gradle/jdks/eclipse_adoptium-21-*-linux*`; si no hay ninguno o hay más de uno, se para. El `Makefile` no fija `JAVA_HOME`.
   - Antes del primer `make install` se pega, sin cambiar nada: `cat /proc/sys/fs/inotify/max_user_instances` (C-130: si es menor que 512, se para y se le pide a David o a Julian que lo suba; el codificador no usa `sudo`), y `ss -ltnH 'sport = :80 or sport = :443 or sport = :5001'` antes de crear el clúster: si algo que no es de SynKra ocupa uno de esos puertos en el host, se para (no se busca de quién es ni se para nada ajeno).
6. **Los verificadores de la unión.** Con la unión, `deploy/base` pasa de 8 a 11 objetos y el overlay de 4 a 12; las listas exactas de U08-A y U08-B tienen que crecer, sin aflojar ninguna regla:
   - `scripts/verificar-base.py`: la lista exacta es la de U08-A más `('PersistentVolumeClaim', 'synkra', 'synkra-datos')`, `('Deployment', 'synkra', 'synkra-backend')` y `('Service', 'synkra', 'synkra-backend')` (solo `kind`, `namespace` y `name`: los campos del backend los mira `verificar-backend.py`). Las reglas de namespaces y RBAC no cambian, y las prohibiciones globales (`Cluster*`, `*`, `delete`, `secrets`) se aplican a los once. Imprime `OK base: 11 objetos`.
   - `scripts/verificar-backend.py` sobre el overlay completo: sus cuatro objetos con todas sus reglas (más `imagePullPolicy: Always`, decisión 9), y **el resto** tiene que ser exactamente los ocho de U08-A (por `kind`, `namespace` y `name`); las prohibiciones globales (`Cluster*`, `hostPath`, `hostNetwork`, `privileged`, `:latest`) se aplican a los doce; «ningún namespace distinto de `synkra`» sigue valiendo para sus cuatro objetos. Imprime `OK backend: 4 objetos (más 8 de la base)`. `--base` (sobre `deploy/base/backend`, 3 objetos, `IfNotPresent`) y `--versiones` no cambian de comportamiento.
7. **`Makefile`.** Se conserva todo lo de U08-A (variables, `guardia`, `cluster`, `registro`, `status`, `cluster-delete`, `help`, `setup`, `doctor`). Objetivos nuevos o cambiados, todos con su `## ayuda` y en `.PHONY`; los que hablan con el clúster dependen de `guardia`:
   - `aplicar-base`: **cambia** a `kubectl --kubeconfig "$(KUBECONFIG_SYNKRA)" apply -f deploy/base/namespaces.yaml -f deploy/base/rbac.yaml` (los mismos ocho objetos de U08-A). Motivo: con `- backend` en la base, `apply -k deploy/base` crearía el Deployment con la imagen `synkra-backend` sin registro, que nunca arranca. Se anota en la bitácora como desviación de U08-A.
   - `ingress: guardia`: `kubectl apply -k deploy/ingress-nginx` y el `rollout status` de la decisión 3.
   - `tls: guardia`: `scripts/secreto-tls.sh` (decisión 10).
   - `imagenes`: `scripts/imagenes.sh --subir` (decisión 8). No pasa por la guardia: no habla con el clúster, solo con el Docker local y el registro de `127.0.0.1:5001`.
   - `desplegar: guardia`: `scripts/desplegar.sh` (decisión 9).
   - `verificar-limite: guardia`: `scripts/verificar-limite.sh` (decisión 11).
   - `install`, en este orden y ningún otro: `@scripts/guardia-contexto.sh --antes-de-crear`, `$(MAKE) cluster`, `$(MAKE) registro`, `$(MAKE) aplicar-base`, `$(MAKE) ingress`, `$(MAKE) tls`, `$(MAKE) imagenes`, `$(MAKE) desplegar`, `$(MAKE) verificar-limite`. Es el orden del diseño (clúster y registro; Ingress y certificado; imágenes; `apply -k`; espera; límite), con `aplicar-base` antes de `tls` porque el secreto vive en `synkra`.
   - Ninguna etiqueta de versión (`0.1.0`, `2.1.0`, `1.0.0`) aparece en el `Makefile` ni en los scripts nuevos (CA-2 lo cuenta).
8. **`scripts/imagenes.sh --subir`** (sin la opción, se comporta igual que en U08-B). Con `--subir`: **antes de construir** comprueba que el registro del prefijo responde (`curl -sf --max-time 5 http://<prefijo con localhost cambiado por 127.0.0.1>/v2/`; si no, `FALLA subir: el registro <prefijo> no responde: corre make registro` y 1, sin ningún `docker build`); construye como siempre; corre `scripts/verificar-imagenes.sh` (si falla, no sube nada); hace `docker push` de las tres etiquetas que construyó (las mismas variables, sin repetirlas); y para cada una lee de vuelta el registro: la etiqueta en `GET /v2/<repo>/tags/list` y el digest en la cabecera `Docker-Content-Digest` de `HEAD /v2/<repo>/manifests/<etiqueta>` con `Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json`. Imprime `subida <imagen> <digest>` por cada una y `OK imagenes subidas`. Cada fallo de `docker push` o `curl`: `FALLA <imagen>: <orden> salió con <código>` y 1.
9. **`scripts/desplegar.sh`** (`bash`, `set -euo pipefail`; todo `kubectl` con `--kubeconfig "$KUBECONFIG_SYNKRA"`):
   1. Espera (≤ 60 s, con `timeout`) a que el Service `ingress-nginx-controller-admission` tenga un endpoint listo (`kubectl get endpointslices -n ingress-nginx -l kubernetes.io/service-name=ingress-nginx-controller-admission`). Sin endpoint: `FALLA desplegar: el webhook de Ingress NGINX no tiene endpoint`. No hay reintentos de `apply` ocultos.
   2. `kubectl apply --dry-run=server -k deploy/overlays/kind` (sustituye a `kubeconform -strict`, que no está) y después `kubectl apply -k deploy/overlays/kind`.
   3. `kubectl -n synkra rollout status deployment/synkra-backend --timeout=300s` (la `startupProbe` da hasta 3 min).
   4. **Lo que corre es lo que se subió.** La imagen y su etiqueta salen de `kubectl kustomize deploy/overlays/kind` (no se escriben). Se compara el digest de `.status.containerStatuses[?(@.name=="synkra-backend")].imageID` del pod con el `Docker-Content-Digest` del registro para esa etiqueta (mismo `Accept` de la decisión 8). Si difieren (la etiqueta se volvió a subir con otro contenido), `kubectl -n synkra rollout restart deployment/synkra-backend`, otro `rollout status` y otra comparación; si siguen distintos, `FALLA desplegar: corre <digest> y el registro tiene <digest>` y 1. Imprime `OK desplegar: <imagen> <digest>`.
   - Por eso el overlay pide **`imagePullPolicy: Always`** (en la base sigue `IfNotPresent`): con la misma etiqueta y `IfNotPresent`, un nodo que ya tiene la imagen nunca traería la nueva. Desviación de la decisión 4 de U08-B, solo en el overlay de `kind`, que se anota.
   - La comparación es de digest de registro contra digest de pod dentro de la misma corrida; nunca de `Id` de imagen ni entre corridas (C-132). Si el pod reporta un digest de otro nivel (manifiesto de plataforma en vez de índice) y por eso nunca cuadran, se para y se reporta con las dos salidas.
10. **`scripts/secreto-tls.sh`** (`bash`, `set -euo pipefail`, con `trap` que borra su `mktemp -d`, registrado antes de crearlo): `.tls/` se crea con `0700` si falta (y se le aplica `chmod 0700`). Si existen `.tls/tls.crt` **y** `.tls/tls.key`, se reutilizan (idempotente: el certificado no cambia entre instalaciones); si existe solo uno, `FALLA tls: .tls tiene solo <archivo>; no se regenera ni se borra` y 1; si no existe ninguno, `scripts/certificado-tls.sh .tls`. Después: `kubectl create secret tls synkra-local-tls -n synkra --cert .tls/tls.crt --key .tls/tls.key --dry-run=client -o yaml` a un archivo del temporal (`0600`: lleva la clave) y `kubectl apply -f` de ese archivo (sin tuberías). Lectura de vuelta: el `type` es `kubernetes.io/tls` y el `sha256` de `.data.tls\.crt` decodificado es el de `.tls/tls.crt`; si no, `FALLA tls: …`. Imprime `OK tls: synkra/synkra-local-tls <sha256 del certificado>`. La clave nunca se imprime ni se pega en la evidencia.
11. **`scripts/verificar-limite.sh`** (U08-T09; `bash`, `set -euo pipefail`; solo lee, no crea nada). Seis preguntas, siempre con `--as=system:serviceaccount:synkra:deployer` y `--kubeconfig "$KUBECONFIG_SYNKRA"`, en este orden:
    | # | Pregunta | Esperado |
    |---|---|---|
    | 1 | `create deployments.apps -n synkra-co` | `yes` |
    | 2 | `create deployments.apps -n synkra-pe` | `yes` |
    | 3 | `create deployments.apps -n default` | `no` |
    | 4 | `create deployments.apps -n kube-system` | `no` |
    | 5 | `create deployments.apps -n synkra` | `no` |
    | 6 | `create namespaces` | `no` |
    - Una línea por pregunta: `ok <pregunta>: <respuesta>` o `FALLA <pregunta>: <respuesta> (esperado <esperado>)`. Al final `OK limite: 6 respuestas esperadas` y 0, o `FALLA limite: <n> respuestas no esperadas` y 1.
    - `kubectl auth can-i` sale con 0 para `yes` y con 1 para `no`: se lee **la salida**, no el código. Un código que no sea 0 ni 1, o una salida que no sea `yes` ni `no`, es `FALLA <pregunta>: kubectl auth can-i salió con <código>` (nunca se cuenta como `no`).
    - La lista de preguntas y respuestas esperadas vive solo en este script; el `Makefile` no la repite.
12. **Qué queda arriba al terminar.** `kind-synkra` y `kind-registry` con SynKra instalado (Ingress NGINX, secreto, backend `Running`), `.kube/` y `.tls/` en el worktree de esta tarea. U08-D sale de aquí y vuelve a crear el clúster desde su propio worktree **después** de que el revisor de U08-C termine (nadie borra el clúster mientras otro lo usa). Se limpian: las copias de `mktemp -d`, los kubeconfig falsos, el `ClusterRoleBinding` y el `RoleBinding` de prueba de CA-7 (en el `trap`), y la imagen de variante de CA-5 (en Docker local; en el registro queda la capa, que no se puede borrar, y se anota). Para borrar todo a mano: `make cluster-delete`.
13. **Bitácora en `bitacoras/U08-C.md`**, evidencia en `bitacoras/U08-C-evidencia/` (un `caN.sh` versionado y su `caN.txt` con la cabecera de la decisión 5, el comando y la salida completa), y el revisor en `revisiones/U08-C/`. Toda la evidencia sale del **último commit que toca código**; si después cambia código, se vuelve a correr. Las mutaciones se hacen en copias (`git archive HEAD | tar -x -C "$(mktemp -d)"` y `git init -q` dentro), nunca con `git stash` ni en el worktree. En los `grep` de patrones con `${...}` se usa `grep -F` con comillas simples.

---

## Criterios de aceptación

En todos, desde la raíz del worktree: `KC=$PWD/.kube/kind-synkra.yaml`; todo `kubectl` lleva `--kubeconfig "$KC"` (o uno falso en `mktemp -d`), nunca el global. `R=127.0.0.1:5001` es el registro. `CURL='curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt'` (nunca `-k`).

- [ ] **CA-1**: la unión es un merge real de las dos puntas VERDES
  ```bash
  M=$(git rev-list --merges -1 HEAD); git log -1 --format='%H %P' "$M"
  git rev-list --merges "<punta VERDE de U08-A>"..HEAD | wc -l
  git merge-base --is-ancestor "<punta VERDE de U08-A>" HEAD; echo "a=$?"
  git merge-base --is-ancestor "<punta VERDE de U08-B>" HEAD; echo "b=$?"
  [ "$(git diff --name-only "<punta VERDE de U08-A>" "$M" | sort)" = "$(git diff --name-only 8306ee7 "<punta VERDE de U08-B>" | sort)" ] && echo "merge-limpio"
  git log --format='%H %P' "<punta VERDE de U08-A>"..HEAD | awk 'NF>2' | wc -l
  ```
  Esperado: los padres de `M` son, en este orden, `<punta VERDE de U08-A>` y `<punta VERDE de U08-B>`; `1`; `a=0`; `b=0`; `merge-limpio` (el merge trae exactamente lo de U08-B, sin arreglos a mano); `1` (un solo merge en la rama). La bitácora pega `git log --oneline --graph` desde `<punta VERDE de U08-A>`.

- [ ] **CA-2**: los manifiestos de la unión y el Ingress NGINX fijado (sin clúster)
  Guardar como `bitacoras/U08-C-evidencia/ca2.sh`:
  ```bash
  kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "base=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "overlay=$?"
  kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base; echo "solo-backend=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in yaml.safe_load_all(sys.stdin) if o))"
  [ "$(sha256sum deploy/ingress-nginx/deploy.yaml | cut -d' ' -f1)" = 2a3ae008c8786431115502644e77ab398fdebfb721a5d1195ed3089cde3299df ] && echo "manifiesto-ok"
  I=$(kubectl kustomize deploy/ingress-nginx | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; d=[o for o in os_ if o['kind']=='Deployment'][0]; print(d['metadata']['namespace'], d['metadata']['name'], sorted(d['spec']['template']['spec']['nodeSelector'].items())); imgs=sorted({c['image'] for o in os_ if o['kind'] in ('Deployment','Job') for c in o['spec']['template']['spec']['containers']}); print(len(imgs), all('@sha256:' in i for i in imgs))")
  printf '%s\n' "$I"; [ "$I" = "$(printf '%s\n%s' "ingress-nginx ingress-nginx-controller [('ingress-ready', 'true'), ('kubernetes.io/os', 'linux')]" '2 True')" ] && echo "ingress-ok"
  grep -nE '0\.1\.0|2\.1\.0|1\.0\.0' Makefile scripts/desplegar.sh scripts/secreto-tls.sh scripts/verificar-limite.sh; echo "literales=$?"
  grep -cE 'amd64|arm64|x86_64|aarch64' Makefile scripts/*.sh deploy/ingress-nginx/kustomization.yaml deploy/ingress-nginx/controlador-kind.yaml | grep -v ':0$'; echo "arquitectura=$?"
  ```
  Esperado: `OK base: 11 objetos` y `base=0`; `OK backend: 4 objetos (más 8 de la base)` y `overlay=0`; `OK backend: 3 objetos` y `solo-backend=0`; la lista de doce (los ocho de U08-A, más `Deployment`, `Ingress`, `PersistentVolumeClaim` y `Service` del backend en `synkra`); `manifiesto-ok`; las dos líneas de `I` e `ingress-ok`; `literales=1`; `arquitectura=1`.

- [ ] **CA-3**: U08-T08. La guardia frena cada objetivo nuevo con un kubeconfig ajeno, sin aplicar nada
  Guardar como `bitacoras/U08-C-evidencia/ca3.sh` y correrlo con SynKra instalado (después de CA-4):
  ```bash
  KC=${KC:-$PWD/.kube/kind-synkra.yaml}
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  falso() { printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "https://127.0.0.1:1"}\ncontexts:\n- name: %s\n  context: {cluster: c, user: u}\ncurrent-context: %s\nusers:\n- name: u\n  user: {token: falso}\n' "$1" "$1"; }
  falso otro > "$D/otro.yaml"
  foto() { for ns in synkra synkra-co synkra-pe ingress-nginx; do kubectl --kubeconfig "$KC" get all,cm,secret,ingress,pvc,role,rolebinding -n "$ns" -o jsonpath='{range .items[*]}{.kind}/{.metadata.name}={.metadata.resourceVersion} {end}'; done; kubectl --kubeconfig "$KC" get clusterrolebindings -o jsonpath='{range .items[*]}{.metadata.name}={.metadata.resourceVersion} {end}'; }
  antes=$(foto)
  for obj in aplicar-base ingress tls desplegar verificar-limite install; do
    KUBECONFIG_SYNKRA="$D/otro.yaml" make "$obj" >"$D/out" 2>&1; echo "$obj salida=$?"; grep -c '^contexto no permitido: otro (solo kind-synkra)' "$D/out"
  done
  sed -n '/^install:/,/^$/p' Makefile
  despues=$(foto); [ "$antes" = "$despues" ] && echo "nada-aplicado"
  ```
  Esperado: para los seis objetivos, `salida=` distinto de 0 y `1`; el bloque de `install` con las nueve líneas de la decisión 7 en ese orden (la guardia `--antes-de-crear` primero y `verificar-limite` al final); `nada-aplicado`.

- [ ] **CA-4**: U08-T08. `make install` sobre un `kind-synkra` recién creado deja SynKra corriendo, y cada paso se lee de vuelta
  Guardar como `bitacoras/U08-C-evidencia/ca4.sh` y correrlo con `JAVA_HOME` exportado (decisión 5):
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml; R=127.0.0.1:5001
  m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
  make cluster-delete; echo "borrar=$?"
  time make install; echo "install=$?"
  kubectl --kubeconfig "$KC" get nodes --no-headers | awk '{print $1, $2}'
  [ "$(kubectl --kubeconfig "$KC" get nodes -o jsonpath='{.items[0].status.nodeInfo.architecture}')" = "$(docker version -f '{{.Server.Arch}}')" ] && echo "misma-arquitectura"
  kubectl --kubeconfig "$KC" get ns --no-headers | awk '{print $1}' | grep -E '^synkra'
  kubectl --kubeconfig "$KC" -n ingress-nginx get pods -l app.kubernetes.io/component=controller -o jsonpath='{range .items[*]}{.spec.nodeName} {.status.phase} {.status.containerStatuses[0].ready}{"\n"}{end}'
  kubectl --kubeconfig "$KC" get ingressclass nginx -o jsonpath='{.spec.controller}'; echo
  kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.type}'; echo
  [ "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.data.tls\.crt}' | base64 -d | sha256sum)" = "$(sha256sum < .tls/tls.crt)" ] && echo "secreto-es-el-certificado"
  stat -c '%a %n' .tls .tls/tls.key; git check-ignore -v .tls/tls.key
  kubectl --kubeconfig "$KC" get pods -n synkra -o wide
  kubectl --kubeconfig "$KC" -n synkra get pvc synkra-datos -o jsonpath='{.status.phase} {.spec.storageClassName} {.status.capacity.storage}'; echo
  P=$(kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-backend -o jsonpath='{.items[0].metadata.name}')
  kubectl --kubeconfig "$KC" -n synkra get pod "$P" -o jsonpath='{.spec.serviceAccountName} {.spec.containers[0].image} {.spec.containers[0].imagePullPolicy} {.status.containerStatuses[0].imageID}'; echo
  kubectl --kubeconfig "$KC" -n synkra exec "$P" -- id -u
  kubectl --kubeconfig "$KC" -n synkra exec "$P" -- ls -ln /data
  for repo in synkra-backend synkra-core-ejemplo synkra-init-descarga; do curl -sf "http://$R/v2/$repo/tags/list"; echo; done
  curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt -o /dev/null -w '%{http_code} %{ssl_verify_result}\n' https://synkra.local/actuator/health
  curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/actuator/health | jq -c '{status, db: .components.db.status}'
  curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises | jq -c '[.[] | {pais, lectura}]'
  kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -ciE 'forbidden|unauthorized|read-only file system|permission denied|UnsatisfiedLinkError'
  m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
  ```
  Esperado: `borrar=0`; `install=0` con el tiempo real anotado (y la salida completa de `make install` en `ca4.txt`, que U08-E citará); `synkra-control-plane Ready` y `synkra-worker Ready`; `misma-arquitectura`; `synkra`, `synkra-co` y `synkra-pe`; el controlador en `synkra-control-plane`, `Running true`; `k8s.io/ingress-nginx`; `kubernetes.io/tls`; `secreto-es-el-certificado`; `700` y `600`, y `git check-ignore` nombra `/.tls/`; el backend `1/1 Running` (U08-T08); `Bound standard 1Gi`; `deployer localhost:5001/synkra-backend:<versión> Always localhost:5001/synkra-backend@sha256:…` con el mismo digest que imprimió `subida` y `OK desplegar` en el log de `make install`; `10001`; `synkra.db` (y lo que la base cree al arrancar) de dueño `10001`; las tres etiquetas en el registro; `200 0` (TLS verificado contra el certificado propio, sin `-k`); `{"status":"UP","db":"UP"}`; `co` y `pe` con su `lectura` (el backend leyó los dos países con el token de `deployer`: no hay Deployments todavía, y lo que diga `lectura` se pega tal cual); `0` en los logs (ni RBAC rechazado ni C-129); `kubeconfig-global-intacto`; y `make install` terminó con las seis líneas `ok` y `OK limite: 6 respuestas esperadas`. Ninguna orden nombra otro clúster.

- [ ] **CA-5**: U08-T08. `make install` es idempotente y lo que corre es siempre lo último que se subió
  Dos guiones, que se corren después de CA-4. Las dos funciones de lectura (`registro` y `corre`) son las mismas en los dos.
  `bitacoras/U08-C-evidencia/ca5.sh` (idempotencia):
  ```bash
  KC=${KUBECONFIG_SYNKRA:-$PWD/.kube/kind-synkra.yaml}; R=127.0.0.1:5001
  A='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
  IMG=$(kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print([o for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment'][0]['spec']['template']['spec']['containers'][0]['image'])")
  TAG=${IMG##*:}
  registro() { curl -sfI -H "Accept: $A" "http://$R/v2/synkra-backend/manifests/$TAG" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}'; }
  corre() { kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-backend -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' | sed 's/.*@//'; }
  c0=$(sha256sum < .tls/tls.crt)
  make install > /dev/null; echo "segunda=$?"
  [ "$c0" = "$(sha256sum < .tls/tls.crt)" ] && echo "certificado-igual"
  [ "$(registro)" = "$(corre)" ] && echo "corre-lo-subido"
  ```
  `bitacoras/U08-C-evidencia/ca5-variante.sh` (la misma etiqueta con otro contenido; `W` es el worktree, para restaurar desde él aunque se corra en una copia):
  ```bash
  W=${W:-$PWD}; KC=${KUBECONFIG_SYNKRA:-$W/.kube/kind-synkra.yaml}; R=127.0.0.1:5001
  # (A, IMG, TAG, registro y corre, iguales que en ca5.sh)
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  printf 'FROM %s\nLABEL io.synkra.prueba-u08c="%s"\n' "$IMG" "$(date +%s)" > "$D/Dockerfile"
  docker build -q -t "$IMG" "$D" && docker push -q "$IMG"; echo "variante=$?"
  v=$(registro); [ "$v" != "$(corre)" ] && echo "registro-cambio"
  KUBECONFIG_SYNKRA="$KC" make desplegar; echo "desplegar=$?"
  [ "$v" = "$(corre)" ] && echo "corre-la-variante"
  KUBECONFIG_SYNKRA="$KC" make -C "$W" imagenes desplegar > /dev/null; echo "restaurado=$?"
  [ "$(registro)" = "$(corre)" ] && echo "corre-lo-subido"
  ```
  Esperado: `segunda=0`; `certificado-igual`; `corre-lo-subido`; `variante=0` y `registro-cambio` (la misma etiqueta apunta ahora a otro digest); `desplegar=0`, y en su salida se ve el `rollout restart`; `corre-la-variante` (sin la decisión 9, el pod seguiría con el digest viejo: CA-8 lo demuestra); `restaurado=0` y `corre-lo-subido`. La variante solo añade una `LABEL` sobre la imagen buena: es el mismo backend, y la restauración la vuelve a construir desde el worktree. No se comparan `Id` de imágenes (C-132).

- [ ] **CA-6**: U08-T09. `make verificar-limite` da las seis respuestas esperadas, solo pregunta y no se deja engañar por un `kubectl` roto
  Guardar como `bitacoras/U08-C-evidencia/ca6.sh`:
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml
  foto() { kubectl --kubeconfig "$KC" get sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}'; kubectl --kubeconfig "$KC" get clusterrolebindings,clusterroles -o jsonpath='{range .items[*]}{.metadata.name}={.metadata.resourceVersion} {end}'; }
  antes=$(foto)
  make verificar-limite; echo "salida=$?"
  despues=$(foto); [ "$antes" = "$despues" ] && echo "solo-pregunta"
  grep -c -- '--as=system:serviceaccount:synkra:deployer' scripts/verificar-limite.sh
  grep -nE 'kubectl[^|]*(create|apply|delete|patch|label|annotate)' scripts/verificar-limite.sh; echo "escrituras=$?"
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  printf '#!/bin/sh\nexit 7\n' > "$D/kubectl"; chmod +x "$D/kubectl"
  (PATH="$D:$PATH" KUBECONFIG_SYNKRA="$KC" bash scripts/verificar-limite.sh); echo "roto=$?"
  ```
  Esperado: las seis líneas `ok …` en el orden de la decisión 11 (`yes`, `yes`, `no`, `no`, `no`, `no`), `OK limite: 6 respuestas esperadas` y `salida=0` (U08-T09); `solo-pregunta`; `1` o más (la cuenta usada es siempre `synkra:deployer`); `escrituras=1`; con el `kubectl` falso, seis líneas `FALLA <pregunta>: kubectl auth can-i salió con 7`, `FALLA limite: 6 respuestas no esperadas` y `roto=1` (un error nunca se cuenta como `no`).

- [ ] **CA-7**: U08-T09. Un `ClusterRoleBinding` de prueba (y un `RoleBinding` en `synkra`) hacen fallar el límite; al quitarlos, vuelve a pasar
  Guardar como `bitacoras/U08-C-evidencia/ca7.sh` (CA-8 lo corre también desde una copia):
  ```bash
  KC=${KC:-$PWD/.kube/kind-synkra.yaml}
  quitar() { kubectl --kubeconfig "$KC" delete clusterrolebinding synkra-prueba-limite --ignore-not-found; kubectl --kubeconfig "$KC" -n synkra delete rolebinding synkra-prueba-limite --ignore-not-found; }
  trap quitar EXIT                                                   # registrado antes de crear nada
  kubectl --kubeconfig "$KC" create clusterrolebinding synkra-prueba-limite --clusterrole=edit --serviceaccount=synkra:deployer
  kubectl --kubeconfig "$KC" get clusterrolebinding synkra-prueba-limite -o jsonpath='{.roleRef.name} {.subjects[0].namespace}/{.subjects[0].name}'; echo
  make verificar-limite; echo "con-crb=$?"
  kubectl --kubeconfig "$KC" delete clusterrolebinding synkra-prueba-limite
  kubectl --kubeconfig "$KC" get clusterrolebinding synkra-prueba-limite; echo "crb-borrado=$?"
  kubectl --kubeconfig "$KC" -n synkra create rolebinding synkra-prueba-limite --clusterrole=edit --serviceaccount=synkra:deployer
  make verificar-limite; echo "con-rb=$?"
  quitar
  kubectl --kubeconfig "$KC" -n synkra get rolebinding synkra-prueba-limite; echo "rb-borrado=$?"
  make verificar-limite; echo "limpio=$?"
  ```
  Esperado: `edit synkra/deployer`; con el `ClusterRoleBinding`, `FALLA` en las preguntas 3, 4 y 5 (`yes`, esperado `no`), `ok` en 1, 2 y 6, `FALLA limite: 3 respuestas no esperadas` y `con-crb=1` (U08-T09: «si se añade a mano un ClusterRoleBinding de prueba, termina con 1»); `NotFound` y `crb-borrado=1`; con el `RoleBinding` en `synkra`, solo la 5 cae y `con-rb=1`; `NotFound` y `rb-borrado=1`; `limpio=0`. Si el script se corta a la mitad, el `trap` borra los dos por nombre.

- [ ] **CA-8**: rojo primero, y cada regla nueva muerde (en copias)
  ```bash
  W=$PWD; KC=$W/.kube/kind-synkra.yaml
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   base-sin-backend:      deploy/base/kustomization.yaml pierde «- backend»
  #   overlay-solo-backend:  el overlay vuelve a ../../base/backend
  #   pull-ifnotpresent:     backend-kind.yaml pierde imagePullPolicy: Always
  #   ingress-sin-nodo:      deploy/ingress-nginx/kustomization.yaml pierde el parche controlador-kind.yaml
  #   manifiesto-tocado:     deploy/ingress-nginx/deploy.yaml gana una línea de comentario
  #   aplicar-base-k:        aplicar-base vuelve a «apply -k deploy/base»
  #   install-sin-limite:    install pierde $(MAKE) verificar-limite
  #   limite-sin-as:         verificar-limite.sh pierde --as=…
  #   limite-sin-comparar:   la comparación respuesta/esperado de verificar-limite.sh da siempre ok
  #   desplegar-sin-digest:  desplegar.sh pierde la comparación de digest y el rollout restart
  sed -i '<mutación>' "$D/<ruta>"
  (cd "$D" && bash "$W/bitacoras/U08-C-evidencia/ca2.sh")                                  # base, overlay, pull, ingress, manifiesto
  (cd "$D" && KUBECONFIG_SYNKRA="$KC" make -n aplicar-base | grep -c -- '-k deploy/base')   # aplicar-base-k
  (cd "$D" && sed -n '/^install:/,/^$/p' Makefile | grep -c 'verificar-limite')            # install-sin-limite
  (cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/verificar-limite.sh; echo "salida=$?")  # limite-sin-as (sin binding de prueba)
  (cd "$D" && KC="$KC" bash "$W/bitacoras/U08-C-evidencia/ca7.sh")                         # limite-sin-comparar
  (cd "$D" && W="$W" KUBECONFIG_SYNKRA="$KC" bash "$W/bitacoras/U08-C-evidencia/ca5-variante.sh")   # desplegar-sin-digest
  rm -rf "$D"
  ```
  Esperado: con cada una de las diez mutaciones (una copia por mutación), su comprobación cae: en `ca2.sh` falta su línea (`base=1`, `overlay=1`, `ingress-ok` o `manifiesto-ok` ausentes); `make -n` da `1`; el bloque de `install` da `0`; `limite-sin-as` da `FALLA` en 3, 4, 5 y 6 y `salida=1` sin ningún binding de prueba; `limite-sin-comparar` da `con-crb=0`; `desplegar-sin-digest` no imprime `corre-la-variante`. Después de las mutaciones que tocan el clúster (`limite-sin-comparar`, `desplegar-sin-digest`), `make desplegar` y `make verificar-limite` desde el worktree vuelven a dar 0 (se pega). La bitácora pega cada mutación y la línea que cayó.
  **Rojo de antes de implementar:** el segundo commit de la rama (después del merge) trae **solo** `scripts/verificar-base.py` y `scripts/verificar-backend.py` con sus listas nuevas (decisión 6) y `bitacoras/U08-C-evidencia/ca2.sh`, `ca3.sh`, `ca6.sh` y `ca7.sh`, sin `Makefile`, `deploy/` ni scripts nuevos; la bitácora pega su sha y su `git show --stat`. En una copia de ese commit: `ca2.sh` da `base=1` (`FALLA` de la lista: 8 en vez de 11), `overlay=1` y sin `manifiesto-ok`; y `make verificar-limite` da `make: *** No rule to make target 'verificar-limite'` (o su traducción).

- [ ] **CA-9**: los scripts nuevos fallan con mensaje claro y el `trap` limpia lo que crearon
  ```bash
  bash -n scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh scripts/imagenes.sh; echo "sintaxis=$?"
  grep -n 'set -euo pipefail' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh
  grep -n -e 'trap' -e 'mktemp' scripts/secreto-tls.sh
  grep -nE 'kubectl ' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh | grep -vF -e '--kubeconfig' | grep -v '^[^:]*:[0-9]*:[[:space:]]*#'; echo "sin-kubeconfig=$?"
  W=$PWD; KC=$W/.kube/kind-synkra.yaml; D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  SYNKRA_REGISTRO_IMAGENES=127.0.0.1:1 bash scripts/imagenes.sh --subir > "$D/out" 2>&1; echo "registro-caido=$?"; grep -c '^FALLA subir' "$D/out"; grep -c 'docker build' "$D/out"
  rv0=$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')
  mkdir "$D/c"; git archive HEAD | tar -x -C "$D/c"; mkdir -m 0700 "$D/c/.tls"; cp .tls/tls.crt "$D/c/.tls/"
  (cd "$D/c" && KUBECONFIG_SYNKRA="$KC" bash scripts/secreto-tls.sh); echo "media-pareja=$?"; ls -A "$D/c/.tls"
  [ "$rv0" = "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')" ] && echo "secreto-intacto"
  ```
  Esperado: `sintaxis=0`; las tres líneas; el `trap` aparece antes del `mktemp`; `sin-kubeconfig=1` (ninguna orden `kubectl` sin `--kubeconfig`); `registro-caido=1`, `1` y `0` (no construyó nada); `FALLA tls: .tls tiene solo tls.crt; no se regenera ni se borra`, `media-pareja=1`, solo `tls.crt` en la copia y `secreto-intacto`.

- [ ] **CA-10**: el diff queda acotado
  Guardar como `bitacoras/U08-C-evidencia/ca10.sh`; `ca10.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  M=$(git rev-list --merges -1 HEAD)
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat "$M"...HEAD
  git diff --name-only "$M"...HEAD | grep -v -E '^(Makefile|deploy/base/kustomization\.yaml|deploy/overlays/kind/(kustomization|backend-kind)\.yaml|deploy/ingress-nginx/(deploy|kustomization|controlador-kind)\.yaml|scripts/(imagenes|secreto-tls|desplegar|verificar-limite)\.sh|scripts/verificar-(base|backend)\.py|bitacoras/U08-C\.md|bitacoras/U08-C-evidencia/.*|revisiones/U08-C/.*)$'; echo "fuera=$?"
  git diff --name-only "$M"...HEAD -- backend core init-descarga plugins deploy/base/namespaces.yaml deploy/base/rbac.yaml deploy/base/backend deploy/kind scripts/guardia-contexto.sh scripts/registro-local.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh | wc -l
  git ls-files .kube .tls | wc -l
  git ls-files | grep -cE '(^|/)tls\.(crt|key)$|\.pem$'
  ```
  Esperado: `fuera=1` (solo esas rutas); `0` (nada de U08-A, U08-B ni del código); `0` y `0` (ni kubeconfig ni certificados en git). El sha impreso es el del padre del commit de `ca10.txt`.

---

**Rojo primero:** después del merge (decisión 1), el primer commit propio trae solo los verificadores con sus listas nuevas y los guiones de CA-2, CA-3, CA-6 y CA-7 (CA-8, «Rojo de antes de implementar»); la implementación (`Makefile`, `deploy/`, scripts nuevos, `--subir`) va en commits posteriores. Después, CA-8 demuestra que cada regla muerde con el código terminado. Al cerrar, `kind-synkra` queda arriba con SynKra instalado (decisión 12).
