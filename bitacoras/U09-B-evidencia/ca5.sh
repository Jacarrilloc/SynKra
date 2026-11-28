#!/usr/bin/env bash
# CA-5 suite completa, T06, escrituras. Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca5.sh > bitacoras/U09-B-evidencia/ca5.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo; set -x
rm -rf front.Tests/TestResults
scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\.)CoreCompatible\b'; echo "resto=$?"
grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
grep -rnE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario|MultipartFormDataContent' front/ --include='*.cs' --include='*.razor'; echo "escrituras=$?"
