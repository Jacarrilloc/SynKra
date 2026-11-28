# Uso: ca8.sh <mutacion>   (desde la raiz del worktree, con el arbol limpio y commiteado)
# Una copia de HEAD por mutacion; la mutacion se imprime literal.
W=$PWD; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
M=$1
case $M in
  guardia-nombre)    echo "sed: paso 3 acepta cualquier contexto"; sed -i 's|if \[ "\$CTX" != "kind-synkra" \]; then|if false; then|' "$D/scripts/guardia-contexto.sh";;
  guardia-servidor)  echo "sed: paso 4 (comparacion de servidores) fuera"; sed -i 's|if \[ "\$SERVIDOR" != "\$SERVIDOR_KIND" \]; then|if false; then|' "$D/scripts/guardia-contexto.sh";;
  guardia-global)    echo "sed: paso 1 (rechazo de ~/.kube/config) fuera"; sed -i 's|if \[ "\$RUTA" = "\$GLOBAL" \]; then|if false; then|' "$D/scripts/guardia-contexto.sh";;
  receta-sin-guardia) echo "sed: aplicar-base: guardia -> aplicar-base:"; sed -i 's|^aplicar-base: guardia ##|aplicar-base: ##|' "$D/Makefile";;
  nombre-variable)   echo "sed: vuelve CLUSTER ?= synkra y cluster-delete usa --name \$(CLUSTER)"; sed -i 's|^COUNTRY ?= co|CLUSTER ?= synkra\nCOUNTRY ?= co|; s|kind delete cluster --name synkra|kind delete cluster --name $(CLUSTER)|' "$D/Makefile";;
  rbac-delete)       echo "sed: el Role de synkra-co gana delete en deployments"; sed -i '0,/verbs: \["get", "list", "watch", "create", "update", "patch"\]/s//verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]/' "$D/deploy/base/rbac.yaml";;
  rbac-clusterrole)  echo "append: ClusterRole y ClusterRoleBinding para deployer"; printf -- '---\napiVersion: rbac.authorization.k8s.io/v1\nkind: ClusterRole\nmetadata:\n  name: x\nrules: []\n---\napiVersion: rbac.authorization.k8s.io/v1\nkind: ClusterRoleBinding\nmetadata:\n  name: x\nroleRef: {apiGroup: rbac.authorization.k8s.io, kind: ClusterRole, name: x}\nsubjects:\n- {kind: ServiceAccount, name: deployer, namespace: synkra}\n' >> "$D/deploy/base/rbac.yaml";;
  ns-etiqueta)       echo "sed: synkra-pe pierde synkra.io/pais"; sed -i '/synkra.io\/pais: pe/d' "$D/deploy/base/namespaces.yaml";;
esac
diff -u <(git show HEAD:Makefile; git show HEAD:scripts/guardia-contexto.sh; git show HEAD:deploy/base/rbac.yaml; git show HEAD:deploy/base/namespaces.yaml) <(cat "$D/Makefile" "$D/scripts/guardia-contexto.sh" "$D/deploy/base/rbac.yaml" "$D/deploy/base/namespaces.yaml") | grep -E '^[+-][^+-]' 
case $M in
  guardia-*|receta-sin-guardia) (cd "$D" && KC="$W/.kube/kind-synkra.yaml" bash "$W/bitacoras/U08-A-evidencia/ca4.sh");;
  nombre-variable) (cd "$D" && make -n cluster-delete CLUSTER=gitops2 | grep -c gitops2);;
  *) (cd "$D" && kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "salida=${PIPESTATUS[1]}");;
esac
rm -rf "$D"
