#!/usr/bin/env bash
# CA-2: compila sin avisos y sin paquetes nuevos; las escrituras solo en su archivo.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
echo '$ docker image inspect --format ... mcr.microsoft.com/dotnet/sdk:10.0.404'
docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404
echo
echo '$ scripts/dotnet-docker.sh build front.Tests --no-incremental'
scripts/dotnet-docker.sh build front.Tests --no-incremental 2>&1 | grep -E 'Build succeeded|Warning\(s\)|Error\(s\)|error|warning'
echo
echo "\$ git diff --name-only \$(git merge-base HEAD main) HEAD -- '*.csproj' | wc -l"
git diff --name-only "$(git merge-base HEAD main)" HEAD -- '*.csproj' | wc -l
echo
echo "\$ grep -rlE 'Http(Method\\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario|MultipartFormDataContent|HttpRequestMessage|HttpClient' front/ --include='*.cs' --include='*.razor' | sort"
grep -rlE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario|MultipartFormDataContent|HttpRequestMessage|HttpClient' front/ --include='*.cs' --include='*.razor' | sort
