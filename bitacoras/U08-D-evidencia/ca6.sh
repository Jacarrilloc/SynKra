#!/usr/bin/env bash
# CA-6: cada comprobacion del script muerde (en copias, contra el cluster real). Desde la raiz del worktree, con SynKra instalado.
W=$PWD; KC=$W/.kube/kind-synkra.yaml
mutar() { # nombre expresiones-sed...: copia, muta, corre y restaura
  local n="$1"; shift
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  cp -r .tls "$D/"; ln -s "$W/plugins/kyc-co/build" "$D/plugins/kyc-co/build"; ln -s "$W/plugins/kyc-pe/build" "$D/plugins/kyc-pe/build"
  mkdir -p "$D/.kube"; ln -s "$KC" "$D/.kube/kind-synkra.yaml"
  cp "$D/scripts/e2e-api.sh" "$D/orig"
  local -a args=(); local e; for e in "$@"; do args+=(-e "$e"); done
  sed -i "${args[@]}" "$D/scripts/e2e-api.sh"
  echo "################ mutacion $n"; diff "$D/orig" "$D/scripts/e2e-api.sh"
  (cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/e2e-api.sh) 2>&1 | grep -v '^ok ' | tail -8; echo "salida=${PIPESTATUS[0]}"
  rm -rf "$D"
  kubectl --kubeconfig "$KC" get cm synkra-core-config -n synkra-co -o jsonpath='{.metadata.annotations}'; echo "<-anotaciones del ConfigMap"
  ./scripts/e2e-api.sh 2>&1 | tail -3; echo "restaurado=${PIPESTATUS[0]}"
}
ANOT='kubectl --kubeconfig "$KC" -n synkra-co annotate configmap synkra-core-config'
mutar cu4-llega "/^provocada 7\$/a $ANOT synkra.io/e2e-mutacion=1 --overwrite >/dev/null" "s|^limpiar() { |limpiar() { $ANOT synkra.io/e2e-mutacion- >/dev/null 2>\&1 \|\| true; |"
mutar cuerpo-ajeno '/^provocada 3$/a pedir GET /noexiste\nespero 404'
mutar sin-vuelta '/^confirmar "\$D5"$/d'
mutar codigo-otro 's/^espero 422 COMBINACION_RECHAZADA$/espero 422 OTRO_CODIGO/'
