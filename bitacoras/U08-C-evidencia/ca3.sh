#!/usr/bin/env bash
# CA-3: la guardia frena cada objetivo nuevo con un kubeconfig ajeno, sin aplicar nada. Desde la raiz del worktree, con SynKra instalado.
KC=${KC:-$PWD/.kube/kind-synkra.yaml}
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
falso() { printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "https://127.0.0.1:1"}\ncontexts:\n- name: %s\n  context: {cluster: c, user: u}\ncurrent-context: %s\nusers:\n- name: u\n  user: {token: falso}\n' "$1" "$1"; }
falso otro > "$D/otro.yaml"
foto() { for ns in synkra synkra-co synkra-pe ingress-nginx; do kubectl --kubeconfig "$KC" get all,cm,secret,ingress,pvc,role,rolebinding -n "$ns" -o jsonpath='{range .items[*]}{.kind}/{.metadata.name}={.metadata.resourceVersion} {end}'; done; kubectl --kubeconfig "$KC" get clusterrolebindings -o jsonpath='{range .items[*]}{.metadata.name}={.metadata.resourceVersion} {end}'; }
antes=$(foto)
for obj in aplicar-base ingress tls desplegar verificar-limite install; do
  KUBECONFIG_SYNKRA="$D/otro.yaml" make "$obj" >"$D/out" 2>&1; echo "$obj salida=$?"; grep -c '^contexto no permitido: otro (solo kind-synkra)' "$D/out"
done
sed -n '/^install:/,/^$/p' Makefile
despues=$(foto); [ "$antes" = "$despues" ] && echo "nada-aplicado"
