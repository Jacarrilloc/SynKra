cd /home/casdav/Projects/Master/wt-m6r-U06-B
echo "# worktree HEAD $(git rev-parse --short HEAD); estado: $(git status --porcelain | wc -l) cambios sin commitear. Copias con git archive HEAD (sin git stash)."
muta() { # nombre archivo sed clase
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  echo; echo "=== $1"; echo "\$ sed -i '$3' $D/$2"
  sed -i "$3" "$D/$2"; (cd "$D" && git diff --no-index --stat /dev/null /dev/null; true)
  grep -c "" "$D/$2" >/dev/null
  (cd "$D" && ./gradlew :backend:test --rerun --tests '*HistorialTest' --tests '*RegistroReinicioTest' 2>&1 | grep -E "^BUILD|tests completed")
  echo "\$ grep -l '<failure' $D/backend/build/test-results/test/TEST-*.xml"
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml | sed "s#$D/##"
  python3 -I - "$D"/backend/build/test-results/test/TEST-*.xml <<'EOF'
import sys, xml.etree.ElementTree as ET
for f in sys.argv[1:]:
    for tc in ET.parse(f).getroot().iter('testcase'):
        if tc.find('failure') is not None:
            print("  cae:", tc.get('classname').split('.')[-1] + "." + tc.get('name'))
EOF
  rm -rf "$D"
}
R=backend/src/main/java/io/synkra/registro/RegistroServicio.java
muta "R-R5 desempate invertido (id asc)" $R 's/order by confirmado_en desc, id desc/order by confirmado_en desc, id asc/'
echo; echo "=== plan de la consulta sin ', id desc' (por que la mutacion literal sobrevive)"
python3 -I -c "
import sqlite3
c=sqlite3.connect(':memory:')
c.executescript(open('backend/src/main/resources/db/migration/V3__registro.sql').read())
print(c.execute(\"explain query plan select id from reg_despliegue where pais = 'pe' order by confirmado_en desc\").fetchall())
print(c.execute(\"explain query plan select id from reg_despliegue where pais = 'pe' order by confirmado_en desc, id desc\").fetchall())
"
