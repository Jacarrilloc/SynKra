#!/bin/bash
# Datos de la punta que el resumen cita; desde la raíz del worktree.
echo "fecha: $(date -Is)  sha: $(git log -1 --format=%h -- backend)  JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2"
echo '$ ls backend/src/main/java/io/synkra/despliegue/'; ls backend/src/main/java/io/synkra/despliegue/
echo '$ grep -rn '"'"'Mapping('"'"' backend/src/main/java/io/synkra/despliegue'; grep -rn 'Mapping(' backend/src/main/java/io/synkra/despliegue
echo '$ grep -n allowedDependencies backend/src/main/java/io/synkra/despliegue/package-info.java backend/src/main/java/io/synkra/registro/package-info.java'; grep -n allowedDependencies backend/src/main/java/io/synkra/despliegue/package-info.java backend/src/main/java/io/synkra/registro/package-info.java
echo '$ grep -rn leerPais backend/src/main/java/io/synkra/despliegue'; grep -rn leerPais backend/src/main/java/io/synkra/despliegue
echo '$ ls backend/src/main/resources/db/migration/'; ls backend/src/main/resources/db/migration/
echo '$ grep -rn Sharedmodules'; grep -n 'sharedModules' backend/src/main/java/io/synkra/SynkraApp.java
for pr in 1d69064 0c83a20 208d5f9 39a242c 20ca4e1 b73d9d0; do echo "\$ git diff --name-status $pr^1...$pr^2 -- backend/src ':!despliegue (main y test)'"; git diff --name-status $pr^1...$pr^2 -- backend/src ':!backend/src/main/java/io/synkra/despliegue' ':!backend/src/test/java/io/synkra/despliegue' ':!backend/src/test/java/io/synkra/FronterasModulosTest.java'; done
