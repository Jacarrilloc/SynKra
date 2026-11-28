#!/usr/bin/env bash
# CA-6: make verificar-limite da las seis respuestas esperadas, solo pregunta y no se deja enganar por un kubectl roto. Desde la raiz del worktree.
KC=$PWD/.kube/kind-synkra.yaml
foto() { kubectl --kubeconfig "$KC" get sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}'; kubectl --kubeconfig "$KC" get clusterrolebindings,clusterroles -o jsonpath='{range .items[*]}{.metadata.name}={.metadata.resourceVersion} {end}'; }
antes=$(foto)
make verificar-limite; echo "salida=$?"
despues=$(foto); [ "$antes" = "$despues" ] && echo "solo-pregunta"
grep -c -- '--as=system:serviceaccount:synkra:deployer' scripts/verificar-limite.sh
grep -nE 'kubectl[^|]*(create|apply|delete|patch|label|annotate)' scripts/verificar-limite.sh; echo "escrituras=$?"
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
printf '#!/bin/sh\nexit 7\n' > "$D/kubectl"; chmod +x "$D/kubectl"
(PATH="$D:$PATH" KUBECONFIG_SYNKRA="$KC" bash scripts/verificar-limite.sh); echo "roto=$?"
