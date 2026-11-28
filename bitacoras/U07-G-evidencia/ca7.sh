export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "fecha: $(date -Is)  sha: $(git log -1 --format=%h -- backend)  JAVA_HOME=$JAVA_HOME"
echo '$ ./gradlew :backend:test --rerun :backend:check'; ./gradlew :backend:test --rerun :backend:check 2>&1 | grep -E 'BUILD|FAILED'
echo '$ for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho tests=... ; done'
for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
echo '$ XML con skipped/failures/errors distinto de 0'; grep -L 'skipped="0" failures="0" errors="0"' backend/build/test-results/test/TEST-*.xml | wc -l
echo '$ grep REQUIRES_NEW'; grep -rnE 'Propagation\.REQUIRES_NEW|PROPAGATION_REQUIRES_NEW|setPropagationBehavior' backend/src/main/java; echo "requiresNew=$?"
