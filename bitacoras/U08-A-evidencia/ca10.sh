git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat 8306ee7...HEAD
git diff --name-only 8306ee7...HEAD | grep -v -E '^(Makefile|\.gitignore|deploy/kind/cluster\.yaml|deploy/base/kustomization\.yaml|deploy/base/namespaces\.yaml|deploy/base/rbac\.yaml|scripts/guardia-contexto\.sh|scripts/registro-local\.sh|scripts/verificar-base\.py|bitacoras/U08-A\.md|bitacoras/U08-A-evidencia/.*|revisiones/U08-A/.*)$'; echo "fuera=$?"
git diff 8306ee7...HEAD -- .gitignore | grep -c '^-[^-]'
git ls-files .kube .tls | wc -l
