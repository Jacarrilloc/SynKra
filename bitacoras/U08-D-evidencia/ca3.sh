#!/usr/bin/env bash
# CA-3: en un kind nuevo, make install && make verificar-limite && ./scripts/e2e-api.sh termina con 0.
KC=$PWD/.kube/kind-synkra.yaml
./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble; echo "jars=$?"
make cluster-delete; echo "borrar=$?"
make install; echo "install=$?"
make verificar-limite; echo "limite=$?"
time ./scripts/e2e-api.sh; echo "e2e=$?"
kubectl --kubeconfig "$KC" get deploy,cm,svc,pods -n synkra-co -o wide
kubectl --kubeconfig "$KC" get deploy synkra-core -n synkra-co -o jsonpath='{range .metadata.managedFields[*]}{.manager} {.operation}{"\n"}{end}'
kubectl --kubeconfig "$KC" get deploy -n synkra-co -o jsonpath='{..image}'; echo
kubectl --kubeconfig "$KC" get --raw /api/v1/namespaces/synkra-co/services/synkra-core:http/proxy/plugins; echo
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises/co/despliegues | jq -c '[.[] | {id, tipo, origenId, estado, creadoPor, confirmadoPor}]'
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId, plugins}'
kubectl --kubeconfig "$KC" get deploy,cm,svc -n synkra-pe --no-headers 2>&1 | wc -l
