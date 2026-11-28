#!/usr/bin/env bash
# Uso (desde la raiz del worktree): bash bitacoras/U08-D-evidencia/corre.sh <nombre> <comando...>
# Escribe bitacoras/U08-D-evidencia/<nombre>.txt con la cabecera de la decision 5, el comando y la salida completa.
N="$1"; shift
SALIDA="bitacoras/U08-D-evidencia/$N.txt"
{
  echo "fecha: $(date -Is)"
  echo "sha: $(git rev-parse --short HEAD) (arbol: $(git status --porcelain | wc -l) cambios sin confirmar)"
  echo "uname -m: $(uname -m)"
  echo "docker arch: $(docker version -f '{{.Server.Arch}}')"
  echo "JAVA_HOME: ${JAVA_HOME:-sin definir}"
  echo "\$ $*"
  "$@"
} > "$SALIDA" 2>&1
echo "$N salida=$?"
