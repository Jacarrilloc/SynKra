# Repite solo la mutación «conexión» y pega el mensaje del fallo, para ver que cae por el plazo de 5 s (no por los 30 s del pool).
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)"
D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
F="$D/backend/src/main/java/io/synkra/registro/VistaGeneralServicio.java"
echo "\$ sed -i -e 's/^    public List<VistaPais> vistaGeneral() {/    @org.springframework.transaction.annotation.Transactional(readOnly = true)\\n    public List<VistaPais> vistaGeneral() {/' \$F"
sed -i -e 's/^    public List<VistaPais> vistaGeneral() {/    @org.springframework.transaction.annotation.Transactional(readOnly = true)\n    public List<VistaPais> vistaGeneral() {/' "$F"
(cd "$D" && ./gradlew :backend:test --tests '*VistaGeneralTest' 2>&1 | grep -E "^BUILD|FAILED")
python3 -I - "$D"/backend/build/test-results/test/TEST-io.synkra.registro.VistaGeneralTest.xml <<'PY'
import sys, xml.etree.ElementTree as ET
for tc in ET.parse(sys.argv[1]).getroot().iter('testcase'):
    f = tc.find('failure')
    print(tc.get('name'), 'tiempo=' + tc.get('time') + 's', 'FALLA: ' + f.get('message')[:300] if f is not None else 'ok')
PY
rm -rf "$D"
