#!/usr/bin/env bash
# CA-4. Uso: bash bitacoras/U09-A-evidencia/ca4.sh > bitacoras/U09-A-evidencia/ca4.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}} {{.Architecture}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo
set -x
scripts/dotnet-docker.sh build front
grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj; echo "grep-todo=$?"
grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
