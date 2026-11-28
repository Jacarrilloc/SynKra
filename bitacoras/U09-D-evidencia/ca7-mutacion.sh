#!/usr/bin/env bash
# CA-7 (mutacion desplegar-sin-front): en una copia, desplegar.sh solo trata el backend; la variante del front no debe llegar al pod.
W=$PWD; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
sed -i 's/^for D in \$DEPLOYMENTS; do/for D in synkra-backend; do/' "$D/scripts/desplegar.sh"
grep -n '^for D in' "$D/scripts/desplegar.sh"
(cd "$D" && W="$W" KUBECONFIG_SYNKRA="$W/.kube/kind-synkra.yaml" bash "$W/bitacoras/U09-D-evidencia/ca7-variante.sh")
rm -rf "$D"
