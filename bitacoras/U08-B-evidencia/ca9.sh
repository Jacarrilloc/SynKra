r() { echo "\$ $*"; eval "$@"; }
r 'git rev-parse --short HEAD; git status --porcelain | wc -l'
r 'git diff --stat 8306ee7...HEAD'
r "git diff --name-only 8306ee7...HEAD | grep -v -E '^(deploy/base/backend/(kustomization|pvc|deployment|service)\.yaml|deploy/overlays/kind/(kustomization|backend-kind|ingress)\.yaml|backend/Dockerfile|backend/\.dockerignore|core/\.dockerignore|init-descarga/Dockerfile|scripts/imagenes\.sh|scripts/verificar-imagenes\.sh|scripts/verificar-backend\.py|scripts/certificado-tls\.sh|bitacoras/U08-B\.md|bitacoras/U08-B-evidencia/.*|revisiones/U08-B/.*)\$'; echo \"fuera=\$?\""
r 'git diff --name-only 8306ee7...HEAD -- Makefile .gitignore deploy/kind deploy/base/kustomization.yaml deploy/base/namespaces.yaml deploy/base/rbac.yaml | wc -l'
r "git ls-files | grep -cE '(^|/)tls\.(crt|key)\$|\.pem\$'"
