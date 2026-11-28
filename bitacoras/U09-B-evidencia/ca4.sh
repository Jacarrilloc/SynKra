#!/usr/bin/env bash
# CA-4 detalle, vista general y menu. Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca4.sh > bitacoras/U09-B-evidencia/ca4.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo; set -x
scripts/dotnet-docker.sh test front.Tests --filter "FullyQualifiedName~DetalleTest|FullyQualifiedName~VistaGeneralTest|FullyQualifiedName~MenuTest" --logger "trx;LogFileName=detalle.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/detalle.trx
