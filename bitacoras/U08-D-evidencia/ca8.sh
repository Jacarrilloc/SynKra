#!/usr/bin/env bash
# CA-8: el diff queda acotado. ca8.txt se genera justo antes de su propio commit, con el arbol limpio (salvo ca8.txt).
BASE=367e09f
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat "$BASE"...HEAD
git diff --name-only "$BASE"...HEAD | grep -v -E '^(scripts/e2e-api\.sh|backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais\.java|backend/src/test/java/io/synkra/kubernetes/ConstruirObjetosPaisTest\.java|backend/src/test/resources/kubernetes/pais-pe-esperado\.yaml|bitacoras/U08-D\.md|bitacoras/U08-D-evidencia/.*|revisiones/U08-D/.*)$'; echo "fuera=$?"
git diff --name-only "$BASE"...HEAD -- Makefile deploy scripts/guardia-contexto.sh scripts/registro-local.sh scripts/imagenes.sh scripts/desplegar.sh scripts/secreto-tls.sh scripts/verificar-limite.sh | wc -l
git ls-files .kube .tls | wc -l
