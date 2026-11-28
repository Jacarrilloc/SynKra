#!/usr/bin/env bash
# Rojo primero (decision 14): CA-3 y CA-4 sobre una copia del commit rojo (pasado como $1).
set -u
R=$(git rev-parse --show-toplevel); cd "$R"
SHA=${1:?sha del commit rojo}
echo "fecha=$(date -u +%FT%TZ) sha=$(git rev-parse --short "$SHA") arch=$(uname -m)"
echo "imagen=$(docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404)"
git show --stat --format='%h %s' "$SHA"
D=$(mktemp -d); git archive "$SHA" | tar -x -C "$D"; (cd "$D" && git init -q)
TRX="$R/bitacoras/U09-A-evidencia/trx.py"
cd "$D"
echo '$ scripts/dotnet-docker.sh test front.Tests --filter CatalogoTest ...'
scripts/dotnet-docker.sh test front.Tests --filter CatalogoTest --logger "trx;LogFileName=catalogo.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E 'Passed!|Failed!|^\s+Failed '; echo "salida=${PIPESTATUS[0]}"
python3 -I "$TRX" front.Tests/TestResults/catalogo.trx
echo '$ scripts/dotnet-docker.sh test front.Tests --filter "...DetalleTest|...VistaGeneralTest|...MenuTest" ...'
scripts/dotnet-docker.sh test front.Tests --filter "FullyQualifiedName~DetalleTest|FullyQualifiedName~VistaGeneralTest|FullyQualifiedName~MenuTest" --logger "trx;LogFileName=detalle.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E 'Passed!|Failed!|^\s+Failed '; echo "salida=${PIPESTATUS[0]}"
python3 -I "$TRX" front.Tests/TestResults/detalle.trx
cd "$R"; rm -rf "$D"
