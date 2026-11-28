#!/usr/bin/env bash
# CA-9: los scripts nuevos fallan con mensaje claro y el trap limpia lo que crearon. Desde la raiz del worktree, con SynKra instalado.
bash -n scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh scripts/imagenes.sh; echo "sintaxis=$?"
grep -n 'set -euo pipefail' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh
grep -n -e 'trap' -e 'mktemp' scripts/secreto-tls.sh
# Desviacion: se excluyen los comentarios y los echo (los mensajes FALLA nombran la orden que fallo, por ejemplo "kubectl apply"): solo cuentan las ordenes.
grep -nE 'kubectl ' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh | grep -vF -e '--kubeconfig' | grep -vE '^[^:]*:[0-9]*:[[:space:]]*(#|echo )' | grep -vE '\|\| *\{ echo|\|\| *\{ *echo'; echo "sin-kubeconfig=$?"
W=$PWD; KC=$W/.kube/kind-synkra.yaml; D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
SYNKRA_REGISTRO_IMAGENES=127.0.0.1:1 bash scripts/imagenes.sh --subir > "$D/out" 2>&1; echo "registro-caido=$?"; grep -c '^FALLA subir' "$D/out"; grep -c 'docker build' "$D/out"
cat "$D/out"
rv0=$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')
mkdir "$D/c"; git archive HEAD | tar -x -C "$D/c"; mkdir -m 0700 "$D/c/.tls"; cp .tls/tls.crt "$D/c/.tls/"
(cd "$D/c" && KUBECONFIG_SYNKRA="$KC" bash scripts/secreto-tls.sh); echo "media-pareja=$?"; ls -A "$D/c/.tls"
[ "$rv0" = "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')" ] && echo "secreto-intacto"
# Que la regla sin-kubeconfig muerde: la copia pierde --kubeconfig en el envoltorio k() de desplegar.sh
mkdir "$D/m"; git archive HEAD | tar -x -C "$D/m"
sed -i 's/kubectl --kubeconfig "\$KC" "\$@"/kubectl "$@"/' "$D/m/scripts/desplegar.sh"
(cd "$D/m" && grep -nE 'kubectl ' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh | grep -vF -e '--kubeconfig' | grep -vE '^[^:]*:[0-9]*:[[:space:]]*(#|echo )' | grep -vE '\|\| *\{ *echo'; echo "sin-kubeconfig-mutado=$?")
