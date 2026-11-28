#!/usr/bin/env bash
KC=${KC:-$PWD/.kube/kind-synkra.yaml}     # en las copias de CA-8 se pasa el del worktree
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
falso() { printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "https://127.0.0.1:1"}\ncontexts:\n- name: %s\n  context: {cluster: c, user: u}\ncurrent-context: %s\nusers:\n- name: u\n  user: {token: falso}\n' "$1" "$1"; }
falso otro > "$D/otro.yaml"
falso kind-synkra > "$D/mismo-nombre.yaml"
mkdir -p "$D/casa/.kube"; falso otro > "$D/casa/.kube/config"
antes=$(kubectl --kubeconfig "$KC" get ns,sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}')
sha_otro=$(sha256sum "$D/otro.yaml")
for obj in install cluster registro aplicar-base status guardia; do
  KUBECONFIG_SYNKRA="$D/otro.yaml" make "$obj" >"$D/out" 2>&1; echo "$obj otro salida=$?"; grep -c '^contexto no permitido: otro (solo kind-synkra)' "$D/out"
done
KUBECONFIG_SYNKRA="$D/mismo-nombre.yaml" make install >"$D/out" 2>&1; echo "mismo-nombre salida=$?"; grep -c '^contexto no permitido: kind-synkra apunta a https://127.0.0.1:1' "$D/out"
HOME="$D/casa" KUBECONFIG_SYNKRA="$D/casa/.kube/config" make guardia >"$D/out" 2>&1; echo "global salida=$?"; grep -c '^kubeconfig no permitido:' "$D/out"
KUBECONFIG_SYNKRA="$D/no-existe.yaml" make guardia >"$D/out" 2>&1; echo "falta salida=$?"; grep -c '^falta el kubeconfig' "$D/out"
[ "$sha_otro" = "$(sha256sum "$D/otro.yaml")" ] && echo "falso-intacto"
despues=$(kubectl --kubeconfig "$KC" get ns,sa,role,rolebinding -A -l app.kubernetes.io/part-of=synkra -o jsonpath='{range .items[*]}{.metadata.resourceVersion} {end}')
[ "$antes" = "$despues" ] && echo "nada-aplicado"
KUBECONFIG_SYNKRA="$KC" make guardia; echo "real salida=$?"
