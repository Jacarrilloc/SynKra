#!/usr/bin/env bash
# CA-2 compila sin avisos. Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca2.sh > bitacoras/U09-B-evidencia/ca2.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo; set -x
docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404
scripts/dotnet-docker.sh build front.Tests
git diff --name-only "$(git merge-base HEAD main)" HEAD -- '*.csproj' | wc -l
