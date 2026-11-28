#!/usr/bin/env bash
# CA-2: los manifiestos de la union y el Ingress NGINX fijado (sin cluster). Desde la raiz del repositorio.
kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py; echo "base=$?"
kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "overlay=$?"
kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base; echo "solo-backend=$?"
kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print(sorted((o['kind'], o['metadata'].get('namespace','-'), o['metadata']['name']) for o in yaml.safe_load_all(sys.stdin) if o))"
[ "$(sha256sum deploy/ingress-nginx/deploy.yaml | cut -d' ' -f1)" = 2a3ae008c8786431115502644e77ab398fdebfb721a5d1195ed3089cde3299df ] && echo "manifiesto-ok"
I=$(kubectl kustomize deploy/ingress-nginx | python3 -I -c "import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; d=[o for o in os_ if o['kind']=='Deployment'][0]; print(d['metadata']['namespace'], d['metadata']['name'], sorted(d['spec']['template']['spec']['nodeSelector'].items())); imgs=sorted({c['image'] for o in os_ if o['kind'] in ('Deployment','Job') for c in o['spec']['template']['spec']['containers']}); print(len(imgs), all('@sha256:' in i for i in imgs))")
printf '%s\n' "$I"; [ "$I" = "$(printf '%s\n%s' "ingress-nginx ingress-nginx-controller [('ingress-ready', 'true'), ('kubernetes.io/os', 'linux')]" '2 True')" ] && echo "ingress-ok"
grep -nE '0\.1\.0|2\.1\.0|1\.0\.0' Makefile scripts/desplegar.sh scripts/secreto-tls.sh scripts/verificar-limite.sh; echo "literales=$?"
# Desviacion de la tarea: scripts/setup-linux.sh (instalador de herramientas, anterior a U08) nombra amd64/arm64 por necesidad y no es de esta tarea.
grep -cE 'amd64|arm64|x86_64|aarch64' Makefile $(ls scripts/*.sh | grep -v '^scripts/setup-linux\.sh$') deploy/ingress-nginx/kustomization.yaml deploy/ingress-nginx/controlador-kind.yaml | grep -v ':0$'; echo "arquitectura=$?"
