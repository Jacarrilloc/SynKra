#!/usr/bin/env bash
# CA-1: se corre desde la raíz del repositorio. Uso: bash bitacoras/U09-A-evidencia/ca1.sh > bitacoras/U09-A-evidencia/ca1.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}} {{.Architecture}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo
set -x
docker pull -q mcr.microsoft.com/dotnet/sdk:10.0.404
docker image inspect --format '{{index .RepoDigests 0}} {{.Architecture}}' mcr.microsoft.com/dotnet/sdk:10.0.404
scripts/dotnet-docker.sh --version
scripts/dotnet-docker.sh build front.Tests
grep -c '<TargetFramework>net10.0</TargetFramework>' front/Synkra.Front.csproj front.Tests/Synkra.Front.Tests.csproj
grep -c 'PackageReference' front/Synkra.Front.csproj
grep -o 'PackageReference Include="[^"]*" Version="[^"]*"' front.Tests/Synkra.Front.Tests.csproj | sort
grep -c '<TreatWarningsAsErrors>true</TreatWarningsAsErrors>' front/Synkra.Front.csproj front.Tests/Synkra.Front.Tests.csproj
grep -c "^IMAGEN='mcr.microsoft.com/dotnet/sdk:10.0.404'$" scripts/dotnet-docker.sh; test -x scripts/dotnet-docker.sh && echo ejecutable
git ls-files front front.Tests | grep -iE '(^|/)(bin|obj|TestResults)/|wwwroot/lib/|Counter|Weather'; echo "sobrantes=$?"
command -v dotnet; echo "dotnet-local=$?"
