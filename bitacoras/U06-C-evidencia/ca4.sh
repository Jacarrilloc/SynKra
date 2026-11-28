export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)"
PS4='$ '; set -x
./gradlew :backend:test --rerun --tests '*FronterasModulosTest' 2>&1 | grep -E "BUILD|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
grep -o 'testcase name="[A-Za-z]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
grep -n "allowedDependencies" backend/src/main/java/io/synkra/registro/package-info.java
grep -rn "io.synkra.despliegue\|io.synkra.catalogo" backend/src/main/java/io/synkra/registro; echo "ajenos=$?"
grep -rnE "io\.synkra\.kubernetes\." backend/src/main/java/io/synkra/registro | grep -vE "io\.synkra\.kubernetes\.api\."; echo "internos=$?"
grep -rnE "\.(aplicar|construir|contextoFallo|existeNamespace)\(" backend/src/main/java/io/synkra/registro; echo "k8s-escrituras=$?"
grep -rn "@Bean" backend/src/main/java/io/synkra/registro; echo "beans=$?"
