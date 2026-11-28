B=2f47340
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat $B...HEAD
git diff --name-only $B...HEAD | grep -v -E '^(backend/src/main/java/io/synkra/despliegue/package-info\.java$|backend/src/test/java/io/synkra/FronterasModulosTest\.java$|aidlc-docs/construction/U07-despliegue/code/|bitacoras/U07-G\.md$|bitacoras/U07-G-evidencia/|revisiones/U07-G/)'; echo "fuera=$?"
git diff $B...HEAD -- backend/src/test/java/io/synkra/FronterasModulosTest.java | grep -c '^-[^-]'
git diff $B...HEAD -- backend/src/main/java/io/synkra/despliegue/package-info.java | grep -c '^-[^-]'
