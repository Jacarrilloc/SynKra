export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)"
PS4='$ '; set -x
./gradlew :backend:test --rerun :backend:check 2>&1 | grep -E "^BUILD|FAILED"
for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
