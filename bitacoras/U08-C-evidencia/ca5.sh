#!/usr/bin/env bash
# CA-5 (idempotencia): make install por segunda vez, con SynKra ya instalado. Desde la raiz del worktree, con JAVA_HOME.
KC=${KUBECONFIG_SYNKRA:-$PWD/.kube/kind-synkra.yaml}; R=127.0.0.1:5001
A='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
IMG=$(kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print([o for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment'][0]['spec']['template']['spec']['containers'][0]['image'])")
TAG=${IMG##*:}
registro() { curl -sfI -H "Accept: $A" "http://$R/v2/synkra-backend/manifests/$TAG" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}'; }
corre() { kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-backend -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' | sed 's/.*@//'; }
c0=$(sha256sum < .tls/tls.crt)
make install > /dev/null; echo "segunda=$?"
[ "$c0" = "$(sha256sum < .tls/tls.crt)" ] && echo "certificado-igual"
[ "$(registro)" = "$(corre)" ] && echo "corre-lo-subido"
