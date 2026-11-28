#!/bin/bash
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "fecha: $(date -Is)  sha: $(git log -1 --format=%h -- backend)  JAVA_HOME=$JAVA_HOME"
echo '$ ./gradlew :backend:test --rerun --tests '"'*FronterasModulosTest'"; ./gradlew :backend:test --rerun --tests '*FronterasModulosTest' 2>&1 | grep -E 'BUILD|FAILED'
echo '$ grep -ho tests=... (XML de FronterasModulosTest)'; grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
grep -o 'testcase name="[A-Za-z0-9]*()"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
echo '$ grep -Fcx despliegue'; grep -Fcx '@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})' backend/src/main/java/io/synkra/despliegue/package-info.java
echo '$ grep -Fcx registro'; grep -Fcx '@ApplicationModule(allowedDependencies = {"kubernetes :: api", "plataforma"})' backend/src/main/java/io/synkra/registro/package-info.java
echo '$ imports ajenos'; grep -rnE '^import (static )?io\.synkra\.' backend/src/main/java/io/synkra/despliegue | grep -vE 'io\.synkra\.(despliegue|catalogo\.api|kubernetes\.api|registro\.api|plataforma|verificador)\.'; echo "imports-ajenos=$?"
echo '$ fabric8'; grep -rlE '^import (static )?io\.fabric8\.' backend/src/main/java/io/synkra/despliegue; echo "fabric8=$?"
echo '$ inverso'; grep -rlE '^import (static )?io\.synkra\.despliegue\.' backend/src/main/java | grep -v '/io/synkra/despliegue/'; echo "inverso=$?"
echo '$ publicas'; grep -rlE '^public ' backend/src/main/java/io/synkra/despliegue; echo "publicas=$?"
echo '$ ls api'; ls -d backend/src/main/java/io/synkra/despliegue/api; echo "api=$?"
