#!/usr/bin/env bash
# CA-1 base y archivos comunes. Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca1.sh > bitacoras/U09-B-evidencia/ca1.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo; set -x
B=$(git merge-base HEAD main); git merge-base --is-ancestor 0910a09 "$B"; echo "base=$?"
git diff --stat 0910a09 "$B" -- front front.Tests scripts/dotnet-docker.sh | wc -l
git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
git diff -U0 "$B" HEAD -- front/Api/ApiSynkra.cs | grep -E '^[-+][^-+]'
