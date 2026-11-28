# Se ejecuta desde la raíz del worktree. Una copia de git archive HEAD por mutación; el worktree no se toca.
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
SHA=$(git rev-parse --short HEAD)
echo "# fecha de inicio: $(date -u +%FT%TZ)  sha: $SHA"
M=backend/src/main/java/io/synkra/registro
corre() { # $1 = nombre de la mutación; el sed ya se aplicó en $D
  (cd "$D" && ./gradlew :backend:test --tests '*VistaGeneralTest' --tests '*FronterasModulosTest' 2>&1 | grep -E "^BUILD|error:" | head -3)
  echo '$ grep -l "<failure" "$D"/backend/build/test-results/test/TEST-*.xml'
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml | sed "s#$D/##"
  echo "pruebas caídas:"
  python3 -I - "$D"/backend/build/test-results/test/TEST-*.xml <<'PY'
import sys, xml.etree.ElementTree as ET
for f in sys.argv[1:]:
    for tc in ET.parse(f).getroot().iter('testcase'):
        if tc.find('failure') is not None or tc.find('error') is not None:
            print("  ", tc.get('classname').split('.')[-1] + "." + tc.get('name'))
PY
  rm -rf "$D"
}
nueva() { echo; echo "=== $1"; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q); }
sedm() { printf '$ sed -i'; printf ' %q' "${@:1:$#-1}"; printf ' "$D/%s/%s"\n' "$M" "${!#}"; sed -i "${@:1:$#-1}" "$D/$M/${!#}"; }

nueva "F-03 E7: una clase pública nueva en registro.api con un parámetro de tipo de clase con límite LecturaPais"
echo '$ printf "package io.synkra.registro.api;\npublic class Fuga<T extends io.synkra.kubernetes.api.LecturaPais> {}\n" > "$D/$M/api/Fuga.java"'
printf "package io.synkra.registro.api;\npublic class Fuga<T extends io.synkra.kubernetes.api.LecturaPais> {}\n" > "$D/$M/api/Fuga.java"
corre
echo; echo "# fin: $(date -u +%FT%TZ)"
