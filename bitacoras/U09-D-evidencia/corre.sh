#!/usr/bin/env bash
# Uso: bash bitacoras/U09-D-evidencia/corre.sh caN  -> escribe caN.txt con fecha, sha, arquitectura, JAVA_HOME, comando y salida completa.
set -u
N="$1"; E=bitacoras/U09-D-evidencia
{
  echo "fecha: $(date -Is)"
  echo "sha: $(git rev-parse --short HEAD) $(git status --porcelain -- . ':!bitacoras' ':!revisiones' | wc -l) cambios de codigo sin confirmar"
  echo "uname -m: $(uname -m)"
  echo "docker arch: $(docker version -f '{{.Server.Arch}}')"
  echo "JAVA_HOME: ${JAVA_HOME:-sin definir}"
  echo "\$ bash $E/$N.sh"
  bash "$E/$N.sh" 2>&1
} > "$E/$N.txt"
