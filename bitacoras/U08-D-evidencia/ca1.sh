#!/usr/bin/env bash
# CA-1: la decision 1. La plantilla del pod cambia cuando cambian solo los plugins. Desde la raiz del worktree.
BASE=367e09f
./gradlew :backend:test --rerun :backend:check; echo "gradle=$?"
python3 -I -c "import sys,xml.etree.ElementTree as E; r=E.parse(sys.argv[1]).getroot(); print(r.get('tests'), r.get('failures'), r.get('errors'), r.get('skipped')); [print(t.get('name')) for t in r.iter('testcase')]" backend/build/test-results/test/TEST-io.synkra.kubernetes.ConstruirObjetosPaisTest.xml
grep -n 'synkra.io/config-sha256' backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java backend/src/test/resources/kubernetes/pais-pe-esperado.yaml
git diff --name-only "$BASE"...HEAD -- backend | sort
echo "== todo el backend: pruebas, fallos, errores, saltos"
python3 -I - <<'PY'
import glob, xml.etree.ElementTree as E
t = f = e = s = 0
for p in glob.glob("backend/build/test-results/test/TEST-*.xml"):
    r = E.parse(p).getroot()
    t += int(r.get("tests")); f += int(r.get("failures")); e += int(r.get("errors")); s += int(r.get("skipped"))
print(t, f, e, s)
PY
