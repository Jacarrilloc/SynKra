#!/usr/bin/env bash
# CA-9: los scripts nuevos fallan con mensaje claro y el trap limpia lo que crearon. Desde la raiz del worktree, con SynKra instalado.
bash -n scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh scripts/imagenes.sh; echo "sintaxis=$?"
grep -n 'set -euo pipefail' scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh
grep -n -e 'trap' -e 'mktemp' scripts/secreto-tls.sh
# Desviacion (regla sin-kubeconfig): el grep de la tarea no puede dar 1 porque los mensajes FALLA nombran la orden ("kubectl apply").
# Aqui se quitan solo los argumentos de echo y los comentarios, cada linea se parte en ordenes (por $( ; && || | { ( y `) y toda orden
# que empieza por kubectl tiene que llevar --kubeconfig. Sale con 0 si ENCUENTRA una orden sin --kubeconfig, con 1 si no (como grep).
sin_kc() {
  (cd "${1:-.}" && python3 -I - scripts/secreto-tls.sh scripts/desplegar.sh scripts/verificar-limite.sh <<'PY'
import re, sys
mal = 0
for ruta in sys.argv[1:]:
    for n, linea in enumerate(open(ruta, encoding="utf-8"), 1):
        l = linea.rstrip("\n")
        if l.lstrip().startswith("#"):
            continue
        l = re.sub(r'\becho\s+("([^"\\]|\\.)*"|\'[^\']*\')', "echo", l)
        l = re.sub(r"\s#.*$", "", l)
        for orden in re.split(r"\$\(|&&|\|\||[;|{(`]", l):
            o = re.sub(r"^\s*((if|then|else|elif|do|while|until|time|!)\s+)*", "", orden)
            if re.match(r"kubectl(\s|$)", o) and "--kubeconfig" not in o:
                print("%s:%d: %s" % (ruta, n, linea.strip()))
                mal += 1
sys.exit(0 if mal else 1)
PY
  )
}
sin_kc; echo "sin-kubeconfig=$?"
W=$PWD; KC=$W/.kube/kind-synkra.yaml; D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
SYNKRA_REGISTRO_IMAGENES=127.0.0.1:1 bash scripts/imagenes.sh --subir > "$D/out" 2>&1; echo "registro-caido=$?"; grep -c '^FALLA subir' "$D/out"; grep -c 'docker build' "$D/out"
cat "$D/out"
rv0=$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')
mkdir "$D/c"; git archive HEAD | tar -x -C "$D/c"; mkdir -m 0700 "$D/c/.tls"; cp .tls/tls.crt "$D/c/.tls/"
(cd "$D/c" && KUBECONFIG_SYNKRA="$KC" bash scripts/secreto-tls.sh); echo "media-pareja=$?"; ls -A "$D/c/.tls"
[ "$rv0" = "$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.metadata.resourceVersion}')" ] && echo "secreto-intacto"
# Que la regla sin-kubeconfig muerde: cada mutacion en su copia; todas tienen que dar 0 (detectada)
mut() { # nombre archivo expresion-sed
  local d="$D/mut-$1"; mkdir "$d"; git archive HEAD | tar -x -C "$d"
  cp "$d/$2" "$d/$2.orig"; sed -i "$3" "$d/$2"
  if cmp -s "$d/$2" "$d/$2.orig"; then echo "FALLA mutacion $1: el sed no cambio nada"; return; fi
  rm "$d/$2.orig"; echo "== $1"; diff <(git show "HEAD:$2") "$d/$2" | sed -n '2p;4p'
  sin_kc "$d"; echo "sin-kubeconfig-mutado-$1=$?"
}
mut k scripts/desplegar.sh 's/kubectl --kubeconfig "\$KC" "\$@"/kubectl "$@"/'
mut patron-revisor-echo scripts/secreto-tls.sh 's#^kubectl --kubeconfig "\$KC" apply -f "\$TMP/secreto.yaml" || rc=\$?#kubectl apply -f "$TMP/secreto.yaml" || { echo "FALLA tls: apply"; exit 1; }#'
mut v1-canI scripts/verificar-limite.sh 's/"\$(kubectl --kubeconfig "\$KC" auth/"$(kubectl auth/'
mut v2-tipo scripts/secreto-tls.sh 's/TIPO="\$(kubectl --kubeconfig "\$KC" -n/TIPO="$(kubectl -n/'
mut v3-encluster scripts/secreto-tls.sh 's/EN_CLUSTER="\$(kubectl --kubeconfig "\$KC" -n/EN_CLUSTER="$(kubectl -n/'
mut v4-jsonpath-dq scripts/secreto-tls.sh "s/^kubectl --kubeconfig \"\\\$KC\" apply -f \"\\\$TMP\\/secreto.yaml\" || rc=\\\$?/kubectl get secret -o jsonpath=\"{.type}\" -n synkra x || rc=\$?/"
mut v5-dos-ordenes scripts/secreto-tls.sh 's#^kubectl --kubeconfig "\$KC" apply -f "\$TMP/secreto.yaml" || rc=\$?#kubectl --kubeconfig "$KC" get ns >/dev/null \&\& kubectl apply -f "$TMP/secreto.yaml" || rc=$?#'
mut v6-kc-en-mensaje scripts/secreto-tls.sh 's#^kubectl --kubeconfig "\$KC" apply -f "\$TMP/secreto.yaml" || rc=\$?#kubectl apply -f "$TMP/secreto.yaml" || { echo "falta --kubeconfig"; exit 1; }#'
