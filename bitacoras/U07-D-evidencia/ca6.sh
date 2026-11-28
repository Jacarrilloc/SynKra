./gradlew :backend:test --rerun :backend:check 2>&1 | grep -E "BUILD|tests completed|FAILED"
for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
grep -rnE "io\.synkra\.registro\." backend/src/main/java/io/synkra/despliegue | grep -vE "io\.synkra\.registro\.api\."; echo "registro-interno=$?"
grep -rn "reg_" backend/src/main/java/io/synkra/despliegue; echo "tablas-reg=$?"
grep -rlE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue
grep -rcE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java
grep -rnE "kubernetes\.(estado|contextoFallo|existeNamespace)\(" backend/src/main/java/io/synkra/despliegue; echo "vigilancia=$?"
grep -rn "REQUIRES_NEW" backend/src/main/java; echo "requiresNew=$?"
grep -rn "@Scheduled\|@EnableScheduling" backend/src/main/java; echo "programado=$?"
grep -n "Transactional" backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java; echo "confirmar-tx=$?"
# Extra (el grep literal de arriba ve el comentario «nunca REQUIRES_NEW» de RegistroServicio, de U06): solo uso real, no comentarios
grep -rnE "Propagation\.REQUIRES_NEW|propagation *= *[A-Za-z.]*REQUIRES_NEW" backend/src/main/java; echo "requiresNew-uso=$?"
# Extra: el grep literal de tablas-reg casa con un comentario de Plan.java (reg_despliegue, de U07-A); solo literales de texto (SQL):
grep -rnE '"[^"]*reg_' backend/src/main/java/io/synkra/despliegue; echo "tablas-reg-sql=$?"
grep -rn "reg_" backend/src/main/java/io/synkra/despliegue
