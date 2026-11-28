#!/usr/bin/env bash
# U09-A: corre «dotnet <args>» en la imagen oficial del SDK, sobre el directorio actual (la raíz del repo o una copia).
# Esta máquina no tiene SDK de .NET. Uso: scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraTest
set -u
IMAGEN='mcr.microsoft.com/dotnet/sdk:10.0.404'
docker run --rm \
  -v "$PWD":/src -w /src \
  -v synkra-nuget:/root/.nuget/packages \
  -e DOTNET_CLI_TELEMETRY_OPTOUT=1 -e DOTNET_NOLOGO=1 -e DOTNET_CLI_UI_LANGUAGE=en \
  -e DUENO="$(id -u):$(id -g)" \
  "$IMAGEN" bash -c 'dotnet "$@"; s=$?; for d in front front.Tests; do [ -d "/src/$d" ] && chown -R "$DUENO" "/src/$d"; done; exit $s' dotnet "$@"
