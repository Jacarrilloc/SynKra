#!/usr/bin/env bash
# CA-3, ultima linea: lo que hay en synkra-pe. El conteo de la tarea (0) no cuenta con kube-root-ca.crt, que Kubernetes crea en todo namespace.
KC=$PWD/.kube/kind-synkra.yaml
kubectl --kubeconfig "$KC" get deploy,cm,svc -n synkra-pe -o name
echo "sin kube-root-ca.crt: $(kubectl --kubeconfig "$KC" get deploy,cm,svc -n synkra-pe -o name | grep -vc 'configmap/kube-root-ca.crt')"
