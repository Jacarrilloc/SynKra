# Una copia de «git archive HEAD» por mutación; el worktree no se toca (sin git stash).
RAIZ=$(pwd); echo "HEAD: $(git rev-parse --short HEAD); ultimo commit que toca codigo: $(git log -1 --format=%h -- backend)"
S=backend/src/main/java/io/synkra/despliegue
mut() { # nombre archivo sed-args...
  local nombre=$1 archivo=$2; shift 2
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  echo; echo "=== MUTACION: $nombre"
  printf 'sed -i'; printf ' %q' "$@"; printf ' %q\n' "$S/$archivo"
  sed -i "$@" "$D/$S/$archivo"
  diff <(cd "$RAIZ" && git show HEAD:"$S/$archivo") "$D/$S/$archivo" | sed 's/^/  | /'
  (cd "$D" && ./gradlew :backend:test --rerun --tests '*ConfirmarTest' --tests '*CarpetaEquivocadaPlanTest' --tests '*ConfirmarSinClusterTest' 2>&1 | grep -E "^BUILD|tests completed")
  echo "XML con <failure:"; grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml | sed 's|.*/TEST-||'
  echo "pruebas caidas:"; python3 -I - "$D"/backend/build/test-results/test/TEST-*.xml <<'EOF'
import sys, xml.etree.ElementTree as ET
for f in sys.argv[1:]:
    for t in ET.parse(f).getroot().iter('testcase'):
        if t.find('failure') is not None or t.find('error') is not None:
            print("  ", t.get('classname').rsplit('.', 1)[1] + "." + t.get('name'))
EOF
  rm -rf "$D"
}
# Ronda 2: las cinco mutaciones M1 a M5 del revisor (F-01). Antes de las pruebas nuevas daban BUILD SUCCESSFUL.
mut "M1 excepcion no AplicacionFallida no queda fallido" ConfirmacionServicio.java -e '/^            fallido(plan.id(), e.getMessage() == null/d'
mut "M2 sin vence_en en el WHERE" ReservaConfirmacion.java -e 's/ AND vence_en > ?"/"/' -e 's/jdbc.update(CONFIRMAR, plan.id(), ahora.toEpochMilli())/jdbc.update(CONFIRMAR, plan.id())/'
mut "M3 el 410 de reservar pasa a 409" ReservaConfirmacion.java -e 's/HttpStatus.GONE/HttpStatus.CONFLICT/'
mut "M4 se registra sin plugins" ConfirmacionServicio.java -e 's/texto(VeredictoDto.de(verificado.veredicto())), plugins)/texto(VeredictoDto.de(verificado.veredicto())), List.of())/'
mut "M5 plugins sin etiquetas del catalogo" ConfirmacionServicio.java -e 's/catalogo.obtenerPlugin(plan.core(), p.nombre(), p.version()).etiquetas()/java.util.Map.of()/'
