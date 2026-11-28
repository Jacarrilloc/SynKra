#!/usr/bin/env bash
# CA-7. Uso, desde la raíz: bash bitacoras/U09-A-evidencia/ca7.sh > bitacoras/U09-A-evidencia/ca7.txt 2>&1
echo "fecha: $(date -Is)"
set -x
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat 8306ee7...HEAD
git diff --name-only 8306ee7...HEAD | grep -v -E '^(front/|front\.Tests/|scripts/dotnet-docker\.sh$|bitacoras/U09-A\.md$|bitacoras/U09-A-evidencia/|revisiones/U09-A/)'; echo "fuera=$?"
git diff --name-only 8306ee7...HEAD -- backend deploy Makefile verificador core plugin-api plugins init-descarga build.gradle.kts settings.gradle.kts .gitignore .gitattributes | wc -l
git diff --name-status --diff-filter=DM 8306ee7...HEAD | wc -l
