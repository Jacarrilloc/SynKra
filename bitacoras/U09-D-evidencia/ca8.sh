#!/usr/bin/env bash
# CA-8: el diff queda acotado. Base: origin/main (este worktree no tiene rama main local). Con el arbol limpio.
B=$(git merge-base HEAD origin/main)
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat "$B"...HEAD
git diff --name-only "$B"...HEAD | grep -v -E '^(front/Dockerfile|front/\.dockerignore|front/Synkra\.Front\.csproj|deploy/base/front/(kustomization|deployment|service)\.yaml|deploy/base/kustomization\.yaml|deploy/overlays/kind/(kustomization|front-kind|ingress-front)\.yaml|scripts/(imagenes|verificar-imagenes|desplegar)\.sh|scripts/verificar-(base|backend|front)\.py|bitacoras/U09-D\.md|bitacoras/U09-D-evidencia/.*|revisiones/U09-D/.*)$'; echo "fuera=$?"
git diff -U0 "$B"...HEAD -- front/Synkra.Front.csproj | grep -E '^[-+][^-+]'
git diff --name-only "$B"...HEAD -- front/Api front/Components front/Program.cs front/appsettings.json front.Tests backend core init-descarga plugins deploy/base/backend deploy/base/namespaces.yaml deploy/base/rbac.yaml deploy/kind deploy/ingress-nginx deploy/overlays/kind/ingress.yaml deploy/overlays/kind/backend-kind.yaml Makefile | wc -l
git ls-files .kube .tls | wc -l
