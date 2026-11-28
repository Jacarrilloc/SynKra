#!/usr/bin/env bash
# Verifica las cuatro imagenes (tres de U08-B y el front de U09-D; C-103, H-28): bases por digest, usuario numerico,
# arquitectura y atadura a ConstructorObjetosPais. Sin red. Uso: bash scripts/verificar-imagenes.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
REG="${SYNKRA_REGISTRO_IMAGENES:-localhost:5001}"
JAVA_FUENTE=backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java

CONTENEDORES=()
limpiar() { local c; for c in "${CONTENEDORES[@]:-}"; do [ -n "$c" ] && docker rm -f "$c" >/dev/null 2>&1 || true; done; }
trap limpiar EXIT

FALLOS=0
ok() { echo "ok $1 $2"; }
falla() { echo "FALLA $1 $2: esperado $3 / visto $4"; FALLOS=$((FALLOS + 1)); }
igual() { # imagen comprobacion esperado visto
  if [ "$3" = "$4" ]; then ok "$1" "$2"; else falla "$1" "$2" "$3" "$4"; fi
}

constante() { # nombre -> valor (sin comillas ni sufijo L)
  local v
  v="$(grep -E "static final [A-Za-z]+ $1 = " "$JAVA_FUENTE" | head -1 | sed -E 's/.*= *"?([^";]*)"?;.*/\1/; s/L$//' || true)"
  [ -n "$v" ] || { echo "FALLA constantes: $1"; exit 1; }
  printf '%s' "$v"
}
UID_C="$(constante UID_CONTENEDORES)"; GID_C="$(constante GID_CONTENEDORES)"
PUERTO="$(constante PUERTO)"; RUTA_PLUGINS="$(constante RUTA_PLUGINS)"; RUTA_CONFIG="$(constante RUTA_CONFIG)"
COMANDO_INIT="$(constante COMANDO_INIT)"; INIT_DEFECTO="$(constante IMAGEN_INIT_POR_DEFECTO)"

# Versiones: un solo origen (los mismos archivos que lee scripts/imagenes.sh).
version_gradle() { grep -E '^version *= *"' "$1/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
VB="$(version_gradle backend)"; VC="$(version_gradle core)"; VI="${INIT_DEFECTO##*:}"
BACKEND="$REG/synkra-backend:$VB"
CORE="$REG/synkra-core-ejemplo:$VC"
INIT="$REG/synkra-init-descarga:$VI"
# La version del front se escribe solo en el .csproj (U09-D).
VF="$(grep -oE '<Version>[^<]+</Version>' front/Synkra.Front.csproj 2>/dev/null | head -1 | sed -E 's/<[^>]+>//g' || true)"
FRONT="$REG/synkra-front:${VF:-sin-version}"
ARQ="$(docker version -f '{{.Server.Arch}}')"

# Las versiones de las etiquetas deben coincidir con las del overlay (newTag y SYNKRA_KUBERNETES_IMAGEN_INIT); si no, FALLA versiones.
KUBECONFIG=/dev/null kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py --versiones \
  || { echo "FALLA versiones: el overlay de kind no coincide con el origen de las versiones"; FALLOS=$((FALLOS + 1)); }

# 1. Toda linea FROM lleva digest.
[ -n "$VF" ] || falla "front/Synkra.Front.csproj" "Version" "<Version>x.y.z</Version>" "ausente"
for df in backend/Dockerfile core/Dockerfile init-descarga/Dockerfile front/Dockerfile; do
  [ -f "$df" ] || { falla "$df" "existe" "Dockerfile" "no existe"; continue; }
  while IFS= read -r linea; do
    if printf '%s' "$linea" | grep -qE '@sha256:[0-9a-f]{64}'; then ok "$df" "FROM"; else falla "$df" "FROM" "@sha256:<64 hex>" "$linea"; fi
  done < <(grep -iE '^[[:space:]]*FROM[[:space:]]' "$df")
done

# 2. La etiqueta del init es la de ConstructorObjetosPais con el prefijo del registro.
igual "$INIT" "etiqueta" "$REG/${INIT_DEFECTO#*/}" "$INIT"

for imagen in "$BACKEND" "$CORE" "$INIT" "$FRONT"; do
  if ! docker image inspect "$imagen" >/dev/null 2>&1; then falla "$imagen" "existe" "imagen construida" "no existe"; continue; fi
  usuario="$(docker image inspect -f '{{.Config.User}}' "$imagen")"
  igual "$imagen" "User" "$UID_C:$GID_C" "$usuario"
  case "$usuario" in ""|root|0|0:*) falla "$imagen" "User-no-root" "distinto de vacio/root/0" "$usuario" ;; esac
  n="synkra-u08b-id-$$-$RANDOM"; CONTENEDORES+=("$n")
  uid_real="$(docker run --pull=never --name "$n" --entrypoint id "$imagen" -u 2>&1 || true)"; docker rm -f "$n" >/dev/null 2>&1 || true
  igual "$imagen" "id-u" "$UID_C" "$uid_real"
  igual "$imagen" "arquitectura" "$ARQ" "$(docker image inspect -f '{{.Architecture}}' "$imagen")"
done

exponer() { docker image inspect -f '{{range $k, $v := .Config.ExposedPorts}}{{$k}} {{end}}' "$1" 2>/dev/null | sed 's/ $//'; }
volumenes() { docker image inspect -f '{{range $k, $v := .Config.Volumes}}{{$k}} {{end}}' "$1" 2>/dev/null | sed 's/ $//'; }
igual "$BACKEND" "ExposedPorts" "$PUERTO/tcp" "$(exponer "$BACKEND")"
igual "$CORE" "ExposedPorts" "$PUERTO/tcp" "$(exponer "$CORE")"
igual "$CORE" "Volumes" "$RUTA_CONFIG $RUTA_PLUGINS" "$(volumenes "$CORE")"
igual "$INIT" "Entrypoint" "[\"$COMANDO_INIT\"]" "$(docker image inspect -f '{{json .Config.Entrypoint}}' "$INIT" 2>/dev/null || true)"
n="synkra-u08b-apk-$$-$RANDOM"; CONTENEDORES+=("$n")
apk="$(docker run --pull=never --name "$n" --entrypoint sh "$INIT" -c 'command -v apk || true' 2>&1 || true)"; docker rm -f "$n" >/dev/null 2>&1 || true
igual "$INIT" "sin-apk" "" "$apk"

# El front: puerto del manifiesto (nunca escrito aqui), entrypoint, variable del puerto y sin SDK.
if docker image inspect "$FRONT" >/dev/null 2>&1; then
  PUERTO_FRONT="$(KUBECONFIG=/dev/null kubectl kustomize deploy/base/front 2>/dev/null | python3 -I -c "import sys,yaml; d=[o for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment'][0]; print(d['spec']['template']['spec']['containers'][0]['ports'][0]['containerPort'])" 2>/dev/null || true)"
  if [ -z "$PUERTO_FRONT" ]; then falla "$FRONT" "puerto-del-manifiesto" "containerPort de deploy/base/front" "ilegible"; fi
  igual "$FRONT" "ExposedPorts" "$PUERTO_FRONT/tcp" "$(exponer "$FRONT")"
  igual "$FRONT" "ASPNETCORE_HTTP_PORTS" "$PUERTO_FRONT" "$(docker image inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$FRONT" | sed -n 's/^ASPNETCORE_HTTP_PORTS=//p')"
  igual "$FRONT" "Entrypoint" '["dotnet","Synkra.Front.dll"]' "$(docker image inspect -f '{{json .Config.Entrypoint}}' "$FRONT" 2>/dev/null || true)"
  runtimes="$(docker run --pull=never --rm --entrypoint dotnet "$FRONT" --list-runtimes 2>/dev/null | awk '{print $1}' | sort | tr '\n' ' ' | sed 's/ $//' || true)"
  igual "$FRONT" "runtime" "Microsoft.AspNetCore.App Microsoft.NETCore.App" "$runtimes"
  igual "$FRONT" "sin-sdk" "" "$(docker run --pull=never --rm --entrypoint dotnet "$FRONT" --list-sdks 2>/dev/null || true)"
fi

if [ "$FALLOS" -ne 0 ]; then echo "FALLAN $FALLOS comprobaciones"; exit 1; fi
echo "OK imagenes"
