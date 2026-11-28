#!/usr/bin/env bash
# CA-1: la base es la de U09-A y los tres archivos comunes son los acordados con U09-B.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
echo '$ B=$(git merge-base HEAD main); git merge-base --is-ancestor 0910a09 "$B"; echo "base=$?"'
B=$(git merge-base HEAD main); git merge-base --is-ancestor 0910a09 "$B"; echo "base=$?"
echo '$ git diff --stat 0910a09 "$B" -- front front.Tests scripts/dotnet-docker.sh | wc -l'
git diff --stat 0910a09 "$B" -- front front.Tests scripts/dotnet-docker.sh | wc -l
echo '$ git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs'
git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
echo "\$ git diff -U0 \"\$B\" HEAD -- front/Api/ApiSynkra.cs | grep -E '^[-+][^-+]'   (B=$B)"
git diff -U0 "$B" HEAD -- front/Api/ApiSynkra.cs | grep -E '^[-+][^-+]'
