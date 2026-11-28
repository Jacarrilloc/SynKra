#!/usr/bin/env bash
# U08-T10 y U08-T11: recorre CU1 a CU5 por la API de SynKra (https://synkra.local) y lee cada paso de vuelta con la API
# y con la API de Kubernetes de kind-synkra. Cada respuesta 4xx tiene que traer codigo, mensaje y detalles.
# Uso: bash scripts/e2e-api.sh   (desde cualquier sitio; antes: make install y ./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble)
# Entorno: KUBECONFIG_SYNKRA (por defecto <raiz>/.kube/kind-synkra.yaml), E2E_ESPERA (segundos por espera, 300).
# Se puede correr otra vez sobre la misma instalacion: cada paso se compara con lo que habia antes, no con numeros fijos.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KC="${KUBECONFIG_SYNKRA:-$RAIZ/.kube/kind-synkra.yaml}"
E2E_ESPERA="${E2E_ESPERA:-300}"
NS=synkra-co
PAIS=co
REGISTRO_HOST=127.0.0.1:5001
REGISTRO_IMAGEN=localhost:5001
REPO_CORE=synkra-core-ejemplo
CORE=synkra-core
VERSION_MENTIROSA=3.0.0
ACEPTA='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'

TMP=""
CASO=preparacion
ESTADO=""
CUERPO=""
SALIDA=""
METODO=""
RUTA=""
NPET=0
N4=0

limpiar() { [ -z "$TMP" ] || rm -rf "$TMP"; }
# El trap se instala antes de crear nada; las senales terminan el script (y disparan EXIT).
trap limpiar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

falla() { echo "FALLA $1: $2" >&2; exit 1; }
ok() { echo "ok $CASO $*"; }
kc() { kubectl --kubeconfig "$KC" --request-timeout=60s "$@"; }

# ejecutar <descripcion> <orden...>: deja la salida estandar en SALIDA; si la orden falla, FALLA con su codigo.
ejecutar() {
  local desc="$1" rc=0
  shift
  SALIDA="$("$@" 2>"$TMP/err")" || rc=$?
  [ "$rc" -eq 0 ] || falla "$CASO" "$desc salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"
}

# ---------------------------------------------------------------- preparacion (sin tocar ni la API ni Kubernetes)
for h in curl jq python3 sha256sum kubectl; do
  command -v "$h" >/dev/null 2>&1 || falla preparacion "falta $h"
done
TMP="$(mktemp -d)"

JAR_CO=plugins/kyc-co/build/libs/kyc-co-1.0.0.jar
JAR_PE13=plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar
JAR_PE20=plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar
for j in "$JAR_CO" "$JAR_PE13" "$JAR_PE20"; do
  [ -f "$RAIZ/$j" ] || falla preparacion "falta $j (corre ./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble)"
done

# Del descriptor de cada jar se leen nombre, version, core y coreCompatible: el guion asume estos valores y los comprueba.
descriptor() { # descriptor <jar> <nombre> <version> <core> <coreCompatible>
  local jar="$1" campo valor esperado i=0 rc=0 salida
  salida="$(python3 -I - "$RAIZ/$jar" <<'PY' 2>"$TMP/err"
import sys, zipfile, yaml
with zipfile.ZipFile(sys.argv[1]) as z:
    d = yaml.safe_load(z.read("synkra-plugin.yaml"))
for campo in ("nombre", "version", "core", "coreCompatible"):
    print(d.get(campo))
PY
)" || rc=$?
  [ "$rc" -eq 0 ] || falla preparacion "python3 leyendo synkra-plugin.yaml de $jar salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"
  shift
  for campo in nombre version core coreCompatible; do
    i=$((i + 1))
    valor="$(printf '%s\n' "$salida" | sed -n "${i}p")"
    esperado="$1"
    shift
    [ "$valor" = "$esperado" ] || falla preparacion "$jar dice $campo $valor (el guion asume $esperado)"
  done
}
descriptor "$JAR_CO" kyc-co 1.0.0 "$CORE" 2.x
descriptor "$JAR_PE13" kyc-pe 1.3.0 "$CORE" 2.x
descriptor "$JAR_PE20" kyc-pe 2.0.0 "$CORE" 3.x
PLUGIN_CO="kyc-co 1.0.0"
PLUGIN_PE13="kyc-pe 1.3.0"
PLUGIN_PE20="kyc-pe 2.0.0"

# El core: version compilada en core/build.gradle.kts (tiene que caer en 2.x) y la mentirosa de CU5 (3.x, distinta).
VC="$(grep -E '^version *= *"' "$RAIZ/core/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
case "$VC" in
  2.*) ;;
  *) falla preparacion "la versión del core en core/build.gradle.kts es '$VC' y no cae en 2.x" ;;
esac
[ "$VERSION_MENTIROSA" != "$VC" ] || falla preparacion "la versión mentirosa $VERSION_MENTIROSA es la del core"
case "$VERSION_MENTIROSA" in
  3.*) ;;
  *) falla preparacion "la versión mentirosa $VERSION_MENTIROSA no cae en el coreCompatible 3.x de kyc-pe 2.0.0" ;;
esac

# La guardia de contexto (AUTONOMIA-01): lo primero que se hace con el cluster.
rc=0
KUBECONFIG_SYNKRA="$KC" "$RAIZ/scripts/guardia-contexto.sh" >/dev/null 2>"$TMP/err" || rc=$?
[ "$rc" -eq 0 ] || falla guardia "scripts/guardia-contexto.sh salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"

# ---------------------------------------------------------------- la API
# pedir <metodo> <ruta> [json <archivo> | <argumentos de curl...>]: deja ESTADO, CUERPO (archivo), METODO y RUTA.
# USUARIO_PETICION cambia la cabecera X-Synkra-Usuario solo para esa peticion; CURL_TOLERA=1 deja ESTADO=000 si curl falla.
# Toda respuesta 4xx pasa por formato_error (U08-T11).
pedir() {
  local rc=0
  local -a extra=()
  METODO="$1"
  RUTA="$2"
  shift 2
  if [ "${1:-}" = json ]; then
    extra=(-H 'Content-Type: application/json' --data-binary "@$2")
  else
    extra=("$@")
  fi
  NPET=$((NPET + 1))
  CUERPO="$TMP/cuerpo.$NPET"
  ESTADO="$(curl -sS --max-time 60 --resolve synkra.local:443:127.0.0.1 --cacert "$RAIZ/.tls/tls.crt" \
    -H "X-Synkra-Usuario: ${USUARIO_PETICION:-e2e}" -X "$METODO" "${extra[@]}" \
    -o "$CUERPO" -w '%{http_code}' "https://synkra.local$RUTA" 2>"$TMP/err")" || rc=$?
  if [ "$rc" -ne 0 ]; then
    [ -z "${CURL_TOLERA:-}" ] || { ESTADO=000; return 0; }
    falla "$CASO" "curl $METODO $RUTA salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"
  fi
  case "$ESTADO" in
    4??) formato_error ;;
  esac
}

formato_error() {
  jq -e 'has("codigo") and has("mensaje") and has("detalles") and (.codigo|type)=="string" and (.mensaje|type)=="string" and (.detalles|type)=="array"' \
    "$CUERPO" >/dev/null 2>&1 || falla formato "$METODO $RUTA $ESTADO"
}

# espero <estados aceptados separados por |> [codigo]: compara el estado (y el codigo de error) con lo esperado.
espero() {
  local e bien=0 cod=""
  for e in ${1//|/ }; do
    [ "$ESTADO" != "$e" ] || bien=1
  done
  cod="$(jq -r '.codigo? // empty' "$CUERPO" 2>/dev/null || true)"
  if [ "$bien" -eq 1 ] && [ -n "${2:-}" ] && [ "$cod" != "$2" ]; then bien=0; fi
  [ "$bien" -eq 1 ] && return 0
  falla "$CASO" "$METODO $RUTA dio $ESTADO ${cod:-sin codigo} (esperado $1${2:+ $2}): $(head -c 300 "$CUERPO" | tr '\n' ' ')"
}

# provocada <n>: la 4xx n de la tabla de U08-T11, en su orden; imprime su linea y la cuenta.
provocada() {
  [ "$1" -eq $((N4 + 1)) ] || falla "$CASO" "la 4xx $1 llegó cuando iba la $((N4 + 1))"
  N4=$((N4 + 1))
  echo "4xx $ESTADO $(jq -r .codigo "$CUERPO") $METODO $RUTA"
}

# jq sobre el cuerpo de la ultima respuesta. En las dos, el filtro es el ULTIMO argumento y lo anterior son opciones de jq.
# jq_c [opciones] <filtro>: deja el resultado en SALIDA.
jq_c() {
  local -a a=("$@")
  local f="${a[-1]}"
  unset 'a[-1]'
  ejecutar "jq $f" jq -r "${a[@]}" "$f" "$CUERPO"
}

# jq_exige <comprobacion> [opciones] <filtro>: el filtro tiene que dar true sobre el cuerpo.
jq_exige() {
  local desc="$1" rc=0
  shift
  local -a a=("$@")
  local f="${a[-1]}"
  unset 'a[-1]'
  jq -e "${a[@]}" "$f" "$CUERPO" >/dev/null 2>"$TMP/err" || rc=$?
  case "$rc" in
    0) ok "$desc" ;;
    1 | 4) falla "$CASO" "$desc: $(jq -c . "$CUERPO" 2>/dev/null | head -c 500)" ;;
    *) falla "$CASO" "jq $f salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')" ;;
  esac
}

# igual <comprobacion> <obtenido> <esperado>
igual() {
  [ "$2" = "$3" ] || falla "$CASO" "$1: obtenido '$2', esperado '$3'"
  ok "$1"
}

# cuerpo_plan <archivo> <pais> <versionCore> [plugin "nombre version"...]: escribe el cuerpo de un plan.
cuerpo_plan() { # <archivo> <pais> <versionCore> [plugin "nombre version"...]
  local f="$1" p="$2" v="$3" rc=0
  shift 3
  jq -n --arg pais "$p" --arg core "$CORE" --arg v "$v" \
    '{pais:$pais, core:$core, versionCore:$v, plugins:[$ARGS.positional[] | split(" ") | {nombre:.[0], version:.[1]}]}' \
    --args "$@" >"$f" 2>"$TMP/err" || rc=$?
  [ "$rc" -eq 0 ] || falla "$CASO" "jq armando el plan salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"
}

# csv_api: de un arreglo de {nombre, version} del cuerpo a "kyc-co 1.0.0,kyc-pe 1.3.0" (ordenado).
CSV_FILTRO='[.[] | "\(.nombre) \(.version)"] | sort | join(",")'

# esperar_estado <id> <estado>: sondea el detalle cada 3 s hasta E2E_ESPERA segundos; deja ESPERA_S.
ESPERA_S=0
esperar_estado() {
  local id="$1" quiero="$2" t0=$SECONDS est=""
  while :; do
    pedir GET "/api/despliegues/$id"
    espero 200
    est="$(jq -r .estado "$CUERPO")"
    [ "$est" != "$quiero" ] || break
    if [ "$est" != aplicando ] || [ $((SECONDS - t0)) -ge "$E2E_ESPERA" ]; then
      falla "$CASO" "el despliegue $id quedó en $est (esperado $quiero), contexto: $(jq -c .contexto "$CUERPO")"
    fi
    sleep 3
  done
  ESPERA_S=$((SECONDS - t0))
  ok "el despliegue $id llegó a $quiero en ${ESPERA_S}s"
}

# confirmar <id>: 202 aplicando, confirmado por e2e.
confirmar() {
  pedir POST "/api/despliegues/$1/confirmacion"
  espero 202
  jq_exige "confirmación de $1: aplicando y confirmadoPor e2e" '.estado=="aplicando" and .confirmadoPor=="e2e"'
}

# Lecturas de Kubernetes. Todas con kc (kubectl con el kubeconfig del proyecto).
pods_core() { # nombres de los pods del core que no se estan borrando, "a,b"
  ejecutar "listar pods" kc get pods -n "$NS" -l app.kubernetes.io/name=synkra-core -o json
  printf '%s' "$SALIDA" > "$TMP/pods.json"
  ejecutar "jq pods" jq -r '[.items[] | select(.metadata.deletionTimestamp==null) | .metadata.name] | sort | join(",")' "$TMP/pods.json"
}
# kubectl recorre el mapa del pod en orden cambiante (a veces initContainers antes de containers): se ordenan las imagenes.
imagenes_ns() {
  ejecutar "imagenes del namespace" kc get deploy -n "$NS" -o jsonpath='{..image}'
  SALIDA="$(printf '%s' "$SALIDA" | tr ' ' '\n' | sort | paste -sd' ' -)"
}
imagen_core() { ejecutar "imagen del contenedor core" kc get deploy synkra-core -n "$NS" -o jsonpath='{.spec.template.spec.containers[?(@.name=="core")].image}'; }
anotacion() { ejecutar "anotacion de la plantilla" kc get deploy synkra-core -n "$NS" -o jsonpath='{.spec.template.metadata.annotations.synkra\.io/config-sha256}'; }
rollout() { ejecutar "rollout status" kc rollout status deployment/synkra-core -n "$NS" --timeout=300s; }
version_rv() { ejecutar "resourceVersion de $1" kc get "$1" -n "$NS" -o jsonpath='{.metadata.resourceVersion}'; }
cm_plugins() { # lo que lista el ConfigMap, "a 1,b 2"
  ejecutar "leer el ConfigMap" kc get configmap synkra-core-config -n "$NS" -o json
  printf '%s' "$SALIDA" > "$TMP/cm.json"
  ejecutar "jq ConfigMap" jq -r '.data["plugins-activos.yaml"]' "$TMP/cm.json"
  printf '%s' "$SALIDA" > "$TMP/activos.yaml"
  ejecutar "yaml del ConfigMap" python3 -I -c 'import sys, yaml
for p in sorted(yaml.safe_load(open(sys.argv[1])) or [], key=lambda p: (p["nombre"], p["version"])):
    print("%s %s" % (p["nombre"], p["version"]))' "$TMP/activos.yaml"
  SALIDA="$(printf '%s' "$SALIDA" | paste -sd, -)"
}
core_cargo() { # lo que el core cargo, por el proxy de la API de Kubernetes: "a 1,b 2"
  local intento rc
  for intento in 1 2 3; do
    rc=0
    SALIDA="$(kc get --raw "/api/v1/namespaces/$NS/services/synkra-core:http/proxy/plugins" 2>"$TMP/err")" || rc=$?
    [ "$rc" -ne 0 ] || break
    [ "$intento" -lt 3 ] || falla "$CASO" "leer /plugins del core por el proxy salió con $rc: $(head -c 300 "$TMP/err" | tr '\n' ' ')"
    sleep 2
  done
  printf '%s' "$SALIDA" > "$TMP/cargo.json"
  ejecutar "jq /plugins del core" jq -r "$CSV_FILTRO" "$TMP/cargo.json"
}
managed_apply() { # managed_apply <tipo/nombre>: manager synkra con operation Apply (C-75)
  ejecutar "managedFields de $1" kc get "$1" -n "$NS" -o jsonpath='{range .metadata.managedFields[*]}{.manager} {.operation}{"\n"}{end}'
  case "$SALIDA" in
    *"synkra Apply"*) ok "managedFields de $1: synkra Apply" ;;
    *) falla "$CASO" "managedFields de $1 sin 'synkra Apply': $SALIDA" ;;
  esac
}
# El estado de co que la API ve (GET /api/paises): deja ANTES_IMG y ANTES_CSV.
leer_pais() {
  pedir GET /api/paises
  espero 200
  jq_c --arg p "$PAIS" '.[] | select(.pais==$p) | .imagen // ""'
  ANTES_IMG="$SALIDA"
  jq_c --arg p "$PAIS" ".[] | select(.pais==\$p) | .plugins | $CSV_FILTRO"
  ANTES_CSV="$SALIDA"
}
pais_concuerda() { # pais_concuerda <true|false> <ultimoDespliegueId>
  pedir GET /api/paises
  espero 200
  jq_exige "GET /api/paises: $PAIS concuerda $1 con ultimoDespliegueId $2" --arg p "$PAIS" --argjson c "$1" --argjson u "$2" \
    '.[] | select(.pais==$p) | .concuerda==$c and .ultimoDespliegueId==$u'
}
largo_historial() {
  pedir GET "/api/paises/$PAIS/despliegues"
  espero 200
  jq_c 'length'
}
verifica_cambios() { # verifica_cambios <antes csv> <despues csv> <igual del core true|false>: lo que dice el plan creado
  jq_exige "cambios: añade, quita y conserva lo que corresponde frente a lo que corría" --arg a "$1" --arg d "$2" --argjson ic "$3" \
    'def s: split(",") | map(select(length > 0)) | sort;
     def n: [.[] | "\(.nombre) \(.version)"] | sort;
     ($a | s) as $A | ($d | s) as $D
     | (.cambios.anadidos | n) == ($D - $A) and (.cambios.quitados | n) == ($A - $D)
       and (.cambios.iguales | n) == ([$A[] | select(. as $x | $D | any(. == $x))] | sort)
       and .cambios.core.igual == $ic'
}

# ---------------------------------------------------------------- antes de CU1
CASO=preparacion
CURL_TOLERA=1 pedir GET /actuator/health
if [ "$ESTADO" != 200 ] || [ "$(jq -r '.status? // empty' "$CUERPO" 2>/dev/null || true)" != UP ]; then
  falla preparacion "el backend no responde en https://synkra.local"
fi
ok "GET /actuator/health 200 UP"

ejecutar "HEAD del manifiesto en el registro" curl -sS --max-time 15 -I -H "Accept: $ACEPTA" "http://$REGISTRO_HOST/v2/$REPO_CORE/manifests/$VC"
DIGEST="$(printf '%s\n' "$SALIDA" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}')"
case "$DIGEST" in
  sha256:????????????????????????????????????????????????????????????????) ;;
  *) falla preparacion "el registro no tiene $REPO_CORE:$VC (corre make imagenes)" ;;
esac
IMAGEN_NUEVA="$REGISTRO_IMAGEN/$REPO_CORE:$VC@$DIGEST"

# imagen_servida <imagen>: el digest de la imagen registrada tiene que servirlo el registro (HEAD 200).
imagen_servida() {
  local dg="${1##*@}" est
  ejecutar "HEAD de $dg en el registro" curl -sS --max-time 15 -I -o /dev/null -w '%{http_code}' -H "Accept: $ACEPTA" "http://$REGISTRO_HOST/v2/$REPO_CORE/manifests/$dg"
  est="$SALIDA"
  igual "el registro sirve el digest $dg" "$est" 200
}

T0=$SECONDS
tiempo() { echo "tiempo $1 $((SECONDS - T0))s"; T0=$SECONDS; }

# ================================================================ CU1: subir core y plugins
CASO=CU1
subir_core() { # subir_core <version> <imagen>: 201 o 409 si ya estaba; deja IMAGEN_REG con la imagen registrada
  jq -n --arg n "$CORE" --arg v "$1" --arg i "$2" '{nombre:$n, version:$v, imagen:$i}' >"$TMP/core-$1.json" 2>"$TMP/err" \
    || falla CU1 "jq armando el core $1 salió con $?"
  pedir POST /api/cores json "$TMP/core-$1.json"
  espero '201|409' "$([ "$ESTADO" = 409 ] && echo CONFLICTO || true)"
  if [ "$ESTADO" = 201 ]; then
    IMAGEN_REG="$2"
    ok "core $CORE $1 registrado (201)"
  else
    pedir GET "/api/cores/$CORE"
    espero 200
    jq_c --arg v "$1" '.versiones[] | select(.version==$v) | .imagen'
    IMAGEN_REG="$SALIDA"
    [ -n "$IMAGEN_REG" ] || falla CU1 "el core $CORE $1 dio 409 y GET /api/cores/$CORE no lo trae"
    ok "core $CORE $1 ya estaba (409 CONFLICTO); imagen $IMAGEN_REG"
    imagen_servida "$IMAGEN_REG"
  fi
}
subir_core "$VC" "$IMAGEN_NUEVA"
IMAGEN="$IMAGEN_REG"
# 4xx 1: el mismo core otra vez
pedir POST /api/cores json "$TMP/core-$VC.json"
espero 409 CONFLICTO
provocada 1
subir_core "$VERSION_MENTIROSA" "$IMAGEN"
# 4xx 2: nombre, version e imagen invalidos
jq -n '{nombre:"Core Malo", version:"2.1", imagen:"nginx:latest"}' >"$TMP/core-malo.json" || falla CU1 "jq armando el core malo salió con $?"
pedir POST /api/cores json "$TMP/core-malo.json"
espero 422 ENTIDAD_NO_PROCESABLE
jq_exige "el 422 detalla nombre, version e imagen" '[.detalles[].campo] | (index("nombre") != null) and (index("version") != null) and (index("imagen") != null)'
provocada 2

subir_plugin() { # subir_plugin <jar> <nombre> <version> <coreCompatible> [etiquetas JSON]
  local jar="$1" local_sha
  local -a partes=(--form "archivo=@$RAIZ/$jar;type=application/java-archive")
  [ -z "${5:-}" ] || partes+=(--form-string "etiquetas=$5")
  pedir POST "/api/cores/$CORE/plugins" "${partes[@]}"
  espero '201|409' "$([ "$ESTADO" = 409 ] && echo CONFLICTO || true)"
  if [ "$ESTADO" = 201 ]; then
    ejecutar "sha256sum $jar" sha256sum "$RAIZ/$jar"
    local_sha="${SALIDA%% *}"
    jq_exige "$2 $3 subido (201) con el sha256 del jar local" --arg n "$2" --arg v "$3" --arg c "$4" --arg s "$local_sha" \
      '.nombre==$n and .version==$v and .coreCompatible==$c and .sha256==$s'
    PLUGIN_SUBIDO=1
  else
    PLUGIN_SUBIDO=0
    pedir GET "/api/cores/$CORE/plugins"
    espero 200
    jq_exige "$2 $3 ya estaba (409 CONFLICTO) y GET lo trae con su coreCompatible" --arg n "$2" --arg v "$3" --arg c "$4" \
      'any(.[]; .nombre==$n and .version==$v and .coreCompatible==$c)'
  fi
}
subir_plugin "$JAR_CO" kyc-co 1.0.0 2.x '{"cliente":"e2e"}'
SUBIDO_CO="$PLUGIN_SUBIDO"
# 4xx 3: kyc-co otra vez
pedir POST "/api/cores/$CORE/plugins" --form "archivo=@$RAIZ/$JAR_CO;type=application/java-archive" --form-string 'etiquetas={"cliente":"e2e"}'
espero 409 CONFLICTO
provocada 3
subir_plugin "$JAR_PE13" kyc-pe 1.3.0 2.x
subir_plugin "$JAR_PE20" kyc-pe 2.0.0 3.x

# Lectura de vuelta de CU1.
pedir GET "/api/cores/$CORE"
espero 200
jq_exige "GET /api/cores/$CORE lista $VC y $VERSION_MENTIROSA" --arg a "$VC" --arg b "$VERSION_MENTIROSA" \
  '[.versiones[].version] | (index($a) != null) and (index($b) != null)'
pedir GET "/api/cores/$CORE/plugins?pais=co"
espero 200
jq_exige "?pais=co trae kyc-co 1.0.0" 'any(.[]; .nombre=="kyc-co" and .version=="1.0.0")'
if [ "$SUBIDO_CO" -eq 1 ]; then
  jq_exige "kyc-co 1.0.0 lleva pais co y cliente e2e" '.[] | select(.nombre=="kyc-co" and .version=="1.0.0") | .etiquetas.pais=="co" and .etiquetas.cliente=="e2e"'
fi
pedir GET "/api/cores/$CORE/plugins?pais=pe"
espero 200
jq_exige "?pais=pe trae kyc-pe 1.3.0 y 2.0.0" 'any(.[]; .nombre=="kyc-pe" and .version=="1.3.0") and any(.[]; .nombre=="kyc-pe" and .version=="2.0.0")'
echo "OK CU1"
tiempo CU1

# ================================================================ CU2: planear y confirmar en synkra-co
CASO=CU2
leer_pais
cuerpo_plan "$TMP/plan-d2.json" "$PAIS" "$VC" "$PLUGIN_CO"
pedir POST /api/despliegues json "$TMP/plan-d2.json"
espero 201
jq_exige "plan creado: pendiente y creadoPor e2e" '.estado=="pendiente" and .creadoPor=="e2e" and .tipo=="despliegue"'
jq_c .id
D2="$SALIDA"
verifica_cambios "$ANTES_CSV" "$PLUGIN_CO" "$([ "$ANTES_IMG" = "$IMAGEN" ] && echo true || echo false)"
echo "cambios de CU2 (antes: ${ANTES_CSV:-vacío}): $(jq -c .cambios "$CUERPO")"
# 4xx 4: pais fuera de la lista
cuerpo_plan "$TMP/plan-mx.json" mx "$VC" "$PLUGIN_CO"
pedir POST /api/despliegues json "$TMP/plan-mx.json"
espero 422 PAIS_NO_PERMITIDO
provocada 4
# 4xx 5: usuario invalido
USUARIO_PETICION='No Valido' pedir POST /api/despliegues json "$TMP/plan-d2.json"
espero 422 USUARIO_INVALIDO
provocada 5
confirmar "$D2"
# 4xx 6: confirmar otra vez
pedir POST "/api/despliegues/$D2/confirmacion"
espero 409 PLAN_NO_PENDIENTE
provocada 6
esperar_estado "$D2" listo
rollout
ok "rollout status de synkra-core terminó"
imagen_core
igual "la imagen del contenedor core es la registrada" "$SALIDA" "$IMAGEN"
cm_plugins
igual "el ConfigMap lista solo $PLUGIN_CO" "$SALIDA" "$PLUGIN_CO"
core_cargo
igual "lo que el core cargó es exactamente $PLUGIN_CO" "$SALIDA" "$PLUGIN_CO"
managed_apply deployment/synkra-core
managed_apply configmap/synkra-core-config
managed_apply service/synkra-core
pedir GET "/api/despliegues/$D2"
espero 200
jq_exige "GET /api/despliegues/$D2: listo con su imagen y sus plugins" --arg i "$IMAGEN" --arg p "$PLUGIN_CO" \
  ".estado==\"listo\" and .imagen==\$i and (.plugins | $CSV_FILTRO) == \$p"
pais_concuerda true "$D2"
anotacion
A2="$SALIDA"
pods_core
P2="$SALIDA"
echo "CU2 anotacion=${A2:-vacía} pods=$P2"
echo "OK CU2"
tiempo CU2

# ================================================================ CU3: cambiar plugins
CASO=CU3
cuerpo_plan "$TMP/plan-d3.json" "$PAIS" "$VC" "$PLUGIN_CO" "$PLUGIN_PE13"
pedir POST /api/despliegues json "$TMP/plan-d3.json"
espero 201
jq_exige "plan creado: pendiente y creadoPor e2e" '.estado=="pendiente" and .creadoPor=="e2e"'
jq_c .id
D3="$SALIDA"
verifica_cambios "$PLUGIN_CO" "$PLUGIN_CO,$PLUGIN_PE13" true
jq_exige "cambios: solo se añade $PLUGIN_PE13" '(.cambios.anadidos | length) == 1 and (.cambios.quitados | length) == 0'
confirmar "$D3"
esperar_estado "$D3" listo
rollout
ok "rollout status de synkra-core terminó"
anotacion
A3="$SALIDA"
if [ -z "$A3" ] || [ "$A3" = "$A2" ]; then
  falla CU3 "la plantilla del pod no cambió: synkra.io/config-sha256 era '${A2:-vacía}' y es '${A3:-vacía}' (un cambio solo de plugins no recreó el pod)"
fi
ok "la anotación synkra.io/config-sha256 cambió ($A2 a $A3)"
pods_core
P3="$SALIDA"
[ "$P3" != "$P2" ] || falla CU3 "el pod del core es el mismo ($P3): un cambio solo de plugins no lo recreó"
ok "hay un pod nuevo ($P2 a $P3)"
imagen_core
igual "la imagen del contenedor core no cambió" "$SALIDA" "$IMAGEN"
cm_plugins
igual "el ConfigMap lista $PLUGIN_CO y $PLUGIN_PE13" "$SALIDA" "$PLUGIN_CO,$PLUGIN_PE13"
core_cargo
igual "lo que el core cargó es exactamente $PLUGIN_CO,$PLUGIN_PE13" "$SALIDA" "$PLUGIN_CO,$PLUGIN_PE13"
pais_concuerda true "$D3"
echo "OK CU3"
tiempo CU3

# ================================================================ CU4: combinacion incompatible rechazada
CASO=CU4
imagenes_ns
I0="$SALIDA"
version_rv deployment/synkra-core
RV_DEPLOY="$SALIDA"
version_rv configmap/synkra-core-config
RV_CM="$SALIDA"
largo_historial
H0="$SALIDA"
core_cargo
C0="$SALIDA"
cuerpo_plan "$TMP/plan-malo.json" "$PAIS" "$VC" "$PLUGIN_CO" "$PLUGIN_PE20"
pedir POST /api/despliegues json "$TMP/plan-malo.json"
espero 422 COMBINACION_RECHAZADA
jq_exige "el 422 trae un detalle de plugins.kyc-pe" 'any(.detalles[]; .campo=="plugins.kyc-pe")'
echo "motivo: $(jq -r '.detalles[] | select(.campo=="plugins.kyc-pe") | .motivo' "$CUERPO")"
provocada 7
imagenes_ns
I1="$SALIDA"
echo "CU4 imagenes antes=$I0 despues=$I1"
igual "las imágenes del namespace no cambiaron" "$I1" "$I0"
version_rv deployment/synkra-core
[ "$SALIDA" = "$RV_DEPLOY" ] || falla CU4 "el resourceVersion del Deployment cambió de $RV_DEPLOY a $SALIDA"
ok "el resourceVersion del Deployment sigue en $RV_DEPLOY"
version_rv configmap/synkra-core-config
[ "$SALIDA" = "$RV_CM" ] || falla CU4 "el resourceVersion del ConfigMap cambió de $RV_CM a $SALIDA"
ok "el resourceVersion del ConfigMap sigue en $RV_CM"
largo_historial
igual "el historial de $PAIS tiene el mismo largo" "$SALIDA" "$H0"
core_cargo
igual "lo que el core cargó sigue siendo lo de $D3" "$SALIDA" "$C0"
echo "OK CU4"
tiempo CU4

# ================================================================ CU5: fallo y vuelta atras
CASO=CU5
cuerpo_plan "$TMP/plan-d4.json" "$PAIS" "$VERSION_MENTIROSA" "$PLUGIN_PE20"
pedir POST /api/despliegues json "$TMP/plan-d4.json"
espero 201
jq_exige "plan de $VERSION_MENTIROSA con $PLUGIN_PE20 aprobado: pendiente" '.estado=="pendiente"'
jq_c .id
D4="$SALIDA"
confirmar "$D4"
T_D4=$SECONDS
# Otro plan, sobre VC, mientras D4 aplica.
cuerpo_plan "$TMP/plan-otro.json" "$PAIS" "$VC" "$PLUGIN_CO"
pedir POST /api/despliegues json "$TMP/plan-otro.json"
espero 201
jq_c .id
POTRO="$SALIDA"
pedir GET "/api/despliegues/$D4"
espero 200
jq_c .estado
[ "$SALIDA" = aplicando ] || falla CU5 "D4 cerró antes de provocar DESPLIEGUE_EN_CURSO (está $SALIDA, a los $((SECONDS - T_D4))s)"
# 4xx 8: confirmar otro plan mientras D4 aplica
pedir POST "/api/despliegues/$POTRO/confirmacion"
espero 409 DESPLIEGUE_EN_CURSO
provocada 8
esperar_estado "$D4" fallido
echo "D4 tardó $((SECONDS - T_D4))s en fallar"
pedir GET "/api/despliegues/$D4"
espero 200
jq_exige "D4 trae contexto con logCore que nombra kyc-pe" '(.contexto.logCore // "") | (length > 0) and contains("kyc-pe")'
pods_core
P_AHORA="$SALIDA"
ejecutar "jq pods" jq -r '[.items[] | select(.metadata.deletionTimestamp==null) | select(any(.status.containerStatuses[]?; .name=="core" and (.state.waiting.reason=="CrashLoopBackOff" or .state.terminated.reason=="Error" or .lastState.terminated.reason=="Error"))) | .metadata.name] | join(",")' "$TMP/pods.json"
[ -n "$SALIDA" ] || falla CU5 "ningún pod del core en CrashLoopBackOff o Error tras D4 fallido (pods: $P_AHORA)"
ok "pod del core caído: $SALIDA"
ejecutar "jq pod viejo" jq -r --arg p "$P3" '[.items[] | select(.metadata.name==$p) | .status.conditions[] | select(.type=="Ready") | .status] | join(",")' "$TMP/pods.json"
igual "el pod viejo $P3 sigue Ready" "$SALIDA" True
core_cargo
igual "lo que el core sirve por el Service sigue siendo lo de $D3" "$SALIDA" "$PLUGIN_CO,$PLUGIN_PE13"
pais_concuerda false "$D3"
# 4xx 9: revertir D4 (fallido)
pedir POST "/api/despliegues/$D4/reversion"
espero 422 ORIGEN_NO_LISTO
provocada 9
# 4xx 10: revertir un despliegue que no existe
pedir POST /api/despliegues/0/reversion
espero 404 NO_ENCONTRADO
provocada 10
# La vuelta atras: revertir D3.
pedir POST "/api/despliegues/$D3/reversion"
espero 201
jq_exige "la reversión es un plan de tipo reversion con origen $D3" --argjson o "$D3" '.tipo=="reversion" and .origenId==$o and .estado=="pendiente"'
jq_c .id
D5="$SALIDA"
confirmar "$D5"
esperar_estado "$D5" listo
rollout
ok "rollout status de synkra-core terminó"
pods_core
ejecutar "jq pods" jq -r '[.items[] | select(.metadata.deletionTimestamp==null) | select(any(.status.conditions[]; .type=="Ready" and .status!="True")) | .metadata.name] | join(",")' "$TMP/pods.json"
igual "todos los pods del core están Ready" "$SALIDA" ""
ejecutar "jq pods" jq -r '[.items[] | select(.metadata.deletionTimestamp==null) | select(any(.status.containerStatuses[]?; .state.waiting.reason=="CrashLoopBackOff")) | .metadata.name] | join(",")' "$TMP/pods.json"
igual "ningún pod del core en CrashLoopBackOff" "$SALIDA" ""
imagenes_ns
igual "las imágenes del namespace son las de CU4" "$SALIDA" "$I0"
cm_plugins
igual "el ConfigMap es el de $D3" "$SALIDA" "$PLUGIN_CO,$PLUGIN_PE13"
core_cargo
igual "lo que el core cargó es lo de $D3" "$SALIDA" "$PLUGIN_CO,$PLUGIN_PE13"
pedir GET "/api/paises/$PAIS/despliegues"
espero 200
jq_exige "el historial termina en $D5 reversion listo, $D4 despliegue fallido, $D3 despliegue listo" \
  --argjson a "$D5" --argjson b "$D4" --argjson c "$D3" --argjson o "$D3" \
  '(.[0] | .id==$a and .tipo=="reversion" and .origenId==$o and .estado=="listo")
   and (.[1] | .id==$b and .tipo=="despliegue" and .estado=="fallido")
   and (.[2] | .id==$c and .tipo=="despliegue" and .estado=="listo")'
pais_concuerda true "$D5"
echo "OK CU5"
tiempo CU5

[ "$N4" -eq 10 ] || falla e2e "se provocaron $N4 respuestas 4xx y la tabla pide 10"
echo "OK e2e: CU1 a CU5, $N4 respuestas 4xx con codigo, mensaje y detalles"
