W=$PWD; KC=$W/.kube/kind-synkra.yaml
bash -n scripts/guardia-contexto.sh scripts/registro-local.sh; echo "sintaxis=$?"
grep -n 'set -euo pipefail' scripts/guardia-contexto.sh scripts/registro-local.sh
grep -n -e 'trap' -e 'docker run' scripts/registro-local.sh
[ "$(docker inspect -f '{{index .Config.Labels "io.synkra.registro"}}' kind-registry)" = true ] && docker rm -f kind-registry
D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
sed -i 's|docker network connect kind kind-registry|docker network connect red-que-no-existe kind-registry|' "$D/scripts/registro-local.sh"
(cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/registro-local.sh); echo "salida=$?"
docker inspect kind-registry >/dev/null 2>&1; echo "quedo=$?"
rm -rf "$D"
make registro; echo "restaurado=$?"
