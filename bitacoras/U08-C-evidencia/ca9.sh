#!/usr/bin/env bash
# CA-9: los scripts nuevos fallan con mensaje claro y el trap limpia lo que crearon. Desde la raiz del worktree, con SynKra instalado.
bash -n scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh scripts/imagenes.sh; echo "sintaxis=$?"
grep -n 'set -euo pipefail' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh
grep -n -e 'trap' -e 'mktemp' scripts/secreto-tls.sh
# Desviacion: el texto entre comillas (los mensajes FALLA nombran la orden que fallo, por ejemplo "kubectl apply") se quita antes de buscar;
# una orden real sin --kubeconfig se ve aunque este en una linea "... || { echo ...; }". Los comentarios se ignoran.
sin_kc() { (cd "${1:-.}" && grep -nE '.' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh | sed -E 's/"[^"]*"//g' | grep -E 'kubectl ' | grep -vF -e '--kubeconfig' | grep -vE '^[^:]*:[0-9]*:[[:space:]]*#'); }
sin_kc; echo "sin-kubeconfig=$?"
W=$PWD; KC=$W/.kube/kind-synkra.yaml; D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
SYNKRA_REGISTRO_IMAGENES=127.0.0.1:1 bash scripts/imagenes.sh --subir > "$D/out" 2>&1; echo "registro-caido=$?"; grep -c '^FALLA subir' "$D/out"; grep -c 'docker build' "$D/out"
cat "$D/out"
rv0=$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')
mkdir "$D/c"; git archive HEAD | tar -x -C "$D/c"; mkdir -m 0700 "$D/c/.tls"; cp .tls/tls.crt "$D/c/.tls/"
(cd "$D/c" && KUBECONFIG_SYNKRA="$KC" bash scripts/secreto-tls.sh); echo "media-pareja=$?"; ls -A "$D/c/.tls"
[ "$rv0" = "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')" ] && echo "secreto-intacto"
# Que la regla sin-kubeconfig muerde: (1) el envoltorio k() de desplegar.sh sin --kubeconfig; (2) el patron del revisor en secreto-tls.sh
mkdir "$D/m" "$D/n"; git archive HEAD | tar -x -C "$D/m"; git archive HEAD | tar -x -C "$D/n"
sed -i 's/kubectl --kubeconfig "\$KC" "\$@"/kubectl "$@"/' "$D/m/scripts/desplegar.sh"
sin_kc "$D/m"; echo "sin-kubeconfig-mutado-k=$?"
sed -i 's#^kubectl --kubeconfig "\$KC" apply -f "\$TMP/secreto.yaml" || rc=\$?#kubectl apply -f "$TMP/secreto.yaml" || { echo "FALLA tls: apply"; exit 1; }#' "$D/n/scripts/secreto-tls.sh"
diff <(git show HEAD:scripts/secreto-tls.sh) "$D/n/scripts/secreto-tls.sh"
sin_kc "$D/n"; echo "sin-kubeconfig-mutado-patron-revisor=$?"
