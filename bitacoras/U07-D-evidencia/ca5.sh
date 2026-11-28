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
mut "R-D6 no vuelve a verificar" ConfirmacionServicio.java -e '/^        PlanServicio.rechazaSiNoCompatible(verificado.veredicto());$/d'
mut "R-D6 antes de reservar (la verificacion va despues)" ConfirmacionServicio.java -e '/^        PlanServicio.rechazaSiNoCompatible(verificado.veredicto());$/d' -e 's/^\(        var confirmadoEn = reserva.reservar.*\)$/\1\n        PlanServicio.rechazaSiNoCompatible(verificado.veredicto());/'
mut "resumen no se compara con el catalogo" ConfirmacionServicio.java -e 's/if (!resumenCatalogo.armar(plan.pais(), verificado.seleccion()).equals(resumen)) {/if (false) {/'
mut "R-D7 sin comprobar aplicando" ReservaConfirmacion.java -e 's/anyMatch(d -> d.estado() == EstadoDespliegue.APLICANDO)/anyMatch(d -> false)/'
mut "R-D7 solo aplicando (bloquea con cualquier historial)" ReservaConfirmacion.java -e 's/anyMatch(d -> d.estado() == EstadoDespliegue.APLICANDO)/anyMatch(d -> true)/'
mut "transaccion: reservar sin @Transactional" ReservaConfirmacion.java -e '/^    @Transactional$/d'
mut "UPDATE primero: sin estado = pendiente en el WHERE" ReservaConfirmacion.java -e "s/ AND estado = 'pendiente' AND vence_en/ AND vence_en/"
mut "R-D8 confirmadoPor = creadoPor" ReservaConfirmacion.java -e 's/plan.creadoPor(), confirmadoPor, ahora, veredicto/plan.creadoPor(), plan.creadoPor(), ahora, veredicto/'
mut "usuario: Usuario.de acepta cualquier texto" Usuario.java -e 's/if (!FORMA.matcher(cabecera).matches()) {/if (false) {/'
mut "orden R-D9: aplicar antes de reservar" ConfirmacionServicio.java -e 's/^\(        var plugins = resumen.plugins().*\)$/        kubernetes.aplicar(objetos);\n\1/'
mut "fallo registrado: no se llama a actualizarResultado" ConfirmacionServicio.java -e '/^            fallido(plan.id(), e.getMessage());$/d'
mut "fuera de la tx: @Transactional en confirmar" ConfirmacionServicio.java -e 's/^    Confirmado confirmar(long id/    @org.springframework.transaction.annotation.Transactional\n    Confirmado confirmar(long id/'
mut "sin cluster: sin comprobar disponible()" ConfirmacionServicio.java -e 's/if (!kubernetes.disponible()) {/if (false) {/'
mut "P4 al crear: VerificacionVivo no busca en las demas carpetas" VerificacionVivo.java -e 's/for (var otro : catalogo.listarCores()) {/for (var otro : java.util.List.<io.synkra.catalogo.api.CoreRegistrado>of()) {/'
