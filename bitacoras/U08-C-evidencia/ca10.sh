#!/usr/bin/env bash
# CA-10: el diff queda acotado. M es la base de la tarea (05a709e). ca10.txt se genera justo antes de su propio commit, con el arbol limpio.
M=05a709e
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat "$M"...HEAD
git diff --name-only "$M"...HEAD | grep -v -E '^(Makefile|deploy/base/kustomization\.yaml|deploy/overlays/kind/(kustomization|backend-kind)\.yaml|deploy/ingress-nginx/(deploy|kustomization|controlador-kind)\.yaml|scripts/(imagenes|secreto-tls|desplegar|verificar-limite)\.sh|scripts/verificar-(base|backend)\.py|bitacoras/U08-C\.md|bitacoras/U08-C-evidencia/.*|revisiones/U08-C/.*)$'; echo "fuera=$?"
git diff --name-only "$M"...HEAD -- backend core init-descarga plugins deploy/base/namespaces.yaml deploy/base/rbac.yaml deploy/base/backend deploy/kind scripts/guardia-contexto.sh scripts/registro-local.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh | wc -l
git ls-files .kube .tls | wc -l
git ls-files | grep -cE '(^|/)tls\.(crt|key)$|\.pem$'
