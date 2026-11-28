KC=$PWD/.kube/kind-synkra.yaml
make aplicar-base; echo "salida=$?"
kubectl --kubeconfig "$KC" get ns synkra synkra-co synkra-pe -o jsonpath='{range .items[*]}{.metadata.name} {.metadata.labels.synkra\.io/pais}{"\n"}{end}'
kubectl --kubeconfig "$KC" get role synkra-deployer -n synkra-co -o json | python3 -I -c "import json,sys; print(json.dumps(json.load(sys.stdin)['rules'], sort_keys=True))"
kubectl --kubeconfig "$KC" get clusterrolebindings -o json | python3 -I -c "import json,sys; print([b['metadata']['name'] for b in json.load(sys.stdin)['items'] if any(s.get('kind')=='ServiceAccount' and s.get('name')=='deployer' and s.get('namespace')=='synkra' for s in b.get('subjects') or [])])"
SA=system:serviceaccount:synkra:deployer
for p in "create deployments.apps -n synkra-co" "patch deployments.apps -n synkra-pe" "update configmaps -n synkra-co" "create services -n synkra-pe" "get pods --subresource=log -n synkra-co" "list events -n synkra-pe" "watch pods -n synkra-co" "get namespaces/synkra-co -n synkra-co" \
         "delete deployments.apps -n synkra-co" "create pods -n synkra-co" "get secrets -n synkra-co" "create deployments.apps -n synkra" "create deployments.apps -n default" "create deployments.apps -n kube-system" "create namespaces" "list namespaces" "get namespaces/default -n default" "get namespaces/synkra-pe -n synkra-co"; do
  printf '%s: ' "$p"; kubectl --kubeconfig "$KC" auth can-i $p --as="$SA" 2>/dev/null; done
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
T=$(kubectl --kubeconfig "$KC" create token deployer -n synkra --duration=10m)
S=$(kubectl --kubeconfig "$KC" config view --minify -o jsonpath='{.clusters[0].cluster.server}')
kubectl --kubeconfig "$KC" config view --minify --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > "$D/ca.crt"
printf 'apiVersion: v1\nkind: Config\nclusters:\n- name: c\n  cluster: {server: "%s", certificate-authority: "%s"}\ncontexts:\n- name: deployer\n  context: {cluster: c, user: deployer}\ncurrent-context: deployer\nusers:\n- name: deployer\n  user: {token: "%s"}\n' "$S" "$D/ca.crt" "$T" > "$D/deployer.yaml"
kubectl --kubeconfig "$D/deployer.yaml" get namespace synkra-co -o name; echo "propio=$?"
kubectl --kubeconfig "$D/deployer.yaml" get namespace default -o name; echo "ajeno=$?"
kubectl --kubeconfig "$D/deployer.yaml" get deployments -n synkra-co; echo "lista-pais=$?"
kubectl --kubeconfig "$D/deployer.yaml" get deployments -n synkra; echo "lista-synkra=$?"
kubectl --kubeconfig "$D/deployer.yaml" auth whoami -o jsonpath='{.status.userInfo.username}'; echo
