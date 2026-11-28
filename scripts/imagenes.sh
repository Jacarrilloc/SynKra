#!/usr/bin/env bash
# Construye las tres imagenes de SynKra en el Docker local (no sube nada; subir es de U08-C).
# Uso: JAVA_HOME=<java 21> bash scripts/imagenes.sh
# Prefijo: SYNKRA_REGISTRO_IMAGENES (por defecto localhost:5001).
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
REG="${SYNKRA_REGISTRO_IMAGENES:-localhost:5001}"
JAVA_FUENTE=backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java

if ! "${JAVA_HOME:-/no/definido}/bin/java" -version 2>&1 | grep -q '"21'; then
  echo "FALLA entorno: JAVA_HOME (${JAVA_HOME:-sin definir}) no es Java 21" >&2
  exit 1
fi

version_gradle() { grep -E '^version *= *"' "$1/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
VB="$(version_gradle backend)"; VC="$(version_gradle core)"
VI="$(grep -E 'IMAGEN_INIT_POR_DEFECTO *= *"' "$JAVA_FUENTE" | head -1 | sed -E 's/.*:([^:"]+)".*/\1/')"
[ "$VB" = "0.1.0" ] || { echo "FALLA versiones: backend/build.gradle.kts dice $VB, la etiqueta es 0.1.0" >&2; exit 1; }
[ "$VC" = "2.1.0" ] || { echo "FALLA versiones: core/build.gradle.kts dice $VC, la etiqueta es 2.1.0" >&2; exit 1; }
[ "$VI" = "1.0.0" ] || { echo "FALLA versiones: IMAGEN_INIT_POR_DEFECTO dice $VI, la etiqueta es 1.0.0" >&2; exit 1; }

echo "+ ./gradlew :backend:bootJar :core:bootJar"
./gradlew :backend:bootJar :core:bootJar || { echo "FALLA gradle: ./gradlew :backend:bootJar :core:bootJar salió con $?" >&2; exit 1; }

construir() { # imagen directorio
  echo "+ docker build -t $1 $2"
  local rc=0
  docker build -t "$1" "$2" || rc=$?
  [ "$rc" -eq 0 ] || { echo "FALLA ${1%%:*}: docker build -t $1 $2 salió con $rc" >&2; exit 1; }
}
construir "$REG/synkra-backend:$VB" backend
construir "$REG/synkra-core-ejemplo:$VC" core
construir "$REG/synkra-init-descarga:$VI" init-descarga
echo "OK imagenes construidas con prefijo $REG"
