#!/usr/bin/env bash
# CA-4: make install sobre un kind-synkra recien creado. Desde la raiz del worktree, con JAVA_HOME exportado.
KC=$PWD/.kube/kind-synkra.yaml; R=127.0.0.1:5001
m0=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente)
make cluster-delete; echo "borrar=$?"
echo "# C-130 y puertos antes de crear el cluster (decision 5)"
cat /proc/sys/fs/inotify/max_user_instances
ss -ltnH 'sport = :80 or sport = :443 or sport = :5001'; echo "ss=$?"
time make install; echo "install=$?"
kubectl --kubeconfig "$KC" get nodes --no-headers | awk '{print $1, $2}'
[ "$(kubectl --kubeconfig "$KC" get nodes -o jsonpath='{.items[0].status.nodeInfo.architecture}')" = "$(docker version -f '{{.Server.Arch}}')" ] && echo "misma-arquitectura"
kubectl --kubeconfig "$KC" get ns --no-headers | awk '{print $1}' | grep -E '^synkra'
kubectl --kubeconfig "$KC" -n ingress-nginx get pods -l app.kubernetes.io/component=controller -o jsonpath='{range .items[*]}{.spec.nodeName} {.status.phase} {.status.containerStatuses[0].ready}{"\n"}{end}'
kubectl --kubeconfig "$KC" get ingressclass nginx -o jsonpath='{.spec.controller}'; echo
kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.type}'; echo
[ "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.data.tls\.crt}' | base64 -d | sha256sum)" = "$(sha256sum < .tls/tls.crt)" ] && echo "secreto-es-el-certificado"
stat -c '%a %n' .tls .tls/tls.key; git check-ignore -v .tls/tls.key
kubectl --kubeconfig "$KC" get pods -n synkra -o wide
kubectl --kubeconfig "$KC" -n synkra get pvc synkra-datos -o jsonpath='{.status.phase} {.spec.storageClassName} {.status.capacity.storage}'; echo
P=$(kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-backend -o jsonpath='{.items[0].metadata.name}')
kubectl --kubeconfig "$KC" -n synkra get pod "$P" -o jsonpath='{.spec.serviceAccountName} {.spec.containers[0].image} {.spec.containers[0].imagePullPolicy} {.status.containerStatuses[0].imageID}'; echo
kubectl --kubeconfig "$KC" -n synkra exec "$P" -- id -u
kubectl --kubeconfig "$KC" -n synkra exec "$P" -- ls -ln /data
for repo in synkra-backend synkra-core-ejemplo synkra-init-descarga; do curl -sf "http://$R/v2/$repo/tags/list"; echo; done
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt -o /dev/null -w '%{http_code} %{ssl_verify_result}\n' https://synkra.local/actuator/health
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/actuator/health | jq -c '{status, db: .components.db.status}'
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises | jq -c '[.[] | {pais, lectura}]'
kubectl --kubeconfig "$KC" -n synkra logs "$P" | grep -ciE 'forbidden|unauthorized|read-only file system|permission denied|UnsatisfiedLinkError'
m1=$(stat -c '%Y %s' "$HOME/.kube/config" 2>/dev/null || echo ausente); [ "$m0" = "$m1" ] && echo "kubeconfig-global-intacto"
