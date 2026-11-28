#!/usr/bin/env bash
# P3 (H-21): el core no conoce países. Mismo control que CoreSinPaisesTest, como comando de shell.
# El patrón se extrae de la constante PATRON de la prueba (única fuente). Sale 0 si core/src/main está limpio.
# Uso: bash scripts/control-core-sin-paises.sh [raiz-del-repo]
set -euo pipefail

raiz="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
prueba="$raiz/core/src/test/java/io/synkra/core/CoreSinPaisesTest.java"
main="$raiz/core/src/main"

patron="$(sed -n '/PATRON-INICIO/,/PATRON-FIN/ s/^.*static final String PATRON = "\(.*\)";$/\1/p' "$prueba" | sed 's/\\"/"/g')"
[ -n "$patron" ] || { echo "ERROR: no se pudo leer PATRON de $prueba" >&2; exit 2; }
[ -d "$main" ] || { echo "ERROR: no existe $main" >&2; exit 2; }

fallo=0
# Contenido: Java, .properties, .yaml, .yml (salida archivo:línea:texto)
if grep -rniE --include='*.java' --include='*.properties' --include='*.yaml' --include='*.yml' -e "$patron" "$main"; then
  fallo=1
fi
# Nombres de archivo y directorios
if (cd "$main" && find . -mindepth 1 | grep -iE -e "$patron" | sed "s|^|$main/: el nombre nombra un país: |" | grep .); then
  fallo=1
fi
if [ "$fallo" -ne 0 ]; then
  echo "FALLO: el core nombra un país (P3)" >&2
  exit 1
fi
echo "OK: core/src/main no nombra países"
