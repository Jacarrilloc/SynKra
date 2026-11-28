#!/usr/bin/env bash
# CA-5: las escrituras de U09-T03 y U09-T05 con los filtros del plan, y el arranque.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
for par in "CatalogoTest catalogo" "DetalleTest detalle" "ArranqueTest arranque"; do
  set -- $par
  echo "\$ scripts/dotnet-docker.sh test front.Tests --filter $1 --logger \"trx;LogFileName=$2.trx\" --results-directory front.Tests/TestResults"
  scripts/dotnet-docker.sh test front.Tests --filter "$1" --logger "trx;LogFileName=$2.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Passed|Failed)!|No test|error|warning'; echo "$2=${PIPESTATUS[0]}"
  echo "\$ python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/$2.trx"
  python3 -I bitacoras/U09-A-evidencia/trx.py "front.Tests/TestResults/$2.trx"
  echo
done
echo '$ ls front | grep -c "^core"   (ningún volcado de memoria en front/)'
ls front | grep -c '^core'
echo '$ ps aux | grep -c "[S]ynkra.Front.dll"   (ningún proceso de la aplicación queda vivo)'
ps aux | grep -c '[S]ynkra.Front.dll'
