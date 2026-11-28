# U08-B: PVC y Deployment del backend, overlay `kind` con Ingress y TLS, imágenes multi-stage sin root

**Unidad:** U08: `Instalación y límite`
**Tareas del plan que agrupa:** U08-T05, U08-T06 y U08-T07
**Historias que implementa:** H-26 (T05, T06), H-28 (T07)
**Depende de:** U02, U03, U05 y U07 (terminadas). Sale de `main` `8306ee7`. Corre **en paralelo con U08-A**, con el reparto de archivos de la decisión 1: esta tarea **no toca el `Makefile`, `.gitignore` ni `deploy/kind/`**, y **no usa ningún clúster** (ni siquiera `kind-synkra`, que U08-A crea y prueba a la vez). Lo que necesita de `make` o del clúster queda declarado como pendiente para U08-C (decisión 2).

---

## Alcance

**Dentro** (una línea, concreta):

> `deploy/base/backend/` (PVC `synkra-datos` de 1 Gi `ReadWriteOnce`, Deployment `synkra-backend` con `strategy: Recreate`, `/data` montado, cuenta `deployer`, sin root, recursos y sondas, y Service `synkra-backend:8080`), `deploy/overlays/kind/` (imagen de `localhost:5001`, variables del registro permitido y del init, e Ingress `synkra.local` con TLS `synkra-local-tls`), `scripts/certificado-tls.sh` (certificado autofirmado), y las tres imágenes `localhost:5001/synkra-backend:0.1.0`, `localhost:5001/synkra-core-ejemplo:2.1.0` y `localhost:5001/synkra-init-descarga:1.0.0` construidas con `scripts/imagenes.sh` (multi-stage, base por digest, `USER 10001:10001`, `arm64` nativo) y atadas a `ConstructorObjetosPais` por `scripts/verificar-imagenes.sh` (C-98, C-101, C-103, y `shellcheck` en la etapa de build del init, C-95).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- **Todo clúster** (AUTONOMIA-01). Esta tarea no corre `kind` ni ningún `kubectl` que hable con un servidor: solo `kubectl kustomize`, que no lo hace. No se lee ni se escribe `~/.kube/config` ni `.kube/kind-synkra.yaml`. En esta máquina corren `gitops2` y `m08-gitops1` (de otros cursos) y hay contenedores parados `gitops*-registry` y `gitops*-gitea`: **ninguna orden los lista, los toca ni los borra** (nada de `docker ps -a`, `docker rm` o `docker rmi` sin un nombre propio de esta tarea, ni `docker system prune`).
- `sudo` y `/etc/hosts`. Los contenedores de prueba publican solo en `127.0.0.1`.
- `docker push` y el registro `kind-registry` (los crea U08-A; subir es de U08-C). Las imágenes se etiquetan con el prefijo `localhost:5001/` pero se quedan en el Docker local.
- Lo de U08-A: `Makefile`, `.gitignore`, `deploy/kind/`, `deploy/base/kustomization.yaml`, `deploy/base/namespaces.yaml`, `deploy/base/rbac.yaml` y los scripts `guardia-contexto.sh`, `registro-local.sh` y `verificar-base.py`. El ServiceAccount `deployer` lo crea U08-A; aquí solo se nombra.
- Lo de U08-C (decisión 2): `make imagenes`, `make install`, Ingress NGINX, crear el secreto TLS en el clúster, `kubectl apply`, la validación en el servidor (`--dry-run=server`), `make verificar-limite`, y unir `deploy/base/backend` a `deploy/base`.
- El front (U09): ni `synkra-front` ni la ruta `/` del Ingress (decisión 7).
- Cambios en el código Java, en `application.properties`, en `init-descarga/descargar.sh` (si `shellcheck` encuentra algo, se para y se reporta: no se arregla aquí), en `init-descarga/test.sh`, en `core/smoke.sh` (sigue usando su etiqueta `synkra-core:2.1.0`), en `core/Dockerfile` (ya cumple: multi-stage, base por digest, `USER 10001:10001`), y en `aidlc-docs/`, `CANDIDATAS.md`, `unidades-y-tareas.md`, el plan de U08 y `loop/`.
- C-97 (`fsGroup` del pod de país): es de `ConstructorObjetosPais`, se mira en U08-C con el pod real. C-17 sí se decide aquí para el backend (decisión 5).
- Fijar la versión de `curl` en el init: Alpine solo guarda la última versión de cada rama, así que un `curl=<versión>` rompe el build cuando sale un parche. Se anota como desviación de C-98 (decisión 9).
- Construir para varias arquitecturas (`buildx` multi-plataforma, QEMU): decisión 8.

---

## Archivos de contexto

- `loop/CONTINUAR.md` («Lo que sigue», punto 2: agrupación y reglas de U08)
- `aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md` (U08-T05 a T07) y `unidades-y-tareas.md` (sección U08)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (filas «Cómputo», «Almacenamiento», «Red», «Cómo se instala») y `deployment-architecture.md`
- `backend/Dockerfile`, `backend/.dockerignore`, `core/Dockerfile`, `core/.dockerignore`, `init-descarga/Dockerfile`, `init-descarga/.dockerignore`, `init-descarga/descargar.sh`
- `backend/src/main/resources/application.properties` (`SYNKRA_DATA`, `SYNKRA_REGISTRO_PERMITIDO`, `SYNKRA_PAISES`, `SYNKRA_KUBERNETES_IMAGEN_INIT`, `management.*`)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (`IMAGEN_INIT_POR_DEFECTO`, `UID_CONTENEDORES`, `GID_CONTENEDORES`, `PUERTO`, `RUTA_PLUGINS`, `RUTA_CONFIG`, `COMANDO_INIT`, `URL_CATALOGO`, `RUTA_READINESS`), `ConfiguracionKubernetes.java` y `CuentaServicio.java` (el backend necesita el token montado de su cuenta y `KUBERNETES_SERVICE_HOST`)
- `backend/build.gradle.kts` (`version = "0.1.0"`) y `core/build.gradle.kts` (`version = "2.1.0"`)
- `CANDIDATAS.md`: C-17, C-72, C-95, C-98, C-101, C-103
- Plantillas de estilo: `tareas/U07-G-fronteras-resumen.md` y `tareas/U06-A-registro-base.md`

---

## Decisiones del orquestador

1. **Reparto de archivos con U08-A** (corren a la vez; ninguna ruta se comparte). **U08-B (esta)** escribe solo:
   - `deploy/base/backend/kustomization.yaml`, `deploy/base/backend/pvc.yaml`, `deploy/base/backend/deployment.yaml`, `deploy/base/backend/service.yaml`;
   - `deploy/overlays/kind/kustomization.yaml`, `deploy/overlays/kind/backend-kind.yaml`, `deploy/overlays/kind/ingress.yaml`;
   - `backend/Dockerfile`, `backend/.dockerignore`, `core/.dockerignore`, `init-descarga/Dockerfile`;
   - `scripts/imagenes.sh`, `scripts/verificar-imagenes.sh`, `scripts/verificar-backend.py`, `scripts/certificado-tls.sh`;
   - `bitacoras/U08-B.md`, `bitacoras/U08-B-evidencia/`, `revisiones/U08-B/`.
   U08-A escribe el `Makefile`, `.gitignore` (con `/.kube/` y `/.tls/`), `deploy/kind/` y `deploy/base/` fuera de `backend/`.
2. **Pendiente para U08-C** (se copia tal cual en la bitácora, sección «Pendiente para U08-C»; esta tarea no lo hace):
   - `deploy/base/kustomization.yaml` gana `- backend`, y `deploy/overlays/kind/kustomization.yaml` pasa de `../../base/backend` a `../../base`.
   - Objetivo `make imagenes` = `scripts/imagenes.sh` más `docker push` de las tres etiquetas a `localhost:5001`.
   - Objetivo `make tls` = `scripts/certificado-tls.sh .tls` (si no existe) y `kubectl create secret tls synkra-local-tls -n synkra --cert .tls/tls.crt --key .tls/tls.key` (idempotente con `--dry-run=client -o yaml | kubectl apply -f -`).
   - Ingress NGINX para `kind` (nodo con `ingress-ready=true`, de U08-A) y `kubectl apply --dry-run=server -k deploy/overlays/kind` antes del `apply` (sustituye a `kubeconform -strict`, que no está).
   - Comprobar en el pod real: que el backend arranca con su cuenta, que `/data` es escribible por `10001` en el PVC de `local-path`, y que `curl --resolve synkra.local:443:127.0.0.1 -k https://synkra.local/actuator/health` responde.
3. **Nombres fijos.**
   - Namespace `synkra` en los cuatro objetos de `deploy/base/backend/` (cada objeto lo lleva en su `metadata`; la `kustomization.yaml` no usa `namespace:`).
   - PVC `synkra-datos`; Deployment, contenedor y Service `synkra-backend`; puerto del contenedor y del Service `8080`, con nombre `http`.
   - Etiquetas en todos: `app.kubernetes.io/name: synkra-backend` y `app.kubernetes.io/part-of: synkra`; el selector del Deployment y del Service es solo `app.kubernetes.io/name: synkra-backend`.
   - Imágenes y etiquetas: `localhost:5001/synkra-backend:0.1.0` (versión de `backend/build.gradle.kts`), `localhost:5001/synkra-core-ejemplo:2.1.0` (de `core/build.gradle.kts`) y `localhost:5001/synkra-init-descarga:1.0.0` (igual a `IMAGEN_INIT_POR_DEFECTO`). El prefijo sale de `SYNKRA_REGISTRO_IMAGENES` (por defecto `localhost:5001`) en los dos scripts de imágenes; las copias de mutación usan otro (`mutacion.local:1`) para no pisar las buenas.
   - Ingress `synkra-backend`, clase `nginx`, host `synkra.local`, secreto TLS `synkra-local-tls`.
4. **`deploy/base/backend/deployment.yaml`** (base sin nada de `kind`):
   - `replicas: 1`, `strategy: {type: Recreate}` (RNF-07: un solo escritor de SQLite) y `revisionHistoryLimit: 3`.
   - `serviceAccountName: deployer` y `automountServiceAccountToken: true` (el backend habla con la API solo con ese token, `CuentaServicio`).
   - Imagen `synkra-backend` (sin registro ni tag: los pone el overlay), `imagePullPolicy: IfNotPresent`.
   - Variables: `SYNKRA_DATA=/data`, `SYNKRA_PAISES=co,pe`, `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` (las sondas `liveness` y `readiness` existen también fuera de Kubernetes, así CA-6 las prueba con `docker run`) y `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70`.
   - Recursos del diseño: `requests` `memory: 256Mi`, `cpu: 250m`; `limits` `memory: 512Mi`, `cpu: 500m`.
   - Sondas sobre el puerto `http`: `startupProbe` `GET /actuator/health/liveness`, `periodSeconds: 5`, `failureThreshold: 36` (3 min, el arranque con 500m es lento); `livenessProbe` `GET /actuator/health/liveness`, `periodSeconds: 10`, `failureThreshold: 3`; `readinessProbe` `GET /actuator/health/readiness`, `periodSeconds: 5`, `failureThreshold: 3`.
   - Volúmenes: `datos` = PVC `synkra-datos` en `/data`; `tmp` = `emptyDir` (`sizeLimit: 256Mi`) en `/tmp` (Tomcat escribe ahí las partes multipart).
5. **Sin root (H-28) y C-17.** `securityContext` del pod: `runAsNonRoot: true`, `runAsUser: 10001`, `runAsGroup: 10001`, `fsGroup: 10001`, `fsGroupChangePolicy: OnRootMismatch`, `seccompProfile: {type: RuntimeDefault}`. Del contenedor: `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `capabilities: {drop: ["ALL"]}`. Con esto C-17 queda decidida para el backend: UID fijo 10001 y `fsGroup` 10001 para el PVC (se anota en la bitácora; `CANDIDATAS.md` no se toca).
6. **`pvc.yaml` y `service.yaml`.** PVC `accessModes: [ReadWriteOnce]`, `storageClassName: standard`, `resources.requests.storage: 1Gi`. Service `type: ClusterIP`, puerto `8080` con `targetPort: http`, `protocol: TCP`, `name: http`. Es el que usa el init de cada país (`URL_CATALOGO` = `http://synkra-backend.synkra.svc:8080`).
7. **Overlay `kind`.**
   - `kustomization.yaml`: `resources: [../../base/backend, ingress.yaml]` (U08-C lo cambia a `../../base`, decisión 2), `patches: [{path: backend-kind.yaml}]` y `images: [{name: synkra-backend, newName: localhost:5001/synkra-backend, newTag: 0.1.0}]`.
   - `backend-kind.yaml` (parche estratégico del Deployment `synkra-backend`): añade `SYNKRA_REGISTRO_PERMITIDO=localhost:5001` (R-C3, H-31: el único registro permitido es el local) y `SYNKRA_KUBERNETES_IMAGEN_INIT=localhost:5001/synkra-init-descarga:1.0.0` (igual al defecto, explícito para que se vea qué imagen usan los países). Esas dos variables **no** van en la base.
   - `ingress.yaml`: `networking.k8s.io/v1`, namespace `synkra`, `ingressClassName: nginx`, `tls: [{hosts: [synkra.local], secretName: synkra-local-tls}]`, y en el host `synkra.local` dos rutas al Service `synkra-backend` puerto `http`: `/api` (`Prefix`) y `/actuator/health` (`Prefix`, para la salud del script de punta a punta). Sin anotaciones salvo `nginx.ingress.kubernetes.io/proxy-body-size: "25m"` (por encima de `synkra.max-artefacto` de 20 MB, para que el 413 lo dé R-C5 y no NGINX).
   - Desviación del plan, a la bitácora: U08-T06 dice «`/` → front»; el front es U09 y su Service no existe, así que la ruta `/` la añade U09.
8. **Arquitectura.** `docker build` nativo (BuildKit, sin `--platform` ni `buildx` multi-plataforma): en esta máquina sale `arm64`, y en la de David saldría `amd64`. Las bases van por digest del **índice** multi-arquitectura, así el mismo Dockerfile sirve en las dos. `verificar-imagenes.sh` comprueba que la arquitectura de cada imagen es la del servidor Docker (`docker version -f '{{.Server.Arch}}'`), que es la de los nodos de `kind`.
9. **Dockerfiles.**
   - `backend/Dockerfile` (C-101): las dos etapas `FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70` (el mismo digest que `core/Dockerfile`), con el comentario de por qué. El resto no cambia (`USER 10001:10001`, `/data` de 10001, `EXPOSE 8080`).
   - `init-descarga/Dockerfile` (C-98 y C-95): pasa a dos etapas. Etapa `revision`: `FROM alpine:3.20@sha256:<digest del índice>`, `apk add --no-cache shellcheck`, `COPY descargar.sh`, `RUN shellcheck -s sh descargar.sh` (si falla, el build falla). Etapa final: la misma base por digest, `apk add --no-cache curl`, el usuario `10001:10001` como ahora, `COPY --from=revision --chmod=0555` del script a `/usr/local/bin/descargar.sh`, y al final `apk del --no-cache apk-tools` (la imagen final no puede instalar paquetes). El digest lo resuelve el codificador con `docker buildx imagetools inspect alpine:3.20` (o del `RepoDigests` tras un `docker pull alpine:3.20`) y lo pega en la bitácora con la orden. Es la única descarga permitida, junto con el paquete `shellcheck` de Alpine; si no hay red, se para y se reporta.
   - `curl` sin versión fija (ver «Fuera»): desviación de C-98 que se anota.
   - `backend/.dockerignore` y `core/.dockerignore` (C-103): se quitan las líneas muertas (las que van después de `**` y excluyen algo que `**` ya excluye: `src/`, `.gradle/`, `*.db`, `*.db-*`). Quedan `**`, `!build/libs/<proyecto>-*.jar` y `build/libs/*-plain.jar`, con el comentario de la primera línea.
10. **`scripts/imagenes.sh`** (`bash`, `set -euo pipefail`): exige `JAVA_HOME` en Java 21 (si `"$JAVA_HOME/bin/java" -version` no dice `21`, sale con 1 y lo dice); corre `./gradlew :backend:bootJar :core:bootJar`; construye las tres imágenes de la decisión 3 con `docker build -t`; comprueba que la versión de cada etiqueta es la de `build.gradle.kts` o de `IMAGEN_INIT_POR_DEFECTO` (lee los archivos con `grep`; si no cuadran, sale con 1 antes de construir). Cada fallo (`gradlew`, `docker build`) da `FALLA <imagen>: <orden> salió con <código>` y 1. No sube nada.
11. **`scripts/verificar-imagenes.sh`** (C-103 y H-28; `bash`, `set -euo pipefail`, con `trap` que borra los contenedores que arranca, registrados por nombre antes de crearlos). Para las tres imágenes, sin red:
    - toda línea `FROM` de los tres Dockerfile lleva `@sha256:` de 64 hexadecimales;
    - `Config.User` es `<UID>:<GID>` con los valores de `UID_CONTENEDORES` y `GID_CONTENEDORES` leídos de `ConstructorObjetosPais.java` (hoy `10001:10001`), nunca vacío, `root` ni `0`, y `docker run --rm --entrypoint id <imagen> -u` da el UID;
    - arquitectura igual a la del servidor Docker (decisión 8);
    - backend y core: `ExposedPorts` es exactamente `8080/tcp` (= `PUERTO`); core: `Volumes` es exactamente `/plugins` y `/config` (= `RUTA_PLUGINS` y `RUTA_CONFIG`); init: `Entrypoint` es `[COMANDO_INIT]` y `docker run --rm --entrypoint sh <init> -c 'command -v apk'` no encuentra `apk`;
    - la etiqueta del init es `IMAGEN_INIT_POR_DEFECTO` con el prefijo de `SYNKRA_REGISTRO_IMAGENES`.
    Imprime una línea `ok <imagen> <comprobación>` por cada una y `OK imagenes`, o `FALLA <imagen> <comprobación>: <esperado> / <visto>` y sale con 1. Una constante que no encuentra en el `.java`: `FALLA constantes: <nombre>` y 1.
12. **`scripts/verificar-backend.py`** (`kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py`; PyYAML 6 del sistema): lista **exacta** de objetos (`Deployment`, `Ingress`, `PersistentVolumeClaim` y `Service`, todos `synkra/synkra-backend` salvo el PVC `synkra/synkra-datos`), y cada campo de las decisiones 3 a 7: `Recreate`, `replicas`, cuenta y token, imagen `localhost:5001/synkra-backend:0.1.0`, las seis variables con su valor (las cuatro de la base y las dos del overlay, y ninguna más), recursos exactos, las tres sondas con ruta y puerto, los dos volúmenes y su montaje, los dos `securityContext`, PVC, Service e Ingress (clase, host, TLS, las dos rutas y la anotación). Además: ningún objeto `Cluster*`, ningún `namespace` distinto de `synkra`, ningún `hostPath`, `hostNetwork`, `privileged` ni `:latest`. Imprime `OK backend: 4 objetos` y 0, o una línea `FALLA <regla>: <detalle>` por fallo y 1. Con `--base` comprueba la salida de `kubectl kustomize deploy/base/backend` (3 objetos, imagen `synkra-backend` sin registro y sin las dos variables del overlay).
13. **`scripts/certificado-tls.sh <directorio>`** (`bash`, `set -euo pipefail`): el directorio tiene que existir; si ya hay `tls.crt` o `tls.key`, sale con 1 sin sobrescribir. Genera con `openssl req -x509` una clave EC P-256 y un certificado de 365 días, `CN=synkra.local` y `subjectAltName=DNS:synkra.local`; `tls.key` con `0600`. Escribe primero en archivos temporales del mismo directorio, registrados en el `trap` antes de crearlos, y los renombra al final: si `openssl` falla, no queda ningún archivo a medias. Todo fallo de `openssl` da `FALLA certificado: <orden> salió con <código>` y 1. No habla con ningún clúster.
14. **Limpieza al terminar.** Esta tarea no deja nada corriendo: los contenedores de CA-5 y CA-6 se borran en su `trap`, las copias de `mktemp -d` se borran, y las imágenes con prefijo `mutacion.local:1/` se borran al final de CA-8 (`docker rmi` por nombre). Las tres imágenes buenas `localhost:5001/…` **se quedan** en el Docker local para U08-C (que las sube). Las imágenes base (`alpine` por digest) quedan en la caché.
15. **Bitácora en `bitacoras/U08-B.md`** (con la sección «Pendiente para U08-C» de la decisión 2 y las desviaciones de las decisiones 7 y 9), evidencia en `bitacoras/U08-B-evidencia/` (un `caN.sh` versionado y su `caN.txt` con fecha, sha, `JAVA_HOME`, comando y salida completa), y el revisor en `revisiones/U08-B/`. `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2` en esta máquina (se anota la ruta usada).

---

## Criterios de aceptación

Ningún criterio habla con un clúster: `kubectl kustomize` es la única orden de `kubectl`, y no usa ningún kubeconfig.

- [ ] **CA-1**: U08-T05. La base del backend: PVC, Deployment `Recreate` y Service
  ```bash
  kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base; echo "salida=$?"
  kubectl kustomize deploy/base/backend | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; print(sorted((o['kind'], o['metadata']['namespace'], o['metadata']['name']) for o in os_)); d=[o for o in os_ if o['kind']=='Deployment' and o['metadata']['name']=='synkra-backend'][0]; s=d['spec']['template']['spec']; print(d['spec']['strategy']['type'], s['serviceAccountName'], s['automountServiceAccountToken'], s['securityContext']['runAsNonRoot'], s['securityContext']['runAsUser'], s['securityContext']['fsGroup']); print(sorted((m['name'], m['mountPath']) for m in s['containers'][0]['volumeMounts']))"
  kubectl kustomize deploy/base/backend | grep -cE 'kind: Cluster|localhost:5001|SYNKRA_REGISTRO_PERMITIDO'
  ```
  Esperado: `OK backend: 3 objetos` y `salida=0`; `[('Deployment', 'synkra', 'synkra-backend'), ('PersistentVolumeClaim', 'synkra', 'synkra-datos'), ('Service', 'synkra', 'synkra-backend')]`; `Recreate deployer True True 10001 10001`; `[('datos', '/data'), ('tmp', '/tmp')]`; `0` (la base no sabe nada de `kind` ni de permisos de clúster).

- [ ] **CA-2**: U08-T06. El overlay `kind`: imagen local, variables del registro e Ingress con TLS
  ```bash
  kubectl kustomize deploy/overlays/kind > /dev/null; echo "kustomize=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "salida=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; d=[o for o in os_ if o['kind']=='Deployment'][0]['spec']['template']['spec']['containers'][0]; print(d['image']); print(sorted((e['name'], e['value']) for e in d['env'])); i=[o for o in os_ if o['kind']=='Ingress'][0]['spec']; print(i['ingressClassName'], i['tls']); print([(r['host'], [(p['path'], p['pathType'], p['backend']['service']['name'], p['backend']['service']['port']) for p in r['http']['paths']]) for r in i['rules']])"
  grep -c 'synkra-front' deploy/overlays/kind/*.yaml
  ```
  Esperado: `kustomize=0`; `OK backend: 4 objetos` y `salida=0`; `localhost:5001/synkra-backend:0.1.0`; `[('JAVA_TOOL_OPTIONS', '-XX:MaxRAMPercentage=70'), ('MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED', 'true'), ('SYNKRA_DATA', '/data'), ('SYNKRA_KUBERNETES_IMAGEN_INIT', 'localhost:5001/synkra-init-descarga:1.0.0'), ('SYNKRA_PAISES', 'co,pe'), ('SYNKRA_REGISTRO_PERMITIDO', 'localhost:5001')]`; `nginx [{'hosts': ['synkra.local'], 'secretName': 'synkra-local-tls'}]`; `[('synkra.local', [('/api', 'Prefix', 'synkra-backend', {'name': 'http'}), ('/actuator/health', 'Prefix', 'synkra-backend', {'name': 'http'})])]`; `0` en los tres archivos.

- [ ] **CA-3**: U08-T06. El certificado autofirmado de `synkra.local`, leído de vuelta con `openssl`
  ```bash
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  bash scripts/certificado-tls.sh "$D"; echo "salida=$?"
  ls "$D"; stat -c '%a %n' "$D/tls.key"
  openssl x509 -in "$D/tls.crt" -noout -subject -ext subjectAltName
  openssl x509 -in "$D/tls.crt" -noout -checkend 86400; echo "vigente=$?"
  [ "$(openssl x509 -in "$D/tls.crt" -noout -pubkey)" = "$(openssl pkey -in "$D/tls.key" -pubout)" ] && echo "par-ok"
  bash scripts/certificado-tls.sh "$D"; echo "repetido=$?"
  bash scripts/certificado-tls.sh "$D/no-existe"; echo "sin-dir=$?"
  ```
  Esperado: `salida=0`; solo `tls.crt` y `tls.key` (ningún temporal); `600`; `subject=CN=synkra.local` y `DNS:synkra.local`; `Certificate will not expire` y `vigente=0`; `par-ok`; `repetido=1` (no sobrescribe; el `sha256sum` de los dos archivos antes y después es el mismo, se pega); `sin-dir=1`. Cada salida 1 trae su línea de error.

- [ ] **CA-4**: U08-T07. Las tres imágenes se construyen, sin root, `arm64`, atadas a `ConstructorObjetosPais`
  ```bash
  export JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2
  bash scripts/imagenes.sh; echo "imagenes=$?"
  for i in synkra-backend:0.1.0 synkra-core-ejemplo:2.1.0 synkra-init-descarga:1.0.0; do docker image inspect -f '{{index .RepoTags 0}} user={{.Config.User}} arch={{.Architecture}}' localhost:5001/$i; done
  docker version -f '{{.Server.Arch}}'
  bash scripts/verificar-imagenes.sh; echo "verificar=$?"
  grep -n '^FROM' backend/Dockerfile core/Dockerfile init-descarga/Dockerfile
  grep -c '^FROM' init-descarga/Dockerfile backend/Dockerfile core/Dockerfile
  grep -n 'shellcheck' init-descarga/Dockerfile
  ```
  Esperado: `imagenes=0`; las tres con `user=10001:10001 arch=arm64` (ninguna vacía, `root` ni `0`); `arm64`; todas las líneas `ok …` de la decisión 11, `OK imagenes` y `verificar=0`; las cinco líneas `FROM` llevan `@sha256:`; `2`, `2` y `2` (las tres son multi-stage); la etapa `revision` del init corre `shellcheck -s sh descargar.sh`, y la bitácora pega la parte del log de `docker build` donde pasa (C-95: es la primera vez que `shellcheck` corre sobre `descargar.sh`).

- [ ] **CA-5**: U08-T07. El init corre como 10001 y sin `apk`, y el script que lleva dentro es el del repositorio y sigue pasando su prueba
  ```bash
  I=localhost:5001/synkra-init-descarga:1.0.0
  docker run --rm --entrypoint id "$I"
  docker run --rm --entrypoint sh "$I" -c 'command -v apk; echo "apk=$?"; command -v curl sha256sum'
  docker run --rm --entrypoint sh "$I" -c 'ls -l /usr/local/bin/descargar.sh'
  [ "$(docker run --rm --entrypoint cat "$I" /usr/local/bin/descargar.sh | sha256sum)" = "$(sha256sum < init-descarga/descargar.sh)" ] && echo "mismo-script"
  sh init-descarga/test.sh; echo "test=$?"
  ```
  Esperado: `uid=10001(synkra) gid=10001(synkra)…`; `apk=1` y las rutas de `curl` y `sha256sum`; `-r-xr-xr-x` del script; `mismo-script` (el que pasó `shellcheck` en la etapa `revision` es el del repositorio); `test=0` (la prueba de U05-T07 corre el `descargar.sh` del repositorio fuera de la imagen; sin cambios, sigue pasando: se pega su salida completa).

- [ ] **CA-6**: U08-T07 y U08-T05. El backend arranca con la configuración del Deployment (sin root, raíz de solo lectura, `/data` y `/tmp` montados) y sus sondas responden
  Guardar como `bitacoras/U08-B-evidencia/ca6.sh`:
  ```bash
  I=localhost:5001/synkra-backend:0.1.0; N=synkra-u08b-backend-$$
  D=$(mktemp -d); chmod 0777 "$D"
  trap 'docker rm -f "$N" >/dev/null 2>&1; rm -rf "$D"' EXIT          # registrado antes del docker run
  docker run -d --name "$N" --user 10001:10001 --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges \
    -e SYNKRA_DATA=/data -e SYNKRA_PAISES=co,pe -e MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true -e JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -e SYNKRA_REGISTRO_PERMITIDO=localhost:5001 \
    -m 512m --cpus 0.5 -v "$D:/data" -p 127.0.0.1::8080 "$I" || { echo "FALLA ca6: docker run"; exit 1; }
  P=$(docker port "$N" 8080/tcp | head -1 | cut -d: -f2)
  timeout 180 bash -c "until curl -sf 127.0.0.1:$P/actuator/health/readiness >/dev/null; do sleep 2; done"; echo "espera=$?"
  for r in /actuator/health/liveness /actuator/health/readiness /actuator/health; do printf '%s ' "$r"; curl -s -o /dev/null -w '%{http_code}\n' 127.0.0.1:$P$r; done
  docker exec "$N" id -u
  ls -ln "$D"
  python3 -I -c "import sqlite3,sys; print(sqlite3.connect(sys.argv[1]).execute('select max(version) from flyway_schema_history').fetchone())" "$D/synkra.db"
  docker logs "$N" 2>&1 | grep -ciE 'read-only file system|permission denied'
  ```
  Esperado: `espera=0`; `200` en las tres rutas; `10001`; `synkra.db` (y `artefactos/` si se crea al arrancar) con dueño `10001`; la última migración (`('4',)` en `8306ee7`; se pega la que salga); `0` (nada intentó escribir fuera de `/data` y `/tmp`). La bitácora anota cuánto tardó la readiness, para contrastar con los 3 min de la `startupProbe`.

- [ ] **CA-7**: los scripts fallan con mensaje claro
  ```bash
  bash -n scripts/imagenes.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh; echo "sintaxis=$?"
  grep -n 'set -euo pipefail' scripts/imagenes.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh
  echo 'no: [yaml' | python3 -I scripts/verificar-backend.py; echo "yaml=$?"
  : | python3 -I scripts/verificar-backend.py; echo "vacio=$?"
  JAVA_HOME=/no/existe bash scripts/imagenes.sh; echo "java=$?"
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  PATH="$D:$PATH"; printf '#!/bin/sh\nexit 7\n' > "$D/openssl"; chmod +x "$D/openssl"
  mkdir "$D/c"; bash scripts/certificado-tls.sh "$D/c"; echo "openssl=$?"; ls -A "$D/c" | wc -l
  ```
  Esperado: `sintaxis=0`; las tres líneas; `FALLA entrada: …` y `yaml=1`; `FALLA entrada: …` y `vacio=1`; una línea que dice que `JAVA_HOME` no es Java 21 y `java=1` (sin construir nada); `FALLA certificado: openssl … salió con 7`, `openssl=1` y `0` (el `trap` no dejó temporales).

- [ ] **CA-8**: rojo primero. Cada regla nueva muerde, demostrado en copias
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   recreate:        deployment.yaml pasa a strategy RollingUpdate
  #   sin-root:        se quita runAsNonRoot del pod
  #   cuenta:          serviceAccountName pasa a default
  #   solo-lectura:    readOnlyRootFilesystem pasa a false
  #   tls:             el Ingress pierde su bloque tls
  #   registro-base:   SYNKRA_REGISTRO_PERMITIDO se mueve del overlay a la base
  #   user-0:          backend/Dockerfile termina con USER 0
  #   sin-digest:      init-descarga/Dockerfile usa FROM alpine:3.20 sin digest
  #   uid-java:        UID_CONTENEDORES pasa a 10002L en ConstructorObjetosPais.java (solo en la copia)
  #   shellcheck:      descargar.sh gana una línea con un error de shellcheck (p. ej. cd $DIR sin comillas)
  sed -i '<mutación>' "$D/<ruta>"
  (cd "$D" && kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py)             # las seis de manifiestos
  (cd "$D" && SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/imagenes.sh && SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/verificar-imagenes.sh)   # las cuatro de imágenes
  rm -rf "$D"
  docker rmi mutacion.local:1/synkra-backend:0.1.0 mutacion.local:1/synkra-core-ejemplo:2.1.0 mutacion.local:1/synkra-init-descarga:1.0.0 2>/dev/null; true
  ```
  Esperado: con cada una de las diez mutaciones (una copia por mutación), su comprobación cae: `verificar-backend.py` imprime su `FALLA <regla>` y sale con 1 (las seis de manifiestos); `verificar-imagenes.sh` imprime `FALLA … User` (user-0), `FALLA … FROM` (sin-digest) o `FALLA … User: 10002:10001 / 10001:10001` (uid-java) y sale con 1; con «shellcheck», `imagenes.sh` sale con `FALLA synkra-init-descarga: docker build …` y el log del build muestra el aviso de `shellcheck`. La bitácora pega cada mutación y la línea que cayó. Las imágenes `localhost:5001/…` buenas no cambian (su `Id` antes y después de CA-8 es el mismo, se pega). Además, la bitácora pega el rojo de antes de implementar: `verificar-imagenes.sh` contra las imágenes construidas desde los Dockerfile de `8306ee7` cae en `FROM` de `backend/Dockerfile` y de `init-descarga/Dockerfile`, y `verificar-backend.py` sin manifiestos cae en `FALLA entrada`. Sin `git stash` ni cambios en el worktree.

- [ ] **CA-9**: el diff queda acotado
  Guardar como `bitacoras/U08-B-evidencia/ca9.sh`; `ca9.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat 8306ee7...HEAD
  git diff --name-only 8306ee7...HEAD | grep -v -E '^(deploy/base/backend/(kustomization|pvc|deployment|service)\.yaml|deploy/overlays/kind/(kustomization|backend-kind|ingress)\.yaml|backend/Dockerfile|backend/\.dockerignore|core/\.dockerignore|init-descarga/Dockerfile|scripts/imagenes\.sh|scripts/verificar-imagenes\.sh|scripts/verificar-backend\.py|scripts/certificado-tls\.sh|bitacoras/U08-B\.md|bitacoras/U08-B-evidencia/.*|revisiones/U08-B/.*)$'; echo "fuera=$?"
  git diff --name-only 8306ee7...HEAD -- Makefile .gitignore deploy/kind deploy/base/kustomization.yaml deploy/base/namespaces.yaml deploy/base/rbac.yaml | wc -l
  git ls-files | grep -cE '(^|/)tls\.(crt|key)$|\.pem$'
  ```
  Esperado: `fuera=1` (solo esas rutas); `0` (nada de lo de U08-A); `0` (ningún certificado ni clave en git). El sha impreso es el del padre del commit de `ca9.txt`.

---

**Rojo primero:** `scripts/verificar-backend.py` y `scripts/verificar-imagenes.sh` se escriben y se commitean **antes** que los manifiestos y los Dockerfile nuevos (ese commit no trae la implementación; la bitácora pega su sha y su `git show --stat`), y se pega su fallo contra `8306ee7` (decisión de CA-8). Después, CA-8 demuestra que cada regla muerde con el código terminado. Al cerrar no queda nada corriendo; las tres imágenes `localhost:5001/…` quedan en el Docker local para U08-C (decisión 14).
