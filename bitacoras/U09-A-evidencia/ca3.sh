#!/usr/bin/env bash
# CA-3. Uso: bash bitacoras/U09-A-evidencia/ca3.sh > bitacoras/U09-A-evidencia/ca3.txt 2>&1
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"; echo "imagen: $(docker image inspect --format "{{index .RepoDigests 0}} {{.Architecture}}" mcr.microsoft.com/dotnet/sdk:10.0.404)"; echo
set -x
rm -rf front.Tests/TestResults
scripts/dotnet-docker.sh test front.Tests --filter VistaGeneralTest --logger "trx;LogFileName=vista.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/vista.trx
python3 -I -c 'import json,sys; print([(p["pais"], p["lectura"], p["concuerda"]) for p in json.load(open(sys.argv[1]))])' front.Tests/Fixtures/paises-dos.json
grep -n '@page\|@inject\|VistaGeneralAsync\|HistorialAsync\|DespliegueAsync\|HttpClient' front/Components/Pages/VistaGeneral.razor
grep -rn 'rendermode="InteractiveServer"' front/Components/App.razor
