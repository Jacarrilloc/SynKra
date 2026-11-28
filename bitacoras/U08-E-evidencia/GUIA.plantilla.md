# Guía de SynKra: instalar y usar

SynKra despliega aplicaciones micro-kernel ya compiladas en Kubernetes. Subes un core y sus plugins, SynKra comprueba que la combinación sea compatible y solo entonces la aplica en el namespace del país (`synkra-co`, `synkra-pe`). Esta guía te lleva de una máquina sin SynKra a un recorrido completo: instalar, entrar, usar cada función y desinstalar.

Cómo leerla:

- Cada paso trae el comando y, debajo, lo que debes ver. Las salidas son copias literales de una corrida real hecha en una máquina x86_64 con Linux. Los números que cambian de una corrida a otra (ids de planes, tiempos, hashes de pods) en tu pantalla serán otros.
- Los comandos de cada sección dependen de los anteriores (usan variables de la terminal): ábrela una vez y ve en orden, sin cerrarla.
- Todo se corre desde la raíz del repositorio. En las salidas, la carpeta personal de la máquina de la corrida aparece escrita como `$HOME` (y el repositorio estaba en `$HOME/Projects/Master/wt-m6r-U08-E`); en tu pantalla verás tus propias rutas.
- Lo único que se instala es un clúster `kind` local llamado `synkra`. SynKra nunca toca tu `~/.kube/config` ni otros clústeres: usa su propio kubeconfig en `.kube/kind-synkra.yaml`.
- El front (Blazor) todavía no se despliega en `kind`: su manifiesto es la tarea U09-D, pendiente. Hoy se entra por la API REST. El script `scripts/dotnet-docker.sh` solo corre `dotnet` dentro de la imagen oficial del SDK (lo usan las pruebas del front, tarea U09-A); esta guía no documenta cómo arrancar el front porque ninguna tarea lo documenta todavía.

## Requisitos

Herramientas (en la máquina de la corrida estas eran las versiones; con otras cercanas también debería funcionar, pero solo se probó con estas):

<!-- ejecutable -->
```bash
export JAVA_HOME="${JAVA_HOME:-$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2}"
docker --version
kind version
kubectl version --client
make --version | head -1
jq --version
curl --version | head -1
python3 --version
"$JAVA_HOME/bin/java" -version 2>&1 | head -1
sysctl fs.inotify.max_user_instances
```
@@SALIDA@@

Qué necesita cada una:

- **Docker**, con permiso para tu usuario. Construye y guarda las imágenes y hace de «máquinas» de `kind`.
- **kind** y **kubectl**: el clúster y su cliente.
- **make**, **curl**, **jq** y **python3**: los comandos de esta guía y los guiones del repositorio.
- **Java 21** en `JAVA_HOME` (el repositorio compila con Gradle y construye las imágenes con él). Si no tienes un JDK 21, Gradle descarga uno solo al compilar (queda bajo `$HOME/.gradle/jdks/`); apunta `JAVA_HOME` a esa carpeta.
- **Límite de inotify.** Si en la máquina ya hay otros clústeres `kind`, `kind create cluster` puede fallar con «Too many open files» porque cada nodo gasta instancias de inotify. Con `fs.inotify.max_user_instances` en 512 o más funcionó, como se ve arriba. Para subirlo (necesita permisos de administrador; esta guía no lo hace por ti): `sudo sysctl fs.inotify.max_user_instances=512`.
- **Puertos libres** en `127.0.0.1`: `80` y `443` (el Ingress, por donde se entra) y `5001` (el registro local de imágenes). Si otro programa los usa, `make install` falla al crear el clúster o el registro.
- **Espacio y red.** La primera vez Docker descarga las imágenes base (`kindest/node`, Ingress NGINX, `registry`, Temurin y Alpine). En la corrida de esta guía ya estaban en caché, por eso los tiempos de abajo son los de una máquina que ya las tenía.

Para solo comprobar herramientas hay también `make doctor`, y `make setup` instala las que falten en Ubuntu, Debian o WSL2.

## Instalar

Si ya había un clúster `synkra` de una prueba anterior, bórralo primero (solo borra el clúster `synkra` y su registro `kind-registry`; ver «Desinstalar»):

<!-- ejecutable -->
```bash
make cluster-delete
```
@@SALIDA@@

Luego, una sola orden:

<!-- ejecutable -->
```bash
make install
```

Su salida completa (cientos de líneas, sobre todo de Gradle y Docker) está en `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/install.txt`. Estos son sus pasos, cada uno es un objetivo del `Makefile` que también puedes correr suelto, y la línea con la que `make install` anuncia cada uno:

| Paso | Qué hace | Línea con la que lo anuncia |
|---|---|---|
| guardia | Se niega a seguir si el kubeconfig apunta a otro clúster que no sea `kind-synkra` (por ejemplo, el tuyo de trabajo). | `contexto permitido: kind-synkra` |
| `make cluster` | Crea el clúster `kind` `synkra` (un nodo de control y un worker) y escribe su kubeconfig en `.kube/kind-synkra.yaml`. | `Creating cluster "synkra" ...` |
| `make registro` | Levanta el registro local `kind-registry` en `127.0.0.1:5001` y lo conecta a la red de `kind`. | `registro listo en 127.0.0.1:5001` |
| `make aplicar-base` | Crea los namespaces `synkra`, `synkra-co` y `synkra-pe` y los permisos (RBAC) de la cuenta `synkra:deployer`. | `namespace/synkra created` |
| `make ingress` | Instala Ingress NGINX (`deploy/ingress-nginx`) y espera a que esté listo. | `deployment "ingress-nginx-controller" successfully rolled out` |
| `make tls` | Genera un certificado autofirmado para `synkra.local` en `.tls/` y lo guarda como secreto `synkra/synkra-local-tls`. | `secret/synkra-local-tls created` |
| `make imagenes` | Compila los JAR con Gradle, construye las tres imágenes (backend, core de ejemplo, init de descarga), las comprueba y las sube al registro local. | `OK versiones: backend 0.1.0, init 1.0.0` |
| `make desplegar` | Aplica el backend de SynKra (`deploy/overlays/kind`) en el namespace `synkra` y comprueba que el pod corra el mismo digest que hay en el registro. | `deployment "synkra-backend" successfully rolled out` |
| `make verificar-limite` | Pregunta qué puede y qué no puede la cuenta de despliegue (ver la sección siguiente). | `OK limite: 6 respuestas esperadas` |

Tiempo de la corrida de esta guía: `make install` tardó 84 segundos (línea `segundos=` del final de `install.txt`) con las imágenes base ya descargadas. Termina con código 0 y la última línea es la del límite.

Compruébalo:

<!-- ejecutable -->
```bash
kubectl --kubeconfig .kube/kind-synkra.yaml get ns | grep synkra
kubectl --kubeconfig .kube/kind-synkra.yaml get pods -n synkra
docker ps --format '{{.Names}} {{.Ports}}' | grep -E 'synkra|kind-registry' | sed 's/ *$//'
```
@@SALIDA@@

El backend (`synkra-backend-...`) debe estar `Running` y `1/1`. `kind` publica los puertos 80 y 443 solo en `127.0.0.1`, y el registro el 5001.

Toda orden de esta guía que habla con Kubernetes lleva `--kubeconfig .kube/kind-synkra.yaml`. No uses `kubectl` sin él: apuntaría a tu configuración personal.

## Comprobar el límite

SynKra solo debe poder cambiar los namespaces de país. `make verificar-limite` lo pregunta a Kubernetes con `kubectl auth can-i`, como la cuenta `system:serviceaccount:synkra:deployer`, y no crea nada:

<!-- ejecutable -->
```bash
make verificar-limite
```
@@SALIDA@@

Las seis respuestas, de arriba abajo:

1. `create deployments.apps -n synkra-co: yes`: SynKra puede crear Deployments en Colombia.
2. `create deployments.apps -n synkra-pe: yes`: y en Perú.
3. `create deployments.apps -n default: no`: no en el namespace `default`.
4. `create deployments.apps -n kube-system: no`: ni en el del sistema.
5. `create deployments.apps -n synkra: no`: ni siquiera en su propio namespace: el backend de SynKra no puede modificarse a sí mismo.
6. `create namespaces: no`: no puede crear namespaces (los de país los crea `make install`, una vez).

La línea final es `OK limite: 6 respuestas esperadas` y `make` sale con 0. Si alguna respuesta cambia, la línea es `FALLA ...` y el script sale con 1; `make` lo reporta como código 2 (así lo hace GNU make cuando una receta falla): no leas el número, léelo como «distinto de 0».

## Entrar

La API está en `https://synkra.local`, detrás del Ingress. El certificado es autofirmado y `synkra.local` no existe en ningún DNS. Sin tocar nada del sistema, `curl` lo resuelve a mano y confía en el certificado que generó `make tls`:

<!-- ejecutable -->
```bash
curl -sS --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/actuator/health
```
@@SALIDA@@

`"status":"UP"` al principio de la respuesta significa que el backend responde (el resto detalla la base de datos, el disco y el TLS). Para el resto de la guía se define una función con esas opciones y el usuario de los ejemplos (la cabecera `X-Synkra-Usuario` solo marca quién crea y quién confirma; no es un control de acceso, y sin ella el usuario queda `anonimo`):

<!-- ejecutable -->
```bash
API=https://synkra.local
sk() { curl -sS --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt -H 'X-Synkra-Usuario: demo' "$@"; }
skj() { sk -H 'Content-Type: application/json' "$@"; }
sk "$API/actuator/health" | jq -c .
```
@@SALIDA@@

`sk` es `curl` con esas opciones; `skj` además dice que el cuerpo es JSON.

Si prefieres el navegador o Postman sin `--resolve`, agrega esta línea a `/etc/hosts` (necesita permisos de administrador; ningún comando de esta guía lo hace):

```
127.0.0.1 synkra.local
```

El navegador avisará del certificado autofirmado. Con la API no hay página: abre `https://synkra.local/actuator/health`. La ruta `/` del front aún no existe (pendiente de U09-D).

## Usar cada función

Los ejemplos usan el país `co` y el usuario `demo`. Cada función dice qué petición de la colección de Postman la hace (carpeta y nombre, en `postman/`; cómo importarla está en `postman/README.md`; para kind, `baseUrl` es `https://synkra.local`).

### Preparar los archivos de ejemplo

El core de ejemplo ya quedó en el registro por `make install`. Los plugins de ejemplo son dos proyectos Gradle que hay que empaquetar (la corrida usa tu `JAVA_HOME`):

<!-- ejecutable -->
```bash
./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble -q
ls plugins/kyc-co/build/libs plugins/kyc-pe/build/libs
```
@@SALIDA@@

Son `kyc-co` 1.0.0 (para el core `2.x`), `kyc-pe` 1.3.0 (core `2.x`) y `kyc-pe` 2.0.0 (core `3.x`). Cada JAR lleva su descriptor `synkra-plugin.yaml` con nombre, versión y el rango de core compatible.

### Subir un core

Un core se registra con su nombre, su versión y la imagen (con digest) que está en el registro local. Se lee la versión de `core/build.gradle.kts` y el digest del registro:

<!-- ejecutable -->
```bash
VC=$(grep -E '^version *= *"' core/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
ACEPTA='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
DIGEST=$(curl -sS -I -H "Accept: $ACEPTA" "http://127.0.0.1:5001/v2/synkra-core-ejemplo/manifests/$VC" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}')
IMAGEN="localhost:5001/synkra-core-ejemplo:$VC@$DIGEST"
echo "$IMAGEN"
```
@@SALIDA@@

Registrarlo (Postman: `2. Catálogo` > `Registrar core 2.1.0`):

<!-- ejecutable -->
```bash
jq -n --arg v "$VC" --arg i "$IMAGEN" '{nombre:"synkra-core", version:$v, imagen:$i}' \
  | skj -X POST "$API/api/cores" --data-binary @- -w '\nHTTP %{http_code}\n'
```
@@SALIDA@@

Si repites la misma versión, responde 409 `CONFLICTO` (Postman: `Core: versión repetida (409)`):

<!-- ejecutable -->
```bash
jq -n --arg v "$VC" --arg i "$IMAGEN" '{nombre:"synkra-core", version:$v, imagen:$i}' \
  | skj -X POST "$API/api/cores" --data-binary @- -w '\nHTTP %{http_code}\n'
```
@@SALIDA@@

### Subir plugins

Un plugin se sube como formulario: la parte `archivo` (el JAR) y, opcional, `etiquetas` (JSON). SynKra lee el descriptor del JAR, valida y guarda el archivo por su hash (Postman: `Subir plugin kyc-co 1.0.0 (con etiqueta cliente)`, `Subir plugin kyc-pe 1.3.0` y `Subir plugin kyc-pe 2.0.0`). La etiqueta `pais` la pone el propio plugin; `cliente` la agregas tú:

<!-- ejecutable -->
```bash
sk -X POST "$API/api/cores/synkra-core/plugins" \
  --form "archivo=@plugins/kyc-co/build/libs/kyc-co-1.0.0.jar;type=application/java-archive" \
  --form-string 'etiquetas={"cliente":"demo"}' -w '\nHTTP %{http_code}\n'
sk -X POST "$API/api/cores/synkra-core/plugins" \
  --form "archivo=@plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar;type=application/java-archive" -w '\nHTTP %{http_code}\n'
sk -X POST "$API/api/cores/synkra-core/plugins" \
  --form "archivo=@plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar;type=application/java-archive" -w '\nHTTP %{http_code}\n'
```
@@SALIDA@@

Cada respuesta es 201 con los datos del plugin (`coreCompatible`, `sha256`, `tamano`, `etiquetas`). Subir el mismo plugin y versión otra vez da 409 `CONFLICTO`.

### Ver el catálogo

Cores y sus versiones (Postman: `Listar cores`, `Ver core`):

<!-- ejecutable -->
```bash
sk "$API/api/cores" | jq -c .
```
@@SALIDA@@

Plugins de un core, filtrados por etiquetas (Postman: `Listar plugins por país`, `Listar plugins por país y cliente`):

<!-- ejecutable -->
```bash
sk "$API/api/cores/synkra-core/plugins?pais=co" | jq -c .
sk "$API/api/cores/synkra-core/plugins?pais=pe" | jq -c '.[] | {nombre, version, coreCompatible}'
```
@@SALIDA@@

Un plugin concreto (Postman: `Ver plugin`) y su archivo por hash (Postman: `Descargar artefacto por hash`); el hash descargado debe ser el del JAR local:

<!-- ejecutable -->
```bash
sk "$API/api/cores/synkra-core/plugins/kyc-pe/1.3.0" | jq -c .
SHA=$(sk "$API/api/cores/synkra-core/plugins/kyc-pe/1.3.0" | jq -r .sha256)
sk "$API/api/artefactos/$SHA" -o /tmp/kyc-pe-descargado.jar
sha256sum /tmp/kyc-pe-descargado.jar plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar | cut -c1-64
rm -f /tmp/kyc-pe-descargado.jar
```
@@SALIDA@@

### Verificar una combinación

Pregunta «¿este core y estos plugins son compatibles?» sin crear nada. Responde 200 tanto si la aprueba como si la rechaza; el campo `global` dice `APROBADA` o `RECHAZADA` (Postman: `3. Verificación` > `Verificar combinación compatible`):

<!-- ejecutable -->
```bash
skj -X POST "$API/api/verificaciones" \
  -d "{\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\"},{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]}" | jq -c '{global, plugins: [.plugins[] | {nombre, version, compatible}]}'
```
@@SALIDA@@

Una combinación incompatible, `kyc-pe` 2.0.0 (pide core `3.x`) sobre el core 2.1.0, se rechaza y trae el motivo y alternativas (Postman: `Verificar combinación incompatible`):

<!-- ejecutable -->
```bash
skj -X POST "$API/api/verificaciones" \
  -d "{\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[{\"nombre\":\"kyc-pe\",\"version\":\"2.0.0\"}]}" | jq .
```
@@SALIDA@@

### Crear un plan y ver su resumen y sus cambios

Un plan es la propuesta de qué quedará en el país: SynKra verifica la combinación (la misma verificación de arriba) y, si la aprueba, guarda un plan `pendiente`. **Todavía no toca el clúster.** El plan trae el `resumen` de lo que se aplicaría y los `cambios` frente a lo que corre hoy en el país (Postman: `4. Despliegue` > `Crear plan (kyc-pe 1.3.0 sobre core 2.1.0)`; el ejemplo de aquí es `kyc-co` 1.0.0):

<!-- ejecutable -->
```bash
skj -X POST "$API/api/despliegues" \
  -d "{\"pais\":\"co\",\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\"}]}" > /tmp/plan1.json
PLAN1=$(jq -r .id /tmp/plan1.json)
jq 'del(.veredicto)' /tmp/plan1.json
```
@@SALIDA@@

`cambios.core` compara la imagen de antes y de después; `anadidos`, `quitados` e `iguales` son los plugins; en un país vacío todos van en `anadidos`. El plan vence a los 30 minutos (`venceEn`).

### Confirmar

Confirmar es lo que sí despliega: SynKra vuelve a verificar, construye los objetos de Kubernetes y los aplica en `synkra-co`. Responde 202 `aplicando` enseguida y sigue en segundo plano (Postman: `Confirmar plan`). Un país tiene un solo despliegue `aplicando` a la vez:

<!-- ejecutable -->
```bash
sk -X POST "$API/api/despliegues/$PLAN1/confirmacion" -w '\nHTTP %{http_code}\n'
```
@@SALIDA@@

Para esperar a que termine (Postman: `Esperar a que el despliegue quede listo`), pide el detalle hasta que el estado deje de ser `aplicando`:

<!-- ejecutable -->
```bash
esperar() { # esperar <id>: sondea cada 3 s, hasta 300 s, hasta listo o fallido
  local t0=$SECONDS e
  while :; do
    e=$(sk "$API/api/despliegues/$1" | jq -r .estado)
    case "$e" in
      listo | fallido) echo "despliegue $1: $e"; return 0 ;;
    esac
    [ $((SECONDS - t0)) -lt 300 ] || { echo "ERROR: el despliegue $1 sigue en $e"; return 1; }
    sleep 3
  done
}
esperar "$PLAN1"
kubectl --kubeconfig .kube/kind-synkra.yaml get deploy,pods -n synkra-co
```
@@SALIDA@@

El id del despliegue es el mismo que el del plan. Los pods del país están en el namespace `synkra-co`; el core de ejemplo carga sus plugins desde un ConfigMap.

### La vista general, el historial y el detalle

La vista general dice, por país, qué corre ahora (leído del clúster) y si coincide con el último despliegue registrado (Postman: `5. Registro` > `Vista general por país`). Para `co` debe decir `"concuerda": true`:

<!-- ejecutable -->
```bash
sk "$API/api/paises" | jq -c '.[] | {pais, lectura, concuerda, ultimoDespliegueId, versionCore, plugins: [.plugins[] | "\(.nombre) \(.version)"]}'
```
@@SALIDA@@

`lectura` es `leido` si el país tiene algo corriendo y `vacio` si no hay nada (como `pe` aquí). El historial de un país, del más nuevo al más viejo (Postman: `Historial del país`), y el detalle de un despliegue con su veredicto y su contexto (Postman: `Detalle de un despliegue`):

<!-- ejecutable -->
```bash
sk "$API/api/paises/co/despliegues" | jq -c '.[] | {id, tipo, estado, creadoPor, confirmadoPor}'
sk "$API/api/despliegues/$PLAN1" | jq '{id, pais, tipo, estado, confirmadoPor, plugins: [.plugins[] | .nombre + " " + .version], contexto}'
```
@@SALIDA@@

### Cambiar plugins

Cambiar la lista de plugins es otro plan: aquí se añade `kyc-pe` 1.3.0 a lo que ya corre. Los `cambios` deben decir que solo se añade ese plugin y que `kyc-co` queda igual. Al confirmarlo, SynKra renueva el pod del core para que cargue los plugins nuevos:

<!-- ejecutable -->
```bash
skj -X POST "$API/api/despliegues" \
  -d "{\"pais\":\"co\",\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\"},{\"nombre\":\"kyc-pe\",\"version\":\"1.3.0\"}]}" > /tmp/plan2.json
PLAN2=$(jq -r .id /tmp/plan2.json)
jq -c '{id, estado, core: .cambios.core.igual, anadidos: [.cambios.anadidos[] | .nombre + " " + .version], quitados: [.cambios.quitados[] | .nombre + " " + .version], iguales: [.cambios.iguales[] | .nombre + " " + .version]}' /tmp/plan2.json
sk -X POST "$API/api/despliegues/$PLAN2/confirmacion" -o /dev/null -w 'HTTP %{http_code}\n'
esperar "$PLAN2"
sk "$API/api/paises" | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId, plugins: [.plugins[] | "\(.nombre) \(.version)"]}'
```
@@SALIDA@@

### Una combinación incompatible rechazada

Si pides un plan con una combinación que no es compatible, SynKra responde 422 `COMBINACION_RECHAZADA` y **no guarda nada ni toca el clúster** (Postman: `Despliegue: casos negativos` > `Plan con plugin incompatible (422)`). Antes y después, la imagen del Deployment y el historial son los mismos:

<!-- ejecutable -->
```bash
IMG_ANTES=$(kubectl --kubeconfig .kube/kind-synkra.yaml get deploy -n synkra-co -o jsonpath='{..image}' | tr ' ' '\n' | sort | paste -sd' ' -)
sk "$API/api/paises/co/despliegues" | jq 'length'
skj -X POST "$API/api/despliegues" \
  -d "{\"pais\":\"co\",\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[{\"nombre\":\"kyc-co\",\"version\":\"1.0.0\"},{\"nombre\":\"kyc-pe\",\"version\":\"2.0.0\"}]}" -w '\nHTTP %{http_code}\n'
sk "$API/api/paises/co/despliegues" | jq 'length'
IMG_DESPUES=$(kubectl --kubeconfig .kube/kind-synkra.yaml get deploy -n synkra-co -o jsonpath='{..image}' | tr ' ' '\n' | sort | paste -sd' ' -)
[ "$IMG_ANTES" = "$IMG_DESPUES" ] && echo "las imágenes del namespace no cambiaron"
```
@@SALIDA@@

### Un fallo y la vuelta atrás

Hay combinaciones que el verificador acepta y que Kubernetes no logra poner en marcha. Para provocar un fallo real se registra el core `3.0.0` apuntando, a propósito, a la misma imagen del core 2.x (que no sabe cargar plugins de core 3.x) y se pide `kyc-pe` 2.0.0 sobre él. SynKra lo acepta (por catálogo es compatible), pero el pod nuevo se cae y el despliegue termina `fallido`. Esto tarda unos 3 minutos: SynKra espera el plazo del despliegue antes de darlo por fallido. El pod anterior sigue sirviendo mientras tanto:

<!-- ejecutable -->
```bash
jq -n --arg i "$IMAGEN" '{nombre:"synkra-core", version:"3.0.0", imagen:$i}' | skj -X POST "$API/api/cores" --data-binary @- -o /dev/null -w 'core 3.0.0: HTTP %{http_code}\n'
skj -X POST "$API/api/despliegues" \
  -d '{"pais":"co","core":"synkra-core","versionCore":"3.0.0","plugins":[{"nombre":"kyc-pe","version":"2.0.0"}]}' > /tmp/plan3.json
PLAN3=$(jq -r .id /tmp/plan3.json)
sk -X POST "$API/api/despliegues/$PLAN3/confirmacion" -o /dev/null -w 'HTTP %{http_code}\n'
esperar "$PLAN3"
sk "$API/api/despliegues/$PLAN3" | jq '{id, estado, terminadoEn, aviso: (.contexto.eventos | split("\n") | .[0]), logCore: (.contexto.logCore | split("\n") | map(select(contains("kyc-pe"))) | .[0:2])}'
sk "$API/api/paises" | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId}'
```
@@SALIDA@@

El `contexto` guarda lo que hace falta para entender el fallo: los eventos de Kubernetes (`eventos`), el log del core (`logCore`) y el del init de descarga (`logInit`). Aquí el core se negó a arrancar por `kyc-pe`. `concuerda: false` avisa que lo que corre ya no es lo del último despliegue.

Para volver atrás se pide la reversión de un despliegue que quedó `listo`: el segundo, con `kyc-co` y `kyc-pe` 1.3.0 (Postman: `Revertir al despliegue 1`). La reversión es un plan nuevo, de tipo `reversion`, y también se confirma (Postman: `Confirmar la reversión`):

<!-- ejecutable -->
```bash
sk -X POST "$API/api/despliegues/$PLAN2/reversion" > /tmp/plan4.json
PLAN4=$(jq -r .id /tmp/plan4.json)
jq -c '{id, tipo, origenId, estado}' /tmp/plan4.json
sk -X POST "$API/api/despliegues/$PLAN4/confirmacion" -o /dev/null -w 'HTTP %{http_code}\n'
esperar "$PLAN4"
sk "$API/api/paises" | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId, plugins: [.plugins[] | "\(.nombre) \(.version)"]}'
sk "$API/api/paises/co/despliegues" | jq -c '.[] | {id, tipo, estado, origenId}'
kubectl --kubeconfig .kube/kind-synkra.yaml get pods -n synkra-co
```
@@SALIDA@@

El historial termina con la reversión `listo`, el despliegue `fallido` y los anteriores. Los pods del core quedan `Running` y sin caídas, con los plugins del segundo despliegue.

## El recorrido completo

`./scripts/e2e-api.sh` hace de una vez todo lo anterior y además comprueba cada resultado leyendo la API y Kubernetes. Va **después** de `make install` y de empaquetar los plugins (arriba). No lo corre esta guía (dura unos 4 minutos, la mayoría esperando el fallo de CU5); su salida completa está en `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt`:

```bash
./scripts/e2e-api.sh
```

Qué esperar, por caso de uso (los tiempos son de la corrida real):

- **CU1, subir core y plugins.** Registra los cores 2.1.0 y 3.0.0, sube los tres plugins y comprueba el catálogo. Provoca tres respuestas 4xx a propósito. Tardó @@TIEMPO CU1@@.
- **CU2, planear y confirmar en `synkra-co`.** Crea el plan, lo confirma y espera `listo`. Después lee el Deployment, el ConfigMap y lo que el core cargó. Tardó @@TIEMPO CU2@@.
- **CU3, cambiar plugins.** Añade un plugin y comprueba que el pod se renueva y que el core carga los dos. Tardó @@TIEMPO CU3@@.
- **CU4, combinación incompatible rechazada.** Pide una combinación mala y comprueba que nada llegó al clúster: mismas imágenes, mismo `resourceVersion`, mismo historial. Tardó @@TIEMPO CU4@@.
- **CU5, fallo y vuelta atrás.** Provoca un fallo real, comprueba que el pod viejo sigue sirviendo, revierte y comprueba que todo vuelve. Es el caso largo: @@TIEMPO CU5@@.

Termina con esta línea (y código 0); si algo falla, imprime `FALLA <caso>: <qué>` y sale con 1:

@@LINEA OK e2e@@

El guion provoca 10 respuestas 4xx y comprueba que todas traen `codigo`, `mensaje` y `detalles`. Se puede correr otra vez sobre la misma instalación.

## Errores comunes

Todo error de la API tiene el mismo cuerpo: `codigo` (una palabra fija, para programas), `mensaje` (para personas) y `detalles` (una lista de `campo` y `motivo`, que puede estar vacía). El código HTTP dice la familia:

| HTTP | `codigo` | Cuándo pasa |
|---|---|---|
| 404 | `NO_ENCONTRADO` | El core, el plugin, el artefacto o el despliegue no existe. También un plan que sigue `pendiente` no tiene detalle de despliegue: su id solo se usa para confirmarlo. |
| 409 | `CONFLICTO` | Ya existe: el mismo core o la misma versión de plugin. |
| 409 | `PLAN_NO_PENDIENTE` | Confirmaste dos veces el mismo plan. |
| 409 | `DESPLIEGUE_EN_CURSO` | Ese país ya tiene un despliegue `aplicando`; espera a que termine. |
| 422 | `ENTIDAD_NO_PROCESABLE` | El cuerpo no es válido: `detalles` dice qué campos (nombre, versión, imagen...). |
| 422 | `COMBINACION_RECHAZADA` | El verificador no aprueba el core con esos plugins; `detalles` da el motivo. |
| 422 | `PAIS_NO_PERMITIDO` | El país no está en la lista de SynKra (por defecto `co` y `pe`). |
| 422 | `USUARIO_INVALIDO` | `X-Synkra-Usuario` con caracteres no permitidos. |
| 422 | `ORIGEN_NO_LISTO` | Pediste revertir un despliegue que no quedó `listo`. |
| 503 | `KUBERNETES_NO_DISPONIBLE` | El backend no alcanza el clúster; no ocurre con `kind` sano. |

Tres ejemplos reales, con su cuerpo (el 503 de la tabla viene de `postman/README.md` y no se provoca aquí):

<!-- ejecutable -->
```bash
sk "$API/api/cores/no-existe" -w '\nHTTP %{http_code}\n'
sk -X POST "$API/api/despliegues" -H 'Content-Type: application/json' -H 'X-Synkra-Usuario: No Valido' \
  -d "{\"pais\":\"co\",\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[]}" -w '\nHTTP %{http_code}\n'
skj -X POST "$API/api/despliegues" \
  -d "{\"pais\":\"mx\",\"core\":\"synkra-core\",\"versionCore\":\"$VC\",\"plugins\":[]}" -w '\nHTTP %{http_code}\n'
```
@@SALIDA@@

Los códigos `CONFLICTO`, `ENTIDAD_NO_PROCESABLE`, `PLAN_NO_PENDIENTE`, `DESPLIEGUE_EN_CURSO`, `ORIGEN_NO_LISTO` y `NO_ENCONTRADO` (de una reversión) salen también en `e2e.txt`, en las líneas que empiezan por `4xx`:

@@LINEAS 4xx@@

Otros problemas de instalación:

- **`contexto no permitido`**: el kubeconfig apunta a otro clúster. SynKra se niega a seguir; usa `--kubeconfig .kube/kind-synkra.yaml`, nunca tu configuración personal.
- **`kind create cluster` falla con «Too many open files»**: sube `fs.inotify.max_user_instances` (ver Requisitos).
- **`FALLA entorno: JAVA_HOME ... no es Java 21`**: apunta `JAVA_HOME` a un JDK 21.
- **`curl: (6) Could not resolve host: synkra.local`**: te faltó `--resolve synkra.local:443:127.0.0.1` (o la línea de `/etc/hosts`).
- **`curl: (60) SSL certificate problem`**: te faltó `--cacert .tls/tls.crt`, o cambió el certificado (se regenera en `.tls/` si borras la carpeta).

## Desinstalar

```bash
make cluster-delete
```

Borra el clúster `kind` `synkra` y el contenedor `kind-registry` (solo si lo creó SynKra: lleva una etiqueta que `make registro` le pone; si es de otro, avisa y no lo toca). No toca `~/.kube/config`, otros clústeres `kind` ni las imágenes del Docker local. Esto es lo que imprimió en la corrida real:

@@BORRAR@@

Con la instalación borrada se pierde también la base de datos de SynKra (vive en un volumen del clúster). Los archivos `.kube/` y `.tls/` del repositorio se quedan; `make install` los reutiliza o los regenera.
