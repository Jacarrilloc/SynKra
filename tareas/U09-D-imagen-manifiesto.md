# U09-D: imagen multi-stage sin root del front y su manifiesto en `kind`, servido en `https://synkra.local/`

**Unidad:** U09: `Front`
**Tareas del plan que agrupa:** U09-T07 y U09-T08
**Historias que implementa:** H-28 (T07), H-26 (T08); contribuye a H-25 (el front corre en el clúster y habla con el backend por su Service)
**Depende de:** U09-B y U09-C (VERDES y fusionadas) y U08-C (VERDE, en `main`). Sale de `<main con U09-B y U09-C>`: el primer `main` que tiene las dos fusiones (rama `m6r/U09-D`, worktree `../wt-m6r-U09-D`). Esa punta es la **unión** de B y C: CA-1 lo comprueba antes de nada. El criterio que usa el clúster (CA-7) se corre **al final**, cuando el orquestador avise de que U08 cerró y nadie más usa `kind-synkra`; todos los demás criterios no usan ningún clúster y se corren antes.

---

## Alcance

**Dentro** (una línea, concreta):

> `front/Dockerfile` (dos etapas, bases por digest: el SDK `10.0.404` que ya usa `scripts/dotnet-docker.sh` y el runtime de ASP.NET `10.0.15`; usuario `10001:10001`; puerto `8080`) y `front/.dockerignore`; la versión del front en `front/Synkra.Front.csproj` (`<Version>0.1.0</Version>`); `deploy/base/front/` (Deployment y Service `synkra-front:8080`, sin root, raíz de solo lectura, sin token de cuenta, con `Synkra__ApiUrl` al Service del backend) y su entrada en `deploy/base/kustomization.yaml`; en el overlay `kind`, la imagen `localhost:5001/synkra-front`, `imagePullPolicy: Always` y un Ingress `synkra-front` en `synkra.local` `/` con el TLS de U08; `scripts/imagenes.sh`, `verificar-imagenes.sh`, `verificar-base.py`, `verificar-backend.py` y `desplegar.sh` ampliados al front sin aflojar ninguna regla, y `scripts/verificar-front.py` nuevo; todo comprobado sin clúster, y al final con `make install` en `kind-synkra`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- **Todo clúster que no sea `kind-synkra`, y `kind-synkra` antes del aviso del orquestador** (AUTONOMIA-01). Nunca `~/.kube/config`: todo `kubectl` lleva `--kubeconfig "$PWD/.kube/kind-synkra.yaml"` (o `KUBECONFIG_SYNKRA`), salvo `kubectl kustomize`, que no habla con ningún servidor y se corre con `KUBECONFIG=/dev/null`. En esta máquina corren `gitops2` y `m08-gitops1` (de otros cursos) y hay contenedores `gitops*`: **ninguna orden los lista, los nombra, los toca, los para ni los borra** (nada de `kind get clusters`, `docker ps` o `docker ps -a` sin un filtro de nombre propio de SynKra, `docker rm`/`docker rmi` sin un nombre de esta tarea, ni `docker system prune`).
- `sudo`, `/etc/hosts`, `kubectl port-forward` y publicar puertos fuera de `127.0.0.1`. Los nombres se resuelven con `curl --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt` (nunca `-k`).
- Código del front y sus pruebas (`front/Api/`, `front/Components/`, `front/Program.cs`, `front/appsettings.json`, `front.Tests/`): esta tarea solo añade `<Version>` al `.csproj`. Si la aplicación no arranca en la imagen o en el clúster, se para y se reporta.
- El backend, `core/`, `init-descarga/`, `plugins/`, sus Dockerfile, `deploy/base/backend/`, `deploy/base/namespaces.yaml`, `deploy/base/rbac.yaml`, `deploy/kind/`, `deploy/ingress-nginx/`, `deploy/overlays/kind/ingress.yaml` y `backend-kind.yaml` (el Ingress del backend no cambia: el del front es otro objeto), y los scripts de U08 que esta tarea no nombra (`guardia-contexto.sh`, `registro-local.sh`, `certificado-tls.sh`, `secreto-tls.sh`, `verificar-limite.sh`, `e2e-api.sh`).
- El `Makefile`: `make install` ya llama a `imagenes` y a `desplegar`, que son los que crecen. Si hace falta tocarlo, se para y se pregunta.
- El recorrido de la demo y el resumen de U09 (U09-E). `aidlc-docs/`, `CANDIDATAS.md`, `unidades-y-tareas.md`, `loop/`.
- Construir para varias arquitecturas o con `--platform`. Health checks nuevos en el front (las sondas usan un archivo estático, decisión 6).
- Cualquier descarga que no esté en la decisión 3.

---

## Archivos de contexto

- `tareas/U08-B-backend-ingress-imagenes.md` (decisiones 3 a 12: nombres, `securityContext`, Dockerfiles por digest, `imagenes.sh`, `verificar-imagenes.sh`, `verificar-backend.py`) y `tareas/U08-C-install-limite.md` (decisiones 1, 2, 5 a 9 y 12: reparto, verificadores de la unión, `--subir`, `desplegar.sh` con la comparación de digest, `imagePullPolicy: Always`, qué queda arriba); `revisiones/U08-B/` y `revisiones/U08-C/` (F-01 de U08-B: `docker run` sin `--pull=never` salía a la red; F-03 y F-06: literales de versión repetidos y chequeos que no podían fallar).
- `tareas/U09-A-proyecto-cliente-vista.md` (decisiones 2, 3 y 8: SDK, `scripts/dotnet-docker.sh`, `Synkra:ApiUrl`), `tareas/U09-B-catalogo-detalle.md` y `tareas/U09-C-desplegar.md` (decisión 1 de las dos: el reparto y los tres archivos comunes; decisión 12 de U09-C: el arranque con una URL buena y una mala).
- `aidlc-docs/construction/plans/U09-front-code-generation-plan.md` (U09-T07, T08) y `unidades-y-tareas.md`; `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` y `deployment-architecture.md` (filas «Cómputo», «Red» y el orden de `make install`).
- De `main`: `Makefile`, `deploy/base/`, `deploy/overlays/kind/`, `scripts/imagenes.sh`, `scripts/verificar-imagenes.sh`, `scripts/verificar-base.py`, `scripts/verificar-backend.py`, `scripts/desplegar.sh`, `bitacoras/U08-C-evidencia/ca4.sh`, `ca5.sh` y `ca5-variante.sh` (estilo de las lecturas de vuelta y de la variante de imagen), `front/`, `front/obj/project.assets.json` de una compilación local (muestra el único paquete que restaura el front, decisión 3).
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (`UID_CONTENEDORES`, `GID_CONTENEDORES`, que `verificar-imagenes.sh` ya lee).
- `CANDIDATAS.md`: C-17 (UID fijo), C-129 (`/tmp` escribible), C-132 (no comparar `Id` de imágenes entre corridas), C-134 y C-135.

---

## Decisiones del orquestador

1. **Reparto de archivos.** Esta tarea escribe solo:
   - `front/Dockerfile`, `front/.dockerignore`, y en `front/Synkra.Front.csproj` **una** línea nueva, `<Version>0.1.0</Version>`, dentro del `PropertyGroup` existente;
   - `deploy/base/front/kustomization.yaml`, `deploy/base/front/deployment.yaml`, `deploy/base/front/service.yaml`, y en `deploy/base/kustomization.yaml` la línea `- front`;
   - `deploy/overlays/kind/kustomization.yaml` (gana `- ingress-front.yaml` en `resources`, `- path: front-kind.yaml` en `patches` y la entrada `synkra-front` en `images`), `deploy/overlays/kind/front-kind.yaml` y `deploy/overlays/kind/ingress-front.yaml`;
   - `scripts/imagenes.sh`, `scripts/verificar-imagenes.sh`, `scripts/verificar-base.py`, `scripts/verificar-backend.py`, `scripts/desplegar.sh`, y nuevo `scripts/verificar-front.py`;
   - `bitacoras/U09-D.md`, `bitacoras/U09-D-evidencia/`, `revisiones/U09-D/`.
2. **Nombres fijos.** Namespace `synkra` en cada objeto (en su `metadata`, sin `namespace:` en la `kustomization.yaml`). Deployment, contenedor, Service e Ingress `synkra-front`; puerto del contenedor y del Service `8080`, con nombre `http`. Etiquetas en todos: `app.kubernetes.io/name: synkra-front` y `app.kubernetes.io/part-of: synkra`; el selector del Deployment y del Service es solo `app.kubernetes.io/name: synkra-front`. Imagen `localhost:5001/synkra-front:<Version del .csproj>` (`0.1.0`); el prefijo sale de `SYNKRA_REGISTRO_IMAGENES`, como las otras tres; las copias de mutación usan `mutacion.local:1`. La versión se escribe **solo** en `front/Synkra.Front.csproj` y en el `newTag` del overlay, y `verificar-backend.py --versiones` comprueba que coinciden (ningún script la repite: F-03 y F-06 de U08-B).
3. **Bases por digest y descargas.**
   - Etapa de compilación: `mcr.microsoft.com/dotnet/sdk:10.0.404@sha256:8c6edfb26c85153f236d26efebd9ad349aec403583f343f201ee67838b23658d` (el `RepoDigests` que anotaron U09-A y su revisor; la misma imagen de `scripts/dotnet-docker.sh`, ya en la caché).
   - Etapa final: `mcr.microsoft.com/dotnet/aspnet:10.0.15@sha256:aac50ceef10391e78a899e3d2ccd00bbafa2dcb56ccf4586cbccf00edb84fc21` (índice multiarquitectura; runtime 10.0.15, el mismo que trae el SDK 10.0.404). **Ya está descargada en la máquina de David** con permiso del responsable del proyecto (`aidlc-docs/audit.md`); la bitácora pega su `docker image inspect --format '{{json .RepoDigests}}'`. No se vuelve a descargar.
   - **El único paquete que restaura el front** es `Microsoft.AspNetCore.App.Internal.Assets` `10.0.15`, que el SDK web añade solo (`autoReferenced`; trae `blazor.web.js`). Dentro de `docker build` no hay acceso al volumen `synkra-nuget`, así que la etapa de compilación lo restaura desde `nuget.org`: es la **única descarga** de esta tarea, ya hecha antes por U09-A en el volumen, y se declara en la bitácora. CA-3 comprueba que la restauración de la etapa trae ese paquete y ningún otro. Si la máquina no tiene red, se para y se reporta (no se busca otro camino).
   - Ninguna otra imagen, paquete ni manifiesto se descarga. Los `docker run` llevan `--pull=never`.
4. **`front/Dockerfile`** (contexto `front/`, como el criterio del plan):
   - Etapa `build` (`FROM <SDK por digest> AS build`, `WORKDIR /src`; CA-3 la construye sola con `--target build`): copia primero `Synkra.Front.csproj` y restaura (capa en caché), después el resto y `dotnet publish -c Release -o /app --no-restore` (sin `-r`, dependiente del framework). `DOTNET_CLI_TELEMETRY_OPTOUT=1` y `DOTNET_NOLOGO=1`.
   - Etapa final: la base de ASP.NET por digest; `WORKDIR /app`; `COPY --from=build /app .` (los archivos de solo lectura para el usuario: dueño `root`, sin escritura de grupo ni otros); `USER 10001:10001` (el UID y GID de `UID_CONTENEDORES` y `GID_CONTENEDORES`, como las otras tres imágenes, C-17); `EXPOSE 8080`; `ENTRYPOINT ["dotnet", "Synkra.Front.dll"]`. El puerto lo fija la base (`ASPNETCORE_HTTP_PORTS=8080`); el Dockerfile no lo cambia.
   - Un comentario por cada `FROM` con el origen del digest.
   - `front/.dockerignore`: `bin/`, `obj/`, `**/TestResults/` (y nada que excluya lo que el `publish` necesita).
5. **`deploy/base/front/deployment.yaml`** (base sin nada de `kind`):
   - `replicas: 1`, `revisionHistoryLimit: 3`, estrategia por defecto (`RollingUpdate`: el front no guarda estado).
   - `automountServiceAccountToken: false` y sin `serviceAccountName` (cuenta `default`): el front no habla con la API de Kubernetes. Imagen `synkra-front` (sin registro ni tag), `imagePullPolicy: IfNotPresent`.
   - Una sola variable: `Synkra__ApiUrl=http://synkra-backend.synkra.svc:8080/` (el Service de U08-B; dentro del clúster, sin pasar por el Ingress ni por TLS: Blazor Server llama a la API desde el servidor, D9).
   - Recursos: `requests` `memory: 128Mi`, `cpu: 100m`; `limits` `memory: 256Mi`, `cpu: 500m`.
   - Sondas sobre el puerto `http` con `GET /app.css` (archivo estático: responde sin llamar a la API, así una API caída no reinicia el front): `startupProbe` `periodSeconds: 2`, `failureThreshold: 30`; `livenessProbe` `periodSeconds: 10`, `failureThreshold: 3`; `readinessProbe` `periodSeconds: 5`, `failureThreshold: 3`.
   - `securityContext` del pod: `runAsNonRoot: true`, `runAsUser: 10001`, `runAsGroup: 10001`, `seccompProfile: {type: RuntimeDefault}`. Del contenedor: `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `capabilities: {drop: ["ALL"]}`. Volumen `tmp` = `emptyDir` (`sizeLimit: 64Mi`) en `/tmp` (el runtime de .NET y ASP.NET escriben ahí; C-129). Sin PVC: las claves de Data Protection quedan en memoria con un aviso en el log, que se acepta con una réplica y se anota.
6. **`service.yaml`, overlay e Ingress.**
   - Service `synkra-front`: `type: ClusterIP`, puerto `8080`, `targetPort: http`, `protocol: TCP`, `name: http`.
   - `deploy/base/front/kustomization.yaml`: `resources: [deployment.yaml, service.yaml]`. `deploy/base/kustomization.yaml` gana `- front` después de `- backend`.
   - Overlay: `images` gana `{name: synkra-front, newName: localhost:5001/synkra-front, newTag: 0.1.0}`; `front-kind.yaml` (parche estratégico del Deployment `synkra-front`) pone `imagePullPolicy: Always` (la misma razón que en U08-C, decisión 9: la misma etiqueta puede volver a subirse); `ingress-front.yaml`: `networking.k8s.io/v1`, Ingress `synkra-front` en `synkra`, `ingressClassName: nginx`, `tls: [{hosts: [synkra.local], secretName: synkra-local-tls}]`, y en el host `synkra.local` una sola ruta `/` (`Prefix`) al Service `synkra-front` puerto `http`. Sin anotaciones (NGINX elige la ruta más larga entre los Ingress del mismo host: `/api` y `/actuator/health` siguen yendo al backend; el WebSocket de `/_blazor` lo soporta el controlador sin anotaciones).
   - Desviación del plan, a la bitácora: U09-T08 pide `kubeconform -strict -summary`, que no está instalado (como en U08-B y U08-C); lo sustituyen `verificar-front.py` sin clúster y `kubectl apply --dry-run=server` de `desplegar.sh` en el clúster.
7. **Los verificadores crecen sin aflojar ninguna regla.**
   - `scripts/verificar-base.py`: la lista exacta gana `('Deployment', 'synkra', 'synkra-front')` y `('Service', 'synkra', 'synkra-front')` (solo `kind`, `namespace` y `name`); imprime `OK base: 13 objetos`. Las prohibiciones globales se aplican a los trece.
   - `scripts/verificar-backend.py` sobre el overlay: sus cuatro objetos con todas sus reglas, y el resto es exactamente los ocho de U08-A **más** el Deployment, el Service y el Ingress `synkra-front`; imprime `OK backend: 4 objetos (más 11 de la base y del front)`. `--base` no cambia. `--versiones` elige el Deployment **por nombre** (`synkra-backend` y `synkra-front`, nunca el primero de la lista) y además compara el `newTag` de `synkra-front` con `<Version>` del `.csproj`: `OK versiones: backend <v>, init <v>, front <v>`.
   - `scripts/verificar-front.py` (nuevo, PyYAML del sistema, `python3 -I`, mismo estilo de `FALLA <regla>: <detalle>` y salida 1): sobre el overlay, la lista exacta de sus objetos (`Deployment`, `Ingress` y `Service`, todos `synkra/synkra-front`) y cada campo de las decisiones 2, 5 y 6: réplicas, cuenta sin token, imagen `localhost:5001/synkra-front:<Version>`, `Always`, la variable y ninguna más, recursos, las tres sondas con ruta, puerto y valores, volumen y montaje, los dos `securityContext`, el Service y el Ingress (clase, TLS, host, la ruta `/` y ninguna otra, al Service `synkra-front`); que **ningún** otro Ingress del overlay tiene la ruta `/`; y las prohibiciones (`Cluster*`, `hostPath`, `hostNetwork`, `privileged`, `:latest`). Imprime `OK front: 3 objetos`. Con `--base`, sobre `deploy/base/front`: 2 objetos, imagen `synkra-front` sin registro, `IfNotPresent`.
   - `scripts/verificar-imagenes.sh`: las comprobaciones de U08-B, sin cambiar, para las tres de antes, y para `synkra-front`: toda línea `FROM` de `front/Dockerfile` por digest; `Config.User` = `UID_CONTENEDORES:GID_CONTENEDORES` (nunca vacío, `root` ni `0`) y `id -u` dentro; arquitectura igual a la del servidor Docker; `ExposedPorts` exactamente `8080/tcp` y la variable `ASPNETCORE_HTTP_PORTS` de la imagen igual a ese puerto (los dos leídos de la imagen; el `8080` sale del `containerPort` de `kubectl kustomize deploy/base/front`, no se escribe); `Entrypoint` `["dotnet","Synkra.Front.dll"]`; y **sin SDK en la imagen final**: `docker run --pull=never --rm --entrypoint dotnet <imagen> --list-sdks` no imprime nada. Una línea `ok <imagen> <comprobación>` por cada una y `OK imagenes`.
8. **`scripts/imagenes.sh`**: construye `synkra-front` con la misma función `construir` (sin atestaciones) y la misma variable de prefijo, después de las tres de U08, con la versión leída del `.csproj`; con `--subir`, la verifica, la sube y la lee de vuelta del registro igual que las otras (cuatro líneas `subida …`). Todo lo demás, igual.
9. **`scripts/desplegar.sh`**: los pasos 1 y 2 no cambian (webhook, `--dry-run=server`, `apply -k`). El 3 y el 4 se hacen para **cada** Deployment de SynKra en `synkra`, por nombre (`synkra-backend` y `synkra-front`; la lista sale del render del overlay, no se escribe dos veces): `rollout status` (300 s el backend, por su `startupProbe`; el front con el mismo tope), y la comparación del digest del pod con el `Docker-Content-Digest` del registro, con `rollout restart` si difieren. Imprime un `OK desplegar: <imagen> <digest>` por Deployment. Motivo: hoy el paso 4 toma «el primer Deployment» del render; con el front ya no se sabe cuál es.
10. **Qué queda al terminar.** Sin clúster (CA-1 a CA-6): ningún contenedor de esta tarea corriendo (los `trap` los borran por nombre), ni copias de `mktemp -d`, ni imágenes `mutacion.local:1/…` ni la etiqueta `localhost:5001/synkra-front:latest` del criterio literal del plan (se borra por nombre). Queda `localhost:5001/synkra-front:0.1.0` en el Docker local. Con clúster (CA-7): `kind-synkra` con SynKra y el front instalados, para U09-E.
11. **Bitácora en `bitacoras/U09-D.md`**, evidencia en `bitacoras/U09-D-evidencia/` (un `caN.sh` versionado y su `caN.txt` con fecha, sha, `uname -m`, `docker version -f '{{.Server.Arch}}'`, `JAVA_HOME`, comando y salida completa). **Todo se corre en la máquina de David (x86_64)**: ningún esperado lleva arquitectura escrita (se compara con la del servidor Docker). `JAVA_HOME` como en U08-C (decisión 5). Toda la evidencia sale del **último commit que toca código, manifiestos o scripts**; si después cambian, se vuelve a correr. Las mutaciones se hacen en copias (`git archive HEAD | tar -x -C "$(mktemp -d)"` y `git init -q` dentro), nunca con `git stash` ni en el worktree. Desviaciones mínimas: la restauración con red de la decisión 3, `kubeconform` (decisión 6), las sondas sobre `/app.css` y las claves de Data Protection en memoria (decisión 5), y el cambio de `desplegar.sh` (decisión 9).

---

## Criterios de aceptación

En todos, desde la raíz del worktree. CA-1 a CA-6 **no** hablan con ningún clúster (`kubectl kustomize` con `KUBECONFIG=/dev/null` es la única orden de `kubectl`). CA-7 es el único que usa `kind-synkra`, y se corre al final.

- [ ] **CA-1**: la base es la unión de U09-B y U09-C, y en ella pasan los criterios del plan de U09-T03, T04 y T05
  ```bash
  git merge-base --is-ancestor "<punta VERDE de U09-B>" HEAD; echo "b=$?"
  git merge-base --is-ancestor "<punta VERDE de U09-C>" HEAD; echo "c=$?"
  git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
  rm -rf front.Tests/TestResults
  for f in CatalogoTest DesplegarTest DetalleTest; do
    scripts/dotnet-docker.sh test front.Tests --filter "$f" --logger "trx;LogFileName=$f.trx" --results-directory front.Tests/TestResults > /dev/null; echo "$f salida=$?"
    python3 -I bitacoras/U09-A-evidencia/trx.py "front.Tests/TestResults/$f.trx" | head -1
  done
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/CatalogoTest.trx | grep -c -x 'DescriptorInvalidoMuestraElMotivo'
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/DesplegarTest.trx | grep -c -x 'ConVeredictoRechazadoLosBotonesEstanDeshabilitados'
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/DetalleTest.trx | grep -c -x 'LaVueltaSoloSeConfirmaDespuesDeConfirmar'
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults > /dev/null; echo "todo=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
  grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\bbool |\.)(Core)?Compatible\b'; echo "resto=$?"
  ```
  Esperado: `b=0` y `c=0` (las dos puntas VERDES están en la base; el orquestador da los dos sha); los tres blobs de la decisión 1 de U09-B y U09-C (`899488ab…`, `85479d56…`, `d5d7babb…`): la unión no tocó los archivos comunes; las tres corridas con `salida=0` y `passed` = `total`, `failed=0 notExecuted=0`: `CatalogoTest` con 24 (18 de U09-B y 6 de U09-C), `DesplegarTest` con 17, `DetalleTest` con 21 (14 de U09-B y 7 de U09-C); `1`, `1` y `1` (los tres casos que nombra el plan, ahora juntos); `todo=0` con la suite completa (29 + 35 + 48 = 112 si la base no trae otras; si trae más, se anota de dónde); `resto=1`. Si algo de esto falla, la unión está mal: se para y se reporta, no se arregla aquí.

- [ ] **CA-2**: U09-T07. El criterio literal del plan, y la imagen con versión
  ```bash
  docker image inspect --format '{{json .RepoDigests}}' mcr.microsoft.com/dotnet/aspnet:10.0.15 mcr.microsoft.com/dotnet/sdk:10.0.404
  docker build -t localhost:5001/synkra-front front/ && docker inspect -f '{{.Config.User}}' localhost:5001/synkra-front; echo "plan=$?"
  docker rmi localhost:5001/synkra-front:latest
  export JAVA_HOME=<el de la decisión 11>
  bash scripts/imagenes.sh; echo "imagenes=$?"
  docker image inspect -f '{{index .RepoTags 0}} user={{.Config.User}} arch={{.Architecture}}' localhost:5001/synkra-front:0.1.0
  docker version -f '{{.Server.Arch}}'
  grep -n '^FROM' front/Dockerfile; grep -c '^FROM' front/Dockerfile
  ```
  Esperado: los dos `RepoDigests` con `sha256:aac50cee…` y `sha256:8c6edfb2…` (los de la decisión 3); `10001:10001` (no vacío, `root` ni `0`) y `plan=0`; la etiqueta `latest` borrada por nombre; `imagenes=0` con las cuatro imágenes construidas; `localhost:5001/synkra-front:0.1.0 user=10001:10001 arch=<la del servidor Docker>`; las dos líneas `FROM` con `@sha256:` y `2` (multi-stage).

- [ ] **CA-3**: la imagen: verificada, sin SDK, y la etapa de compilación solo restauró el paquete declarado
  Guardar como `bitacoras/U09-D-evidencia/ca3.sh`:
  ```bash
  bash scripts/verificar-imagenes.sh; echo "verificar=$?"
  N=synkra-u09d-build-$$; trap 'docker rmi "$N:etapa" >/dev/null 2>&1' EXIT
  docker build -q --target build -t "$N:etapa" front/ > /dev/null; echo "etapa=$?"
  docker run --pull=never --rm --entrypoint cat "$N:etapa" /src/obj/project.assets.json | python3 -I -c 'import json,sys; d=json.load(sys.stdin); print(sorted(d["libraries"]))'
  docker run --pull=never --rm --entrypoint dotnet localhost:5001/synkra-front:0.1.0 --list-sdks | wc -l
  docker run --pull=never --rm --entrypoint dotnet localhost:5001/synkra-front:0.1.0 --list-runtimes
  docker run --pull=never --rm --entrypoint sh localhost:5001/synkra-front:0.1.0 -c 'ls -ln /app | head -5; touch /app/x 2>&1; echo "escribe=$?"'
  ```
  Esperado: todas las líneas `ok …` de la decisión 7 (también las de las tres imágenes de U08), `OK imagenes` y `verificar=0`; `etapa=0`; `['Microsoft.AspNetCore.App.Internal.Assets/10.0.15']` (el único paquete; la bitácora pega esta línea como prueba de la descarga declarada; la ruta `/src` es la del `WORKDIR` de la etapa, y si el codificador eligió otra se ajusta en el guion y se anota); `0` (sin SDK); los runtimes `Microsoft.AspNetCore.App 10.0.15` y `Microsoft.NETCore.App 10.0.15`; los archivos de `/app` de dueño `0` y `escribe=` distinto de 0 (el usuario 10001 no puede escribir en la aplicación).

- [ ] **CA-4**: la imagen arranca sin root, con la raíz de solo lectura, y no arranca con una URL mala (F-04 de U09-A, en la imagen)
  Guardar como `bitacoras/U09-D-evidencia/ca4.sh`:
  ```bash
  I=localhost:5001/synkra-front:0.1.0; N=synkra-u09d-front-$$; M=synkra-u09d-malo-$$
  trap 'docker rm -f "$N" "$M" >/dev/null 2>&1' EXIT          # registrado antes de los docker run
  docker run --pull=never -d --name "$N" --user 10001:10001 --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges \
    -e Synkra__ApiUrl=http://127.0.0.1:9/ -m 256m --cpus 0.5 -p 127.0.0.1::8080 "$I" || { echo "FALLA ca4: docker run"; exit 1; }
  P=$(docker port "$N" 8080/tcp | head -1 | cut -d: -f2)
  timeout 60 bash -c "until curl -sf 127.0.0.1:$P/app.css >/dev/null; do sleep 1; done"; echo "espera=$?"
  curl -s -o /dev/null -w '%{http_code}\n' 127.0.0.1:$P/app.css
  curl -s 127.0.0.1:$P/ | grep -o -e '<h1>Vista general</h1>' -e 'API_NO_DISPONIBLE: No se pudo conectar con la API de SynKra' | sort -u
  docker exec "$N" id -u
  docker logs "$N" 2>&1 | grep -ciE 'read-only file system|permission denied|unhandled exception'
  docker run --pull=never --name "$M" --user 10001:10001 --read-only --tmpfs /tmp -e Synkra__ApiUrl=ftp://h/ "$I" > /dev/null 2>&1; echo "malo=$?"
  docker logs "$M" 2>&1 | grep -c 'Synkra:ApiUrl falta o no es una URL absoluta http o https'
  ```
  Esperado: `espera=0`; `200`; las dos líneas (`<h1>Vista general</h1>` y el error `API_NO_DISPONIBLE` del prerender: el front arrancó con la URL de la variable y llamó a la API); `10001`; `0`; `malo=` distinto de 0 y `1` (con una URL mala el contenedor no arranca, y lo dice). La bitácora anota los avisos de Data Protection que salgan (decisión 5).

- [ ] **CA-5**: U09-T08 sin clúster. Los manifiestos de la base y del overlay
  Guardar como `bitacoras/U09-D-evidencia/ca5.sh`:
  ```bash
  export KUBECONFIG=/dev/null
  kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "base=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "backend=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py --versiones; echo "versiones=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-front.py; echo "front=$?"
  kubectl kustomize deploy/base/front | python3 -I scripts/verificar-front.py --base; echo "front-base=$?"
  kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base; echo "solo-backend=$?"
  kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in os_)); print(sorted((r['host'], p['path'], p['backend']['service']['name']) for o in os_ if o['kind']=='Ingress' for r in o['spec']['rules'] for p in r['http']['paths']))"
  grep -nE '0\.1\.0' scripts/*.sh scripts/*.py; echo "literales=$?"
  ```
  Esperado: `OK base: 13 objetos` y `base=0`; `OK backend: 4 objetos (más 11 de la base y del front)` y `backend=0`; `OK versiones: backend 0.1.0, init 1.0.0, front 0.1.0` y `versiones=0`; `OK front: 3 objetos` y `front=0`; `front-base=0`; `solo-backend=0`; la lista de quince objetos (los doce de U08-C más `Deployment`, `Ingress` y `Service` `synkra/synkra-front`); las tres rutas `('synkra.local', '/', 'synkra-front')`, `('synkra.local', '/actuator/health', 'synkra-backend')` y `('synkra.local', '/api', 'synkra-backend')`; `literales=1` (ninguna versión escrita en los scripts).

- [ ] **CA-6**: rojo primero y cada regla muerde, en copias (sin clúster)
  ```bash
  W=$PWD; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   user-0:           front/Dockerfile termina con USER 0
  #   sin-digest:       el FROM final de front/Dockerfile pierde su @sha256
  #   una-etapa:        el FROM final pasa a la imagen del SDK (por digest)
  #   sin-root:         el Deployment del front pierde runAsNonRoot
  #   escribible:       readOnlyRootFilesystem pasa a false
  #   token:            automountServiceAccountToken pasa a true
  #   url-api:          Synkra__ApiUrl apunta a https://synkra.local/
  #   ruta-raiz:        ingress-front.yaml manda / al Service synkra-backend
  #   sin-tls:          ingress-front.yaml pierde su bloque tls
  #   base-sin-front:   deploy/base/kustomization.yaml pierde «- front»
  #   tag-distinto:     el newTag de synkra-front en el overlay pasa a 0.2.0
  #   primero:          verificar-backend.py --versiones vuelve a tomar el primer Deployment
  (cd "$D" && KUBECONFIG=/dev/null bash "$W/bitacoras/U09-D-evidencia/ca5.sh")                                         # las de manifiestos
  (cd "$D" && SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/imagenes.sh && SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/verificar-imagenes.sh)   # las de imagen
  rm -rf "$D"; docker rmi mutacion.local:1/synkra-backend:0.1.0 mutacion.local:1/synkra-core-ejemplo:2.1.0 mutacion.local:1/synkra-init-descarga:1.0.0 mutacion.local:1/synkra-front:0.1.0 2>/dev/null; true
  ```
  Esperado: con cada una de las doce mutaciones (una copia por mutación), su comprobación cae: `verificar-imagenes.sh` con `FALLA … User` (user-0), `FALLA … FROM` (sin-digest) o `FALLA … sin-sdk` (una-etapa), y salida 1; `verificar-front.py` con su `FALLA <regla>` y `front=1` (sin-root, escribible, token, url-api, ruta-raiz, sin-tls); `verificar-base.py` con `base=1` (base-sin-front: 11 en vez de 13); `--versiones` con `FALLA versiones` y `versiones=1` (tag-distinto, y en «primero» al tomar el front por backend o al revés, según el orden del render: la bitácora pega cuál sale primero y la mutación se escribe para que tome el otro). La bitácora pega cada mutación y la línea que cayó. La imagen `localhost:5001/synkra-front:0.1.0` buena no cambia (su digest de contenido, `docker image inspect -f '{{.Id}}'`, antes y después de CA-6 en la misma corrida, C-132). **Rojo de antes de implementar:** el primer commit de la rama trae **solo** `scripts/verificar-front.py`, los cambios de `verificar-base.py`, `verificar-backend.py` y `verificar-imagenes.sh`, y los guiones `ca3.sh`, `ca4.sh` y `ca5.sh`, sin Dockerfile ni manifiestos; en una copia de ese commit `ca5.sh` da `base=1`, `backend=1` y `front=1`, y `verificar-imagenes.sh` da `FALLA … existe` para el front (la bitácora pega su sha y su `git show --stat`).

- [ ] **CA-7**: U09-T08 en el clúster. `make install` deja `synkra-front` `Running` y servido en `https://synkra.local/` (se corre **al final**, cuando el orquestador avise)
  Guardar como `bitacoras/U09-D-evidencia/ca7.sh` y correrlo con `JAVA_HOME` exportado:
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml; R=127.0.0.1:5001
  CURL='curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt'
  m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
  make cluster-delete; echo "borrar=$?"
  time make install; echo "install=$?"
  kubectl --kubeconfig "$KC" -n synkra get deploy,svc,ingress -o wide
  P=$(kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-front -o jsonpath='{.items[0].metadata.name}')
  kubectl --kubeconfig "$KC" -n synkra get pod "$P" -o jsonpath='{.status.phase} {.status.containerStatuses[0].ready} {.spec.containers[0].image} {.spec.containers[0].imagePullPolicy} {.spec.automountServiceAccountToken} {.status.containerStatuses[0].imageID}'; echo
  kubectl --kubeconfig "$KC" -n synkra exec "$P" -- id -u
  curl -sf "http://$R/v2/synkra-front/tags/list"; echo
  $CURL -o /dev/null -w '%{http_code} %{ssl_verify_result}\n' https://synkra.local/
  $CURL https://synkra.local/ | grep -o -e '<h1>Vista general</h1>' -e 'data-pais="[a-z]*"' | sort -u
  $CURL https://synkra.local/api/paises | jq -c '[.[] | .pais]'
  $CURL https://synkra.local/actuator/health | jq -r .status
  $CURL -X POST -o /dev/null -w '%{http_code}\n' 'https://synkra.local/_blazor/negotiate?negotiateVersion=1'
  kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -c 'http://synkra-backend.synkra.svc:8080/api/paises'
  kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -ciE 'read-only file system|permission denied|unhandled exception'
  m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
  ```
  Esperado: `borrar=0`; `install=0` con el tiempo real, y en su salida cuatro líneas `subida …`, dos `OK desplegar: …` (backend y front, cada una con el digest de su `subida`) y `OK limite: 6 respuestas esperadas`; `synkra-front` `1/1` con su Service `8080/TCP` y el Ingress `synkra-front` en `synkra.local`; `Running true localhost:5001/synkra-front:0.1.0 Always false localhost:5001/synkra-front@sha256:…` con el digest de su `subida` (U09-T08: «`make install` deja `synkra-front` `Running`»); `10001`; la etiqueta `0.1.0` en el registro; `200 0` (TLS verificado sin `-k`); `<h1>Vista general</h1>`, `data-pais="co"` y `data-pais="pe"` (el prerender del front leyó `GET /api/paises` del backend por su Service, dentro del clúster); `["co","pe"]` (`/api` sigue en el backend); `UP`; `200` (la negociación de Blazor llega al front por el Ingress); `1` o más (el log de `HttpClient` del front muestra la llamada al Service del backend); `0`; `kubeconfig-global-intacto`. Ninguna orden nombra otro clúster. Además, con el mismo estilo de `bitacoras/U08-C-evidencia/ca5-variante.sh`, `bitacoras/U09-D-evidencia/ca7-variante.sh`: una variante de `localhost:5001/synkra-front:0.1.0` con una `LABEL` nueva, subida con la misma etiqueta; `make desplegar` hace `rollout restart` del front y el pod corre el digest nuevo (`corre-la-variante`); y `make imagenes desplegar` lo restaura (`corre-lo-subido`). En una copia con la mutación «desplegar-sin-front» (`desplegar.sh` solo compara el backend), `corre-la-variante` no sale. Al terminar, el clúster queda arriba para U09-E (decisión 10).

- [ ] **CA-8**: el diff queda acotado
  Guardar como `bitacoras/U09-D-evidencia/ca8.sh`; `ca8.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  B=$(git merge-base HEAD main)
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat "$B"...HEAD
  git diff --name-only "$B"...HEAD | grep -v -E '^(front/Dockerfile|front/\.dockerignore|front/Synkra\.Front\.csproj|deploy/base/front/(kustomization|deployment|service)\.yaml|deploy/base/kustomization\.yaml|deploy/overlays/kind/(kustomization|front-kind|ingress-front)\.yaml|scripts/(imagenes|verificar-imagenes|desplegar)\.sh|scripts/verificar-(base|backend|front)\.py|bitacoras/U09-D\.md|bitacoras/U09-D-evidencia/.*|revisiones/U09-D/.*)$'; echo "fuera=$?"
  git diff -U0 "$B"...HEAD -- front/Synkra.Front.csproj | grep -E '^[-+][^-+]'
  git diff --name-only "$B"...HEAD -- front/Api front/Components front/Program.cs front/appsettings.json front.Tests backend core init-descarga plugins deploy/base/backend deploy/base/namespaces.yaml deploy/base/rbac.yaml deploy/kind deploy/ingress-nginx deploy/overlays/kind/ingress.yaml deploy/overlays/kind/backend-kind.yaml Makefile | wc -l
  git ls-files .kube .tls | wc -l
  ```
  Esperado: `fuera=1`; una sola línea, `+    <Version>0.1.0</Version>`; `0` (nada del código del front, del backend ni de lo de U08 que esta tarea no nombra); `0`. El sha impreso es el del padre del commit de `ca8.txt`.

---

**Rojo primero:** el primer commit trae solo los verificadores y los guiones de CA-3 a CA-5 (CA-6, «Rojo de antes de implementar»); el `Dockerfile`, los manifiestos y los cambios de `imagenes.sh` y `desplegar.sh` van después. CA-6 demuestra con el código terminado que cada regla muerde. CA-1 a CA-6 no usan ningún clúster; CA-7 se corre al final en `kind-synkra`, con el kubeconfig del proyecto, cuando el orquestador lo diga.
