#!/usr/bin/env bash
# Construye las tres imagenes de SynKra en el Docker local; con --subir, ademas las verifica y las sube al registro.
# Uso: JAVA_HOME=<java 21> bash scripts/imagenes.sh [--subir]
# Versiones: salen de build.gradle.kts (backend, core) y de IMAGEN_INIT_POR_DEFECTO; scripts/verificar-imagenes.sh lee las mismas.
# Prefijo: SYNKRA_REGISTRO_IMAGENES (por defecto localhost:5001).
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
REG="${SYNKRA_REGISTRO_IMAGENES:-localhost:5001}"
JAVA_FUENTE=backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java
SUBIR=0
case "${1:-}" in
  "") ;;
  --subir) SUBIR=1 ;;
  *) echo "FALLA uso: opcion desconocida '$1' (solo --subir)" >&2; exit 1 ;;
esac
# Desde el host el registro se alcanza por 127.0.0.1 (el prefijo localhost:5001 es el que ven los nodos).
REG_HOST="${REG/#localhost/127.0.0.1}"
ACEPTA='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'

if [ "$SUBIR" -eq 1 ] && ! curl -sf --max-time 5 "http://$REG_HOST/v2/" >/dev/null; then
  echo "FALLA subir: el registro $REG no responde: corre make registro" >&2
  exit 1
fi

if ! "${JAVA_HOME:-/no/definido}/bin/java" -version 2>&1 | grep -q '"21'; then
  echo "FALLA entorno: JAVA_HOME (${JAVA_HOME:-sin definir}) no es Java 21" >&2
  exit 1
fi

version_gradle() { grep -E '^version *= *"' "$1/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
VB="$(version_gradle backend)"; VC="$(version_gradle core)"
VI="$(grep -E 'IMAGEN_INIT_POR_DEFECTO *= *"' "$JAVA_FUENTE" | head -1 | sed -E 's/.*:([^:"]+)".*/\1/')"
for v in "$VB" "$VC" "$VI"; do
  [ -n "$v" ] || { echo "FALLA versiones: no se pudo leer una version de build.gradle.kts o de IMAGEN_INIT_POR_DEFECTO" >&2; exit 1; }
done

# Las versiones de las etiquetas deben coincidir con las del overlay (newTag y SYNKRA_KUBERNETES_IMAGEN_INIT); si no, FALLA versiones.
KUBECONFIG=/dev/null kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py --versiones \
  || { echo "FALLA versiones: el overlay de kind no coincide con el origen de las versiones" >&2; exit 1; }

echo "+ ./gradlew :backend:bootJar :core:bootJar"
./gradlew :backend:bootJar :core:bootJar || { echo "FALLA gradle: ./gradlew :backend:bootJar :core:bootJar salió con $?" >&2; exit 1; }

IMAGENES=()
construir() { # imagen directorio
  IMAGENES+=("$1")
  echo "+ docker build -t $1 $2"
  local rc=0 nombre="${1##*/}"
  docker build -t "$1" "$2" || rc=$?
  [ "$rc" -eq 0 ] || { echo "FALLA ${nombre%%:*}: docker build -t $1 $2 salió con $rc" >&2; exit 1; }
}
construir "$REG/synkra-backend:$VB" backend
construir "$REG/synkra-core-ejemplo:$VC" core
construir "$REG/synkra-init-descarga:$VI" init-descarga
echo "OK imagenes construidas con prefijo $REG"

[ "$SUBIR" -eq 1 ] || exit 0

# Con --subir: primero se verifican; si algo falla no se sube nada.
SYNKRA_REGISTRO_IMAGENES="$REG" bash scripts/verificar-imagenes.sh || { echo "FALLA subir: scripts/verificar-imagenes.sh salió con $?; no se sube nada" >&2; exit 1; }

# Corre un comando externo; si falla, FALLA <imagen>: <orden> salió con <codigo>.
externo() { # imagen orden...
  local imagen="$1" rc=0; shift
  "$@" || rc=$?
  [ "$rc" -eq 0 ] || { echo "FALLA $imagen: $* salió con $rc" >&2; exit 1; }
}
for imagen in "${IMAGENES[@]}"; do
  echo "+ docker push $imagen"
  externo "$imagen" docker push "$imagen"
done
for imagen in "${IMAGENES[@]}"; do
  ruta="${imagen#"$REG"/}"; repo="${ruta%%:*}"; etiqueta="${ruta##*:}"
  tags="$(externo "$imagen" curl -sf --max-time 10 "http://$REG_HOST/v2/$repo/tags/list")"
  printf '%s' "$tags" | grep -qF "\"$etiqueta\"" || { echo "FALLA $imagen: la etiqueta $etiqueta no está en /v2/$repo/tags/list" >&2; exit 1; }
  cabeceras="$(externo "$imagen" curl -sfI --max-time 10 -H "Accept: $ACEPTA" "http://$REG_HOST/v2/$repo/manifests/$etiqueta")"
  digest="$(printf '%s\n' "$cabeceras" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}')"
  [ -n "$digest" ] || { echo "FALLA $imagen: el registro no devolvió Docker-Content-Digest" >&2; exit 1; }
  echo "subida $imagen $digest"
done
echo "OK imagenes subidas"
