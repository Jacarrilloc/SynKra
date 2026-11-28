KC=$PWD/.kube/kind-synkra.yaml
make cluster; echo "salida=$?"
kubectl --kubeconfig "$KC" config current-context
stat -c '%a' "$KC" .kube; git check-ignore -v "$KC"
kubectl --kubeconfig "$KC" get nodes -o json | python3 -I -c "import json,sys; d=json.load(sys.stdin); print(sorted((n['metadata']['name'], [c['status'] for c in n['status']['conditions'] if c['type']=='Ready'][0], n['metadata']['labels'].get('ingress-ready','-'), n['status']['nodeInfo']['architecture']) for n in d['items']))"
docker port synkra-control-plane
A=$(docker inspect -f '{{.State.StartedAt}}' synkra-control-plane); make cluster; echo "salida=$?"; B=$(docker inspect -f '{{.State.StartedAt}}' synkra-control-plane); [ "$A" = "$B" ] && echo "no-recreado"
make -n cluster-delete CLUSTER=gitops2 | grep -c gitops2
grep -nE 'kind get clusters|CLUSTER' Makefile; echo "variable=$?"
