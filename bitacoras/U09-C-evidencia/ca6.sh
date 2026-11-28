#!/usr/bin/env bash
# CA-6: toda la suite pasa, sin saltos, y U09-T06.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
rm -rf front.Tests/TestResults
echo '$ rm -rf front.Tests/TestResults; scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults'
scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Passed|Failed)!|No test|error|warning'; echo "salida=${PIPESTATUS[0]}"
echo
echo '$ python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1'
python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
echo
echo "\$ grep -rnE '\\[(Fact|Theory)\\([^)]*Skip|\\[Theory' front.Tests --include='*.cs'; echo saltos=\$?"
grep -rnE '\[(Fact|Theory)\([^)]*Skip|\[Theory' front.Tests --include='*.cs'; echo "saltos=$?"
echo
echo "\$ grep -rniE \"semver|compatib\" front/ --include='*.cs' --include='*.razor'; echo grep-plan=\$?"
grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'; echo "grep-plan=$?"
echo
echo "\$ grep -rniE \"semver|compatib\" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\\bstring |\\bbool |\\.)(Core)?Compatible\\b' | grep -vE '>[^<>]*[Cc]ompatib[^<>]*</(th|td|span|label|caption|h[1-6])>'; echo resto=\$?   (comando enmendado, tarea 81b92af)"
grep -rniE "semver|compatib" front/ --exclude-dir=bin --exclude-dir=obj | grep -vE '(\bstring |\bbool |\.)(Core)?Compatible\b' | grep -vE '>[^<>]*[Cc]ompatib[^<>]*</(th|td|span|label|caption|h[1-6])>'; echo "resto=$?"
echo
echo "\$ grep -rnE 'System\\.Version|Version\\.(Try)?Parse|NuGet\\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo versiones=\$?"
grep -rnE 'System\.Version|Version\.(Try)?Parse|NuGet\.Versioning|Semver' front/ --include='*.cs' --include='*.razor' --include='*.csproj'; echo "versiones=$?"
