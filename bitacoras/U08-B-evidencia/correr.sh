#!/usr/bin/env bash
# Uso: bash bitacoras/U08-B-evidencia/correr.sh N  -> escribe caN.txt con fecha, sha, JAVA_HOME y la salida completa de caN.sh
set -u
cd "$(git rev-parse --show-toplevel)"
export JAVA_HOME="${JAVA_HOME:-$HOME/.gradle/jdks/eclipse_adoptium-21-aarch64-linux.2}"
d=bitacoras/U08-B-evidencia
{ echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)  JAVA_HOME=$JAVA_HOME  script: $d/ca$1.sh"; bash $d/ca$1.sh </dev/null 2>&1; } > $d/ca$1.txt
tail -n +1 $d/ca$1.txt | head -3 >/dev/null
