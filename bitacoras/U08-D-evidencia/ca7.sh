#!/usr/bin/env bash
# CA-7: el script falla con mensaje claro antes de tocar nada. Desde la raiz del worktree, con SynKra instalado.
KC=$PWD/.kube/kind-synkra.yaml
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
git archive HEAD | tar -x -C "$D"
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises/co/despliegues | jq length > "$D/largo-antes"
rv0=$(kubectl --kubeconfig "$KC" -n synkra-co get deploy synkra-core -o jsonpath='{.metadata.resourceVersion}')
(cd "$D" && bash scripts/e2e-api.sh); echo "sin-jars=$?"
KUBECONFIG_SYNKRA="$D/no-existe.yaml" ./scripts/e2e-api.sh; echo "sin-kubeconfig=$?"
grep -n 'command -v' scripts/e2e-api.sh | head -3
echo "== sin herramienta (PATH sin jq)"
mkdir "$D/bin"; for h in curl python3 sha256sum kubectl bash dirname mktemp rm cat head tr; do ln -s "$(command -v $h)" "$D/bin/$h"; done
(cd "$D" && PATH="$D/bin" /bin/bash scripts/e2e-api.sh); echo "sin-jq=$?"
echo "== nada se toco"
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises/co/despliegues | jq length > "$D/largo-despues"
echo "historial antes=$(cat "$D/largo-antes") despues=$(cat "$D/largo-despues")"
echo "resourceVersion del Deployment antes=$rv0 despues=$(kubectl --kubeconfig "$KC" -n synkra-co get deploy synkra-core -o jsonpath='{.metadata.resourceVersion}')"
