#!/usr/bin/env bash
# CA-7: make install deja synkra-front Running y servido en https://synkra.local/. Desde la raiz del worktree, con JAVA_HOME exportado.
KC=$PWD/.kube/kind-synkra.yaml; R=127.0.0.1:5001
CURL='curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt'
m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
make cluster-delete; echo "borrar=$?"
time make install; echo "install=$?"
kubectl --kubeconfig "$KC" -n synkra get deploy,svc,ingress -o wide
P=$(kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-front -o json | python3 -I -c "import sys,json; print([p['metadata']['name'] for p in json.load(sys.stdin)['items'] if not p['metadata'].get('deletionTimestamp') and p['status'].get('phase')=='Running'][0])")
kubectl --kubeconfig "$KC" -n synkra get pod "$P" -o jsonpath='{.status.phase} {.status.containerStatuses[0].ready} {.spec.containers[0].image} {.spec.containers[0].imagePullPolicy} {.spec.automountServiceAccountToken} {.status.containerStatuses[0].imageID}'; echo
kubectl --kubeconfig "$KC" -n synkra exec "$P" -- id -u
curl -sf "http://$R/v2/synkra-front/tags/list"; echo
$CURL -o /dev/null -w '%{http_code} %{ssl_verify_result}\n' https://synkra.local/
$CURL https://synkra.local/ | grep -o -e '<h1>Vista general</h1>' -e 'data-pais="[a-z]*"' | sort -u
$CURL https://synkra.local/api/paises | jq -c '[.[] | .pais]'
$CURL https://synkra.local/actuator/health | jq -r .status
$CURL -X POST -o /dev/null -w '%{http_code}\n' 'https://synkra.local/_blazor/negotiate?negotiateVersion=1'
kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -c 'http://synkra-backend.synkra.svc:8080/api/paises'
kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -ciE 'read-only file system|permission denied|unhandled exception'
m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
