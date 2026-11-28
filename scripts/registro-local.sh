#!/usr/bin/env bash
# Registro local kind-registry en 127.0.0.1:5001, conectado a la red kind y
# configurado en los nodos del cluster synkra (localhost:5001 -> kind-registry:5000).
set -euo pipefail

REGISTRO=kind-registry
IMAGEN=registry@sha256:1be55279f18a2fe1a74edf2664cac61c1bea305b7b4642dab412e7affdcb3e33
ETIQUETA=io.synkra.registro

CREADO_AQUI=0
limpiar() {
  local rc=$?
  if [ "$rc" -ne 0 ] && [ "$CREADO_AQUI" -eq 1 ]; then
    echo "limpieza: se borra $REGISTRO, que creó esta corrida" >&2
    docker rm --force "$REGISTRO" >/dev/null 2>&1 || true
  fi
}
trap limpiar EXIT

# Corre un comando externo; si falla, dice cuál y con qué código.
correr() {
  local rc=0
  "$@" || rc=$?
  if [ "$rc" -ne 0 ]; then
    echo "error: $* falló con código $rc" >&2
    exit 1
  fi
}

if docker inspect "$REGISTRO" >/dev/null 2>&1; then
  if [ "$(docker inspect -f '{{index .Config.Labels "'"$ETIQUETA"'"}}' "$REGISTRO")" != "true" ]; then
    echo "$REGISTRO existe y no es de SynKra: no se toca" >&2
    exit 1
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$REGISTRO")" != "true" ]; then
    correr docker start "$REGISTRO" >/dev/null
  fi
else
  CREADO_AQUI=1
  correr docker run -d --restart=always --name "$REGISTRO" --label "$ETIQUETA=true" -p 127.0.0.1:5001:5000 "$IMAGEN" >/dev/null
fi

if ! docker inspect -f '{{json .NetworkSettings.Networks}}' "$REGISTRO" | grep -q '"kind"'; then
  correr docker network connect kind kind-registry
fi

NODOS=$(kind get nodes --name synkra) || { echo "error: kind get nodes --name synkra falló con código $?" >&2; exit 1; }
if [ -z "$NODOS" ]; then
  echo "error: el clúster kind synkra no tiene nodos: corre make cluster" >&2
  exit 1
fi
for nodo in $NODOS; do
  correr docker exec "$nodo" mkdir -p /etc/containerd/certs.d/localhost:5001
  printf '[host."http://kind-registry:5000"]\n' | correr docker exec -i "$nodo" sh -c 'cat > /etc/containerd/certs.d/localhost:5001/hosts.toml'
done

for _ in $(seq 1 30); do
  if curl -sf --max-time 2 http://127.0.0.1:5001/v2/ >/dev/null; then
    echo "registro listo en 127.0.0.1:5001"
    exit 0
  fi
  sleep 1
done
echo "error: el registro no responde en http://127.0.0.1:5001/v2/ tras 30 s" >&2
exit 1
