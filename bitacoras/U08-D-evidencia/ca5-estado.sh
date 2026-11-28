#!/usr/bin/env bash
# Tras el rojo de CA-5: el ConfigMap ya lista los dos plugins y el core sigue cargando uno.
KC=$PWD/.kube/kind-synkra.yaml
kubectl --kubeconfig "$KC" get cm synkra-core-config -n synkra-co -o jsonpath='{.data.plugins-activos\.yaml}'; echo
kubectl --kubeconfig "$KC" get --raw /api/v1/namespaces/synkra-co/services/synkra-core:http/proxy/plugins; echo
kubectl --kubeconfig "$KC" get pods -n synkra-co --no-headers
kubectl --kubeconfig "$KC" get deploy synkra-core -n synkra-co -o jsonpath='{.spec.template.metadata.annotations}'; echo "<-anotaciones"
curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId, plugins}'
