export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)  puerto 18091 libre: $(ss -ltn | grep -c ':18091 ') sockets"
PS4='$ '; set -x
./gradlew :backend:test --rerun --tests '*PaisesConfiguradosTest' 2>&1 | grep -E "BUILD|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PaisesConfiguradosTest.xml
git show 8182b9d:backend/src/main/resources/application.properties | diff - backend/src/main/resources/application.properties; echo "igual=$?"
./gradlew :backend:bootJar 2>&1 | grep -E "BUILD|FAILED"
L=$(mktemp); H=$(mktemp -d)
HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_DATA=$(mktemp -d) SYNKRA_PAISES='co,PE' timeout 60 $JAVA_HOME/bin/java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18091 > "$L" 2>&1; echo "salida=$?"
grep -c "SYNKRA_PAISES" "$L"; grep -ci "paisesConfigurados" "$L"
set +x
echo "--- líneas relevantes del log:"; grep -E "SYNKRA_PAISES|PaisesConfigurados" "$L" | cut -c1-300 | head -5
