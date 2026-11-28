#!/usr/bin/env bash
# Guardia de contexto (AUTONOMIA-01): solo se trabaja contra el cluster kind-synkra,
# con el kubeconfig del proyecto. Cada rechazo sale con 1 y una linea en stderr.
# Uso: scripts/guardia-contexto.sh [--antes-de-crear]
set -euo pipefail

ANTES=0
[ "${1:-}" = "--antes-de-crear" ] && ANTES=1

fallar() { echo "$1" >&2; exit 1; }

# Ejecuta un comando externo y, si falla, da una linea con el comando y el codigo.
capturar() {
  local salida rc=0
  salida=$("$@" 2>/dev/null) || rc=$?
  if [ "$rc" -ne 0 ]; then
    return "$rc"
  fi
  printf '%s' "$salida"
}

RUTA_CRUDA=${KUBECONFIG_SYNKRA:-$PWD/.kube/kind-synkra.yaml}
RUTA=$(realpath -m "$RUTA_CRUDA") || fallar "error: realpath -m $RUTA_CRUDA falló con código $?"

# 1. Nunca el kubeconfig global (no se abre).
GLOBAL=$(realpath -m "${HOME}/.kube/config") || fallar "error: realpath -m ${HOME}/.kube/config falló con código $?"
if [ "$RUTA" = "$GLOBAL" ]; then
  fallar "kubeconfig no permitido: $RUTA"
fi

# 2. Debe existir.
if [ ! -e "$RUTA" ]; then
  if [ "$ANTES" -eq 1 ]; then
    echo "sin kubeconfig todavía: lo crea make cluster"
    exit 0
  fi
  fallar "falta el kubeconfig $RUTA: corre make cluster"
fi

# 2b. Tras `kind delete cluster` el archivo queda sin ningun contexto: equivale a no tener kubeconfig.
if [ "$ANTES" -eq 1 ]; then
  rc=0
  CTXS=$(capturar kubectl --kubeconfig "$RUTA" config get-contexts -o name) || rc=$?
  if [ "$rc" -eq 0 ] && [ -z "$CTXS" ]; then
    echo "kubeconfig sin contextos todavía: lo rellena make cluster"
    exit 0
  fi
fi

# 3. El contexto actual debe ser kind-synkra.
rc=0
CTX=$(capturar kubectl --kubeconfig "$RUTA" config current-context) || rc=$?
if [ "$rc" -ne 0 ] || [ -z "$CTX" ]; then
  fallar "contexto no permitido: sin contexto actual en $RUTA (kubectl config current-context, código $rc)"
fi
if [ "$CTX" != "kind-synkra" ]; then
  fallar "contexto no permitido: $CTX (solo kind-synkra) en $RUTA"
fi

# 4. El servidor debe ser el del cluster kind synkra.
rc=0
SERVIDOR=$(capturar kubectl --kubeconfig "$RUTA" config view --minify -o jsonpath='{.clusters[0].cluster.server}') || rc=$?
if [ "$rc" -ne 0 ] || [ -z "$SERVIDOR" ]; then
  fallar "error: kubectl config view --minify falló con código $rc en $RUTA"
fi
rc=0
KIND_CFG=$(capturar kind get kubeconfig --name synkra) || rc=$?
if [ "$rc" -ne 0 ]; then
  if [ "$ANTES" -eq 1 ]; then
    echo "el clúster kind synkra no existe todavía: lo crea make cluster"
    exit 0
  fi
  fallar "contexto no permitido: el clúster kind synkra no existe"
fi
rc=0
SERVIDOR_KIND=$(printf '%s' "$KIND_CFG" | capturar kubectl --kubeconfig /dev/stdin config view --minify -o jsonpath='{.clusters[0].cluster.server}') || rc=$?
if [ "$rc" -ne 0 ] || [ -z "$SERVIDOR_KIND" ]; then
  fallar "error: kubectl config view sobre kind get kubeconfig falló con código $rc"
fi
if [ "$SERVIDOR" != "$SERVIDOR_KIND" ]; then
  fallar "contexto no permitido: kind-synkra apunta a $SERVIDOR y el clúster kind synkra está en $SERVIDOR_KIND"
fi

# 5. Todo cuadra.
echo "contexto permitido: kind-synkra ($SERVIDOR)"
