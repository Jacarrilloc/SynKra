#!/usr/bin/env bash
# CA-4: U09-T04. «Desplegar»: con veredicto incompatible el botón está deshabilitado.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
echo '$ scripts/dotnet-docker.sh test front.Tests --filter DesplegarTest --logger "trx;LogFileName=desplegar.trx" --results-directory front.Tests/TestResults'
scripts/dotnet-docker.sh test front.Tests --filter DesplegarTest --logger "trx;LogFileName=desplegar.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Passed|Failed)!|No test|error|warning'; echo "salida=${PIPESTATUS[0]}"
echo
echo '$ python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/desplegar.trx'
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/desplegar.trx
