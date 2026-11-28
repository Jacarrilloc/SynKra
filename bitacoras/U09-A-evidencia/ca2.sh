#!/usr/bin/env bash
# CA-2. Uso: bash bitacoras/U09-A-evidencia/ca2.sh > bitacoras/U09-A-evidencia/ca2.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}} {{.Architecture}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo
set -x
rm -rf front.Tests/TestResults
scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraTest --logger "trx;LogFileName=api.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/api.trx
grep -o '"[a-zA-Z0-9]*":' front.Tests/Fixtures/paises-dos.json front.Tests/Fixtures/historial-pe.json front.Tests/Fixtures/despliegue-fallido.json front.Tests/Fixtures/error-422-pais.json | sort -u
grep -nE 'UnmappedMemberHandling\.Disallow|RespectNullableAnnotations = true|RespectRequiredConstructorParameters = true|SnakeCaseLower' front/Api/ApiSynkra.cs
grep -rn 'JsonPropertyName' front/; echo "renombres=$?"
grep -rnE 'Http(Method\.)?(Post|Put|Delete|Patch)|PostAsync|PutAsync|DeleteAsync|PatchAsync|X-Synkra-Usuario' front/ --include='*.cs' --include='*.razor'; echo "escrituras=$?"
grep -n 'Synkra:ApiUrl\|ApiUrl' front/appsettings.json front/Api/ConfiguracionApi.cs
grep -n 'ConfiguracionApi.UrlBase\|AddHttpClient' front/Program.cs
