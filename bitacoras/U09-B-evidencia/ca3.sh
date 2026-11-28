#!/usr/bin/env bash
# CA-3 catalogo. Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca3.sh > bitacoras/U09-B-evidencia/ca3.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo; set -x
scripts/dotnet-docker.sh test front.Tests --filter CatalogoTest --logger "trx;LogFileName=catalogo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/catalogo.trx
python3 -I -c 'import json,sys; print([(p["nombre"], p["version"], p["coreCompatible"], sorted(p["etiquetas"])) for p in json.load(open(sys.argv[1]))])' front.Tests/Fixtures/catalogo-plugins-synkra-core.json
