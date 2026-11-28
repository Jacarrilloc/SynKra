#!/usr/bin/env bash
# Verifica las tres imagenes de U08-B (C-103, H-28): bases por digest, usuario numerico, arquitectura y
# atadura a ConstructorObjetosPais. Sin red. Uso: bash scripts/verificar-imagenes.sh
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

BACKEND="$REG/synkra-backend:0.1.0"
CORE="$REG/synkra-core-ejemplo:2.1.0"
INIT="$REG/synkra-init-descarga:1.0.0"
ARQ="$(docker version -f '{{.Server.Arch}}')"

# 1. Toda linea FROM lleva digest.
for df in backend/Dockerfile core/Dockerfile init-descarga/Dockerfile; do
  while IFS= read -r linea; do
    if printf '%s' "$linea" | grep -qE '@sha256:[0-9a-f]{64}'; then ok "$df" "FROM"; else falla "$df" "FROM" "@sha256:<64 hex>" "$linea"; fi
  done < <(grep -E '^FROM ' "$df")
done

# 2. La etiqueta del init es la de ConstructorObjetosPais con el prefijo del registro.
igual "$INIT" "etiqueta" "$REG/${INIT_DEFECTO#*/}" "$INIT"

for imagen in "$BACKEND" "$CORE" "$INIT"; do
  if ! docker image inspect "$imagen" >/dev/null 2>&1; then falla "$imagen" "existe" "imagen construida" "no existe"; continue; fi
  usuario="$(docker image inspect -f '{{.Config.User}}' "$imagen")"
  igual "$imagen" "User" "$UID_C:$GID_C" "$usuario"
  case "$usuario" in ""|root|0|0:*) falla "$imagen" "User-no-root" "distinto de vacio/root/0" "$usuario" ;; esac
  n="synkra-u08b-id-$$-$RANDOM"; CONTENEDORES+=("$n")
  uid_real="$(docker run --name "$n" --entrypoint id "$imagen" -u 2>&1 || true)"; docker rm -f "$n" >/dev/null 2>&1 || true
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
apk="$(docker run --name "$n" --entrypoint sh "$INIT" -c 'command -v apk || true' 2>&1 || true)"; docker rm -f "$n" >/dev/null 2>&1 || true
igual "$INIT" "sin-apk" "" "$apk"

if [ "$FALLOS" -ne 0 ]; then echo "FALLAN $FALLOS comprobaciones"; exit 1; fi
echo "OK imagenes"
