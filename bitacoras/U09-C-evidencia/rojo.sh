#!/usr/bin/env bash
# Rojo de la decision 13: CA-3 a CA-5 sobre una copia del commit rojo. Uso: rojo.sh <sha-rojo>
set -u
SHA=${1:?sha del commit rojo}
D=$(mktemp -d); git archive "$SHA" | tar -x -C "$D"; (cd "$D" && git init -q)
echo "fecha=$(date -u +%FT%TZ) sha=$SHA uname=$(uname -m)"
echo "RepoDigests=$(docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404)"
echo "== git show --stat $SHA"; git show --stat --format='%h %s' "$SHA"
cd "$D"
for par in "ApiSynkraEscriturasTest escrituras" "DesplegarTest desplegar" "CatalogoTest catalogo" "DetalleTest detalle" "ArranqueTest arranque"; do
  set -- $par
  echo; echo "== \$ scripts/dotnet-docker.sh test front.Tests --filter $1 --logger \"trx;LogFileName=$2.trx\" --results-directory front.Tests/TestResults"
  scripts/dotnet-docker.sh test front.Tests --filter "$1" --logger "trx;LogFileName=$2.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Failed!|Passed!|No test)|error|Warn' ; echo "salida=${PIPESTATUS[0]}"
  echo "== \$ python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/$2.trx"
  python3 -I bitacoras/U09-A-evidencia/trx.py "front.Tests/TestResults/$2.trx"
done
cd /; rm -rf "$D"
