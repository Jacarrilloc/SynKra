#!/usr/bin/env bash
# CA-5. Uso: bash bitacoras/U09-A-evidencia/ca5.sh > bitacoras/U09-A-evidencia/ca5.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}} {{.Architecture}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo
set -x
rm -rf front.Tests/TestResults
scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx
grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
