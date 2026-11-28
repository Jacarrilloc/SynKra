#!/usr/bin/env bash
# CA-5: mensajes de las tres pruebas nuevas en rojo, en una copia del primer commit (3af0a36). Desde la raiz del worktree.
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
git archive 3af0a36 | tar -x -C "$D"
(cd "$D" && ./gradlew :backend:test --rerun --tests 'io.synkra.kubernetes.ConstruirObjetosPaisTest' >/dev/null 2>&1; echo "gradle=$?"
python3 -I -c "
import xml.etree.ElementTree as E
r=E.parse('backend/build/test-results/test/TEST-io.synkra.kubernetes.ConstruirObjetosPaisTest.xml').getroot()
for t in r.iter('testcase'):
    f=t.find('failure')
    if f is not None: print(t.get('name'),'|',f.get('message')[:200].replace(chr(10),' '))
")
