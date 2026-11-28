#!/usr/bin/env bash
# Prueba de humo de la imagen del core (U02-T12): arranca con kyc-pe 1.3.0 y se rechaza con kyc-pe 2.0.0.
# Requiere: imagen synkra-core:2.1.0 construida y los JAR de plugins/kyc-pe/build/libs (assemble).
set -euo pipefail

RAIZ="${SMOKE_RAIZ:-$(cd "$(dirname "$0")/.." && pwd)}"
IMAGEN="synkra-core:2.1.0"
TOPE_ESPERA=60   # segundos maximos para la readiness (caso 1) y para que el core termine (caso 2)
JAR_COMPATIBLE="$RAIZ/plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar"
JAR_INCOMPATIBLE="$RAIZ/plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar"
SUFIJO="$$-$(date +%s)"
TMP="$(mktemp -d)"
CONTENEDORES=()
FALLOS=0

limpiar() {
  local c
  for c in "${CONTENEDORES[@]:-}"; do
    [ -n "$c" ] && docker rm -f "$c" >/dev/null 2>&1 || true
  done
  rm -rf "$TMP"
}
trap limpiar EXIT

for f in "$JAR_COMPATIBLE" "$JAR_INCOMPATIBLE"; do
  [ -f "$f" ] || { echo "FALTA $f (corre ./gradlew :plugins:kyc-pe:assemble)" >&2; exit 2; }
done
docker image inspect "$IMAGEN" >/dev/null 2>&1 || { echo "FALTA la imagen $IMAGEN" >&2; exit 2; }

# Prepara /plugins y /config del caso; el usuario 10001 del contenedor debe poder leerlos.
# Formato real de plugins-activos.yaml (C-81): lista en la raiz de {nombre, version}.
preparar() {
  local caso="$1" version="$2" d="$TMP/$1"
  mkdir -p "$d/plugins" "$d/config"
  cp "$JAR_COMPATIBLE" "$JAR_INCOMPATIBLE" "$d/plugins/"
  printf -- '- nombre: kyc-pe\n  version: "%s"\n' "$version" > "$d/config/plugins-activos.yaml"
  chmod 755 "$d" "$d/plugins" "$d/config"
  chmod 644 "$d"/plugins/*.jar "$d/config/plugins-activos.yaml"
}

ok()    { echo "ok $1"; }
falla() { echo "FALLA $1: $2"; FALLOS=$((FALLOS + 1)); }

# Caso 1: kyc-pe 1.3.0 es compatible; readiness 200 y GET /plugins lo lista.
caso1() {
  local caso="caso1-kyc-pe-1.3.0" nombre="smoke-c1-$SUFIJO" t0 estado="" puerto cuerpo
  preparar "$caso" "1.3.0"
  docker run -d --name "$nombre" -p 127.0.0.1::8080 \
    -v "$TMP/$caso/plugins:/plugins:ro" -v "$TMP/$caso/config:/config:ro" "$IMAGEN" >/dev/null
  CONTENEDORES+=("$nombre")
  t0=$SECONDS
  while [ $((SECONDS - t0)) -lt "$TOPE_ESPERA" ]; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$nombre")" != "true" ]; then
      falla "$caso" "el contenedor termino antes de estar listo"; return
    fi
    puerto="$(docker port "$nombre" 8080/tcp | head -n1 | sed 's/.*://')"
    estado="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://127.0.0.1:$puerto/actuator/health/readiness" || true)"
    [ "$estado" = "200" ] && break
    sleep 1
  done
  [ "$estado" = "200" ] || { falla "$caso" "readiness no llego a 200 en ${TOPE_ESPERA}s (ultimo: ${estado:-sin respuesta})"; return; }
  cuerpo="$(curl -s --max-time 5 "http://127.0.0.1:$puerto/plugins")"
  if echo "$cuerpo" | grep -q '"kyc-pe"' && echo "$cuerpo" | grep -q '1\.3\.0'; then
    ok "$caso"
  else
    falla "$caso" "GET /plugins no lista kyc-pe 1.3.0: $cuerpo"
  fi
}

# Caso 2: kyc-pe 2.0.0 exige core 3.x; el core debe terminar con codigo != 0 y VERSION_INCOMPATIBLE.
caso2() {
  local caso="caso2-kyc-pe-2.0.0" nombre="smoke-c2-$SUFIJO" t0 codigo=""
  preparar "$caso" "2.0.0"
  docker run -d --name "$nombre" \
    -v "$TMP/$caso/plugins:/plugins:ro" -v "$TMP/$caso/config:/config:ro" "$IMAGEN" >/dev/null
  CONTENEDORES+=("$nombre")
  t0=$SECONDS
  while [ $((SECONDS - t0)) -lt "$TOPE_ESPERA" ]; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$nombre")" != "true" ]; then
      codigo="$(docker wait "$nombre")"; break
    fi
    sleep 1
  done
  [ -n "$codigo" ] || { falla "$caso" "el core no termino en ${TOPE_ESPERA}s"; return; }
  [ "$codigo" != "0" ] || { falla "$caso" "termino con codigo 0"; return; }
  if docker logs "$nombre" 2>&1 | grep -q 'VERSION_INCOMPATIBLE plugin=kyc-pe'; then
    ok "$caso"
  else
    falla "$caso" "codigo $codigo pero el log no contiene 'VERSION_INCOMPATIBLE plugin=kyc-pe'"
  fi
}

caso1
caso2
[ "$FALLOS" -eq 0 ]
