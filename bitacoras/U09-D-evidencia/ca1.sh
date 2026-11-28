#!/usr/bin/env bash
# CA-1: la base es la union de U09-B (5d0a48d) y U09-C (3dfda60), y en ella pasan los criterios de U09-T03, T04 y T05. Desde la raiz del worktree.
git merge-base --is-ancestor 5d0a48d HEAD; echo "b=$?"
git merge-base --is-ancestor 3dfda60 HEAD; echo "c=$?"
git rev-parse HEAD:front/Api/ApiSynkra.cs HEAD:front/Api/ModelosCatalogo.cs HEAD:front/Api/ApiSynkra.Catalogo.cs
rm -rf front.Tests/TestResults
for f in CatalogoTest DesplegarTest DetalleTest; do
  scripts/dotnet-docker.sh test front.Tests --filter "$f" --logger "trx;LogFileName=$f.trx" --results-directory front.Tests/TestResults > /dev/null; echo "$f salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py "front.Tests/TestResults/$f.trx" | head -1
done
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/CatalogoTest.trx | grep -c -x 'DescriptorInvalidoMuestraElMotivo'
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/DesplegarTest.trx | grep -c -x 'ConVeredictoRechazadoLosBotonesEstanDeshabilitados'
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/DetalleTest.trx | grep -c -x 'LaVueltaSoloSeConfirmaDespuesDeConfirmar'
scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults > /dev/null; echo "todo=$?"
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\bbool |\.)(Core)?Compatible\b'; echo "resto=$?"
