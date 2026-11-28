#!/usr/bin/env bash
# CA-3: las escrituras de ApiSynkra contra el handler simulado.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
echo '$ scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraEscriturasTest --logger "trx;LogFileName=escrituras.trx" --results-directory front.Tests/TestResults'
scripts/dotnet-docker.sh test front.Tests --filter ApiSynkraEscriturasTest --logger "trx;LogFileName=escrituras.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Passed|Failed)!|No test|error|warning'; echo "salida=${PIPESTATUS[0]}"
echo
echo '$ python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/escrituras.trx'
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/escrituras.trx
echo
for f in front.Tests/Fixtures/desplegar-plan-pe.json front.Tests/Fixtures/desplegar-veredicto-rechazado.json front.Tests/Fixtures/desplegar-confirmacion.json front.Tests/Fixtures/subida-plugin.json; do printf '%s ' "$f"; python3 -I -c 'import json,sys
def k(o,p=""):
    if isinstance(o,dict):
        for a,b in o.items(): yield a; yield from k(b)
    elif isinstance(o,list):
        for b in o: yield from k(b)
print(sorted(set(k(json.load(open(sys.argv[1]))))))' "$f"; done
