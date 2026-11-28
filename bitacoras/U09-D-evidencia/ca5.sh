#!/usr/bin/env bash
# CA-5: U09-T08 sin cluster: los manifiestos de la base y del overlay. Desde la raiz del repositorio (o de una copia).
export KUBECONFIG=/dev/null
kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "base=$?"
kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "backend=$?"
kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py --versiones; echo "versiones=$?"
kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-front.py; echo "front=$?"
kubectl kustomize deploy/base/front | python3 -I scripts/verificar-front.py --base; echo "front-base=$?"
kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base; echo "solo-backend=$?"
kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in os_)); print(sorted((r['host'], p['path'], p['backend']['service']['name']) for o in os_ if o['kind']=='Ingress' for r in o['spec']['rules'] for p in r['http']['paths']))"
grep -nE '0\.1\.0' scripts/*.sh scripts/*.py; echo "literales=$?"
