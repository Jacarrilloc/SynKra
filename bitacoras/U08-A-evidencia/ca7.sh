KC=$PWD/.kube/kind-synkra.yaml
m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
docker inspect -f '{{.Name}} {{.State.Running}}' gitops2-control-plane m08-gitops1-control-plane
make cluster-delete; echo "borrar=$?"
kind get kubeconfig --name synkra >/dev/null 2>&1; echo "existe=$?"
docker inspect kind-registry >/dev/null 2>&1; echo "registro=$?"
time make install; echo "install=$?"
kubectl --kubeconfig "$KC" get nodes --no-headers | awk '{print $1, $2}'
kubectl --kubeconfig "$KC" get ns synkra synkra-co synkra-pe --no-headers | awk '{print $1, $2}'
curl -sf http://127.0.0.1:5001/v2/; echo " v2=$?"
docker inspect -f '{{.Name}} {{.State.Running}}' gitops2-control-plane m08-gitops1-control-plane
m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
