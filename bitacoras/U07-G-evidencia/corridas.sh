#!/bin/bash
# Las tres corridas de la decisión 10; desde la raíz del worktree, con el árbol en el último commit que toca código.
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
E=aidlc-docs/construction/U07-despliegue/code/evidencia
S=$(git log -1 --format=%h -- backend)
cab() { echo "fecha: $(date -Is)  sha: $S  JAVA_HOME=$JAVA_HOME"; }
C1="./gradlew :backend:test --rerun --tests '*despliegue*'; echo \"salida=\$?\""
{ cab; echo "\$ $C1"; ./gradlew :backend:test --rerun --tests '*despliegue*'; echo "salida=$?"; } > $E/gradle-despliegue.txt 2>&1
{ cab; echo '$ for f in backend/build/test-results/test/TEST-*despliegue*.xml; do echo "$(basename $f .xml | sed '"'"'s/TEST-io.synkra.//'"'"') $(grep -o -m1 '"'"'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"'"'"' $f)"; done'
  for f in backend/build/test-results/test/TEST-*despliegue*.xml; do echo "$(basename $f .xml | sed 's/TEST-io.synkra.//') $(grep -o -m1 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' $f)"; done; } > $E/conteo-por-clase-despliegue.txt
C2="./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*RelojTest' --tests '*AplicandoTest' --tests '*ContextoEventosTest' --tests '*EstadoRolloutTest' --tests '*ReintentosClienteTest'; echo \"salida=\$?\""
{ cab; echo "\$ $C2"; ./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*RelojTest' --tests '*AplicandoTest' --tests '*ContextoEventosTest' --tests '*EstadoRolloutTest' --tests '*ReintentosClienteTest'; echo "salida=$?"; } > $E/gradle-otros-paquetes.txt 2>&1
{ cab; echo '$ for f in backend/build/test-results/test/TEST-*.xml; do echo "$(basename $f .xml | sed '"'"'s/TEST-io.synkra.//'"'"') $(grep -o -m1 '"'"'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"'"'"' $f)"; done'
  for f in backend/build/test-results/test/TEST-*.xml; do echo "$(basename $f .xml | sed 's/TEST-io.synkra.//') $(grep -o -m1 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' $f)"; done; } > $E/conteo-por-clase-otros-paquetes.txt
{ cab; echo "\$ ./gradlew :backend:test --rerun :backend:check; echo \"salida=\$?\""; ./gradlew :backend:test --rerun :backend:check; echo "salida=$?"; } > $E/gradle-check.txt 2>&1
{ cab; echo '$ ls backend/build/test-results/test/TEST-*.xml | wc -l'; ls backend/build/test-results/test/TEST-*.xml | wc -l
  echo "\$ grep -L 'skipped=\"0\" failures=\"0\" errors=\"0\"' backend/build/test-results/test/TEST-*.xml | wc -l"; grep -L 'skipped="0" failures="0" errors="0"' backend/build/test-results/test/TEST-*.xml | wc -l
  echo '$ cat backend/build/test-results/test/TEST-*.xml | grep -o -h '"'"'<testsuite [^>]*'"'"' | grep -o '"'"' tests="[0-9]*"'"'"' | grep -o '"'"'[0-9]*'"'"' | paste -sd+ | bc'
  cat backend/build/test-results/test/TEST-*.xml | grep -o -h '<testsuite [^>]*' | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' | paste -sd+ | bc; } > $E/conteo-xml.txt
