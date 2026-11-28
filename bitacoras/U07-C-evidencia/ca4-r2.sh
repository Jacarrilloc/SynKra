export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
R=$(pwd); P=backend/src/main/java/io/synkra/despliegue
mut() { # nombre archivo sed-args...
  local n=$1 f=$2; shift 2
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  echo "=== $n (sha $(git rev-parse --short HEAD))"
  printf '$ sed -i'; printf ' %q' "$@"; echo " \"\$D/$P/$f\""
  sed -i "$@" "$D/$P/$f"
  (cd "$D" && diff <(git -C "$R" show HEAD:$P/$f) $P/$f | head -12)
  (cd "$D" && ./gradlew :backend:test --tests '*PlanPendienteTest' --tests '*VencimientoPlanesTest' > "$D/salida.txt" 2>&1; echo "gradle: $(grep -E '^BUILD' $D/salida.txt)")
  echo "pruebas caidas:"
  for c in PlanPendienteTest VencimientoPlanesTest; do
    grep -o 'testcase name="[^"]*"[^>]*>\s*<failure' -z "$D"/backend/build/test-results/test/TEST-io.synkra.despliegue.$c.xml | tr '\0' '\n' | grep -o 'name="[^"]*"' | grep -v 'name="io.' | sed "s/^/  $c /"
  done
  rm -rf "$D"
}
mut "frontera Q6" VencimientoPlanes.java -e "s/vence_en <= ?/vence_en < ?/"
mut "persistir (@Transactional en la guarda)" GuardaConfirmacion.java -e 's/^    Plan exigirConfirmable/    @org.springframework.transaction.annotation.Transactional\n    Plan exigirConfirmable/'
mut "solo pendiente" VencimientoPlanes.java -e "s/WHERE estado = '\" + Plan.PENDIENTE + \"' AND vence_en/WHERE vence_en/"
mut "por id" VencimientoPlanes.java -e 's/jdbc.update(SQL + " AND id = ?", reloj.instant().toEpochMilli(), id) == 1/jdbc.update(SQL, reloj.instant().toEpochMilli()) >= 1/'
mut "409" GuardaConfirmacion.java -e '/case Plan.CONFIRMADO/,+1d'
mut "reloj" PlanServicio.java -e 's/this.reloj = reloj;/this.reloj = Clock.systemUTC();/'
mut "R-D13 crear" PlanServicio.java -e 's/^\(        var guardado = .*\)$/\1\n        kubernetes.aplicar(kubernetes.construir(new io.synkra.kubernetes.api.PlanPais(pais, imagen, List.of())));/'
mut "R-D13 confirmar" RutasConfirmacion.java -e 's/    private final GuardaConfirmacion guarda;/&\n    private final io.synkra.kubernetes.api.AdaptadorKubernetes k;/' -e 's/RutasConfirmacion(GuardaConfirmacion guarda) {/RutasConfirmacion(GuardaConfirmacion guarda, io.synkra.kubernetes.api.AdaptadorKubernetes k) {\n        this.k = k;/' -e 's/        guarda.exigirConfirmable(id);/        var plan = guarda.exigirConfirmable(id);\n        k.leerPais(plan.pais());/'
mut "F-02 boolean de vencerSiToca" VencimientoPlanes.java -e 's/toEpochMilli(), id) == 1;/toEpochMilli(), id) >= 0;/'
mut "F-02 frontera del barrido" VencimientoPlanes.java -e 's/return jdbc.update(SQL, reloj.instant().toEpochMilli());/return jdbc.update(SQL, reloj.instant().toEpochMilli() - 1);/'
