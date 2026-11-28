git status --short | wc -l   # árbol limpio
git rev-parse --short HEAD; git diff --stat ade68f6...HEAD
git diff --name-status ade68f6...HEAD -- backend/src/test backend/src/main/resources backend/src/main/java/io/synkra/registro backend/src/main/java/io/synkra/plataforma backend/src/main/java/io/synkra/kubernetes backend/src/main/java/io/synkra/catalogo
git diff ade68f6...HEAD -- backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java | grep -c '^-[^-]'
