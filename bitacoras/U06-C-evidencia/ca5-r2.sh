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

nueva "R-R6 digest: Concordancia no compara la imagen (cl da true)"
sedm -e 's/if (!registrado.imagen().equals(enClaster.imagenCore())) {/if (false) {/' Concordancia.java
corre

nueva "R-R6 sha256: Concordancia compara solo por (nombre, version) (co da true)"
sedm -e 's/new Terna(p.nombre(), p.version(), p.sha256())/new Terna(p.nombre(), p.version(), "")/g' Concordancia.java
corre

nueva "R-R6 conjunto: Concordancia compara las listas en orden (pe da false)"
sedm -e 's/Set<Terna> esperados = new HashSet<>();/List<Terna> esperados = new java.util.ArrayList<>();/' -e 's/Set<Terna> corriendo = new HashSet<>();/List<Terna> corriendo = new java.util.ArrayList<>();/' Concordancia.java
corre

nueva "R-R6 de más: Concordancia acepta que lo leído contenga lo registrado (sv da true)"
sedm -e 's/return esperados.equals(corriendo);/return corriendo.containsAll(esperados);/' Concordancia.java
corre

nueva "R-R6 vacíos: «uno sin el otro» da true (bo y uy dan true)"
sedm -e 's/return ultimo.isEmpty() \&\& leido.isEmpty();/return true;/' Concordancia.java
corre

nueva "último listo: quitar estado = 'listo' de ultimoListo (pe toma el 47)"
sedm -e "s/ and estado = 'listo' / /" RegistroServicio.java
corre

nueva "desempate: id DESC pasa a id ASC en ultimoListo (cl toma el 45 y da true)"
sedm -e 's/order by confirmado_en desc, id desc limit 1/order by confirmado_en desc, id asc limit 1/' RegistroServicio.java
corre

nueva "R-R7: vistaGeneral recorre List.of(pe, co, mx) en vez de paises.lista()"
sedm -e 's/for (var pais : paises.lista()) {/for (var pais : List.of("pe", "co", "mx")) {/' VistaGeneralServicio.java
corre

nueva "no disponible: ClienteNoDisponible/LecturaFallida dan concuerda = false (py y ve dan false)"
sedm -e '/LecturaCluster.NO_DISPONIBLE/s/List.of(), null, id/List.of(), false, id/' VistaGeneralServicio.java
corre

nueva "conexión: @Transactional(readOnly = true) en VistaGeneralServicio.vistaGeneral"
sedm -e 's/^    public List<VistaPais> vistaGeneral() {/    @org.springframework.transaction.annotation.Transactional(readOnly = true)\n    public List<VistaPais> vistaGeneral() {/' VistaGeneralServicio.java
corre

nueva "frontera: RegistroServicio recibe un CatalogoApi en un campo nuevo con @Autowired"
sedm -e 's/^    private final Clock reloj;/    private final Clock reloj;\n    @Autowired private io.synkra.catalogo.api.CatalogoApi catalogo;/' RegistroServicio.java
corre

nueva "API: VistaPais gana un componente LecturaPais lectura2 (con constructor de 8 para que compile)"
sedm -e 's/String versionCore) {}/String versionCore, io.synkra.kubernetes.api.LecturaPais lectura2) {\n    public VistaPais(String pais, LecturaCluster lectura, String imagen, List<PluginCorriendo> plugins, Boolean concuerda, Long ultimoDespliegueId, String core, String versionCore) {\n        this(pais, lectura, imagen, plugins, concuerda, ultimoDespliegueId, core, versionCore, null);\n    }\n}/' api/VistaPais.java
corre

nueva "escritura: RutasVistaGeneral gana un @PostMapping vacío"
sedm -e 's/^    private static VistaPaisDto dto(VistaPais v) {/    @org.springframework.web.bind.annotation.PostMapping("\/api\/x")\n    void escribe() {}\n\n    private static VistaPaisDto dto(VistaPais v) {/' RutasVistaGeneral.java
corre

nueva "F-01 E3: la imagen sale del último listo del registro y no del clúster (cl muestra B)"
sedm -e 's/LecturaCluster.LEIDO, lectura.imagenCore(), plugins,/LecturaCluster.LEIDO, ultimo.map(DespliegueRegistrado::imagen).orElse(lectura.imagenCore()), plugins,/' VistaGeneralServicio.java
corre

nueva "F-01 E4: los plugins salen del registro cuando hay último listo (co muestra sha c, sv sin kyc-extra)"
sedm -e 's/LecturaCluster.LEIDO, lectura.imagenCore(), plugins,/LecturaCluster.LEIDO, lectura.imagenCore(), ultimo.map(u -> u.plugins().stream().map(p -> new PluginCorriendo(p.nombre(), p.version(), p.sha256())).toList()).orElse(plugins),/' VistaGeneralServicio.java
corre

nueva "F-02 E5: @RequestMapping(\"/api/x\") sin method en un método de RutasVistaGeneral"
sedm -e 's/^    private static VistaPaisDto dto(VistaPais v) {/    @org.springframework.web.bind.annotation.RequestMapping("\/api\/x")\n    void escribe() {}\n\n    private static VistaPaisDto dto(VistaPais v) {/' RutasVistaGeneral.java
corre

nueva "F-02 E6: @RequestMapping(method = POST) a nivel de clase en RutasVistaGeneral"
sedm -e 's/^@RestController/@RestController\n@org.springframework.web.bind.annotation.RequestMapping(path = "\/api\/y", method = org.springframework.web.bind.annotation.RequestMethod.POST)/' RutasVistaGeneral.java
corre

nueva "F-03 E7: VistaGeneralApi gana un parámetro de tipo con límite LecturaPais"
sedm -e 's/public interface VistaGeneralApi {/public interface VistaGeneralApi<T extends io.synkra.kubernetes.api.LecturaPais> {/' api/VistaGeneralApi.java
corre

echo; echo "# fin: $(date -u +%FT%TZ)"
