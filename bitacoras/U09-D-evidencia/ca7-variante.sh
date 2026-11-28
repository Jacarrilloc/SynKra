#!/usr/bin/env bash
# CA-7 (variante): la misma etiqueta de synkra-front con otro contenido. W es el worktree real (para restaurar aunque se corra en una copia).
W=${W:-$PWD}; KC=${KUBECONFIG_SYNKRA:-$W/.kube/kind-synkra.yaml}; R=127.0.0.1:5001
A='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
IMG=$(kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print([o for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment' and o['metadata']['name']=='synkra-front'][0]['spec']['template']['spec']['containers'][0]['image'])")
TAG=${IMG##*:}
registro() { curl -sfI -H "Accept: $A" "http://$R/v2/synkra-front/manifests/$TAG" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}'; }
corre() { kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-front -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' | sed 's/.*@//'; }
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
printf 'FROM %s\nLABEL io.synkra.prueba-u09d="%s"\n' "$IMG" "$(date +%s)" > "$D/Dockerfile"
docker build -q --provenance=false --sbom=false -t "$IMG" "$D" > /dev/null && docker push -q "$IMG" > /dev/null; echo "variante=$?"
v=$(registro); [ "$v" != "$(corre)" ] && echo "registro-cambio"
KUBECONFIG_SYNKRA="$KC" make desplegar; echo "desplegar=$?"
[ "$v" = "$(corre)" ] && echo "corre-la-variante"
KUBECONFIG_SYNKRA="$KC" make -C "$W" imagenes desplegar > /dev/null; echo "restaurado=$?"
[ "$(registro)" = "$(corre)" ] && echo "corre-lo-subido"
