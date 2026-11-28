kubectl kustomize deploy/base | grep -c 'kind: Namespace'
kubectl kustomize deploy/base | grep -cE 'kind: Cluster(Role|RoleBinding)'
kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "salida=$?"
kubectl kustomize deploy/base | python3 -I -c "import sys,yaml; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in yaml.safe_load_all(sys.stdin) if o))"
grep -nE "delete|secrets|'\*'|\"\*\"|- \*" deploy/base/*.yaml; echo "prohibidos=$?"
