package io.synkra.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.catalogo.api.CatalogoApi;
import io.synkra.verificador.Descriptor;
import io.synkra.verificador.LectorDescriptor;
import io.synkra.verificador.RangoVersion;
import io.synkra.verificador.Resultado;
import io.synkra.verificador.SuiteCombinaciones;
import io.synkra.verificador.SuiteCombinaciones.Entrada;
import io.synkra.verificador.Verificador;
import io.synkra.verificador.Version;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** U04-T09: el {@code Catalogo} de U01 leído de SQLite da los mismos veredictos que el de memoria (S1 a S7). */
@SpringBootTest
class CatalogoVerificadorTest {

    private static final String IMAGEN = "registro.local/synkra/core:1.0.0@sha256:" + "a".repeat(64);

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired CatalogoApi api;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private static boolean cargada;

    /** Sube a SQLite todo {@code SuiteCombinaciones.contenido()}, solo por la API pública. */
    private synchronized void cargar() throws IOException {
        if (cargada) {
            return;
        }
        for (var c : SuiteCombinaciones.contenido()) {
            for (var v : c.versiones()) {
                api.registrarCore(c.core(), v.toString(), IMAGEN);
            }
            for (var d : c.plugins()) {
                api.subirPlugin(c.core(), new ByteArrayInputStream(jar(d)), Map.of());
            }
        }
        cargada = true;
    }

    private static byte[] jar(Descriptor d) throws IOException {
        var salida = new ByteArrayOutputStream();
        try (var z = new ZipArchiveOutputStream(salida)) {
            z.putArchiveEntry(new ZipArchiveEntry(LectorDescriptor.ARCHIVO));
            z.write(LectorDescriptor.aYaml(d).getBytes(StandardCharsets.UTF_8));
            z.closeArchiveEntry();
        }
        return salida.toByteArray();
    }

    static Stream<SuiteCombinaciones.Caso> casosS1aS7() {
        return SuiteCombinaciones.casos().stream().filter(c -> List.of("S1", "S2", "S3", "S4", "S5", "S6", "S7").contains(c.id()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosS1aS7")
    void elVeredictoEsElMismoQueEnMemoria(SuiteCombinaciones.Caso caso) throws IOException {
        cargar();
        var seleccion = ((Entrada.PorSeleccion) caso.entrada()).seleccion();
        var enMemoria = Verificador.verificar(seleccion, SuiteCombinaciones.catalogo());
        var enSqlite = Verificador.verificar(seleccion, api.catalogoParaVerificar());
        assertThat(enSqlite).isEqualTo(enMemoria);
        assertThat(enSqlite.global()).isEqualTo(caso.esperado().global().orElseThrow());
        var altsMemoria = Verificador.alternativas(seleccion, enMemoria, SuiteCombinaciones.catalogo());
        var altsSqlite = Verificador.alternativas(seleccion, enSqlite, api.catalogoParaVerificar());
        assertThat(altsSqlite).isEqualTo(altsMemoria);
        assertThat(altsSqlite.porPlugin().stream().flatMap(p -> p.alternativas().stream()).toList())
                .isEqualTo(caso.esperado().alternativas());
    }

    @Test
    void lasSieteSeleccionesSeCubren() {
        assertThat(casosS1aS7().count()).isEqualTo(7);
    }

    @Test
    void versionesDeAlgoInexistenteSonListaVacia() throws IOException {
        cargar();
        var c = api.catalogoParaVerificar();
        assertThat(c.versionesCore("no-existe")).isEmpty();
        assertThat(c.versionesPlugin("synkra-core", "no-existe")).isEmpty();
        assertThat(c.versionesPlugin("no-existe", "kyc-co")).isEmpty();
    }

    @Test
    void descriptorDeVersionInexistenteEsVacio() throws IOException {
        cargar();
        var c = api.catalogoParaVerificar();
        assertThat(c.descriptor("synkra-core", "kyc-co", new Version(9, 9, 9))).isEmpty();
        assertThat(c.descriptor("synkra-core", "no-existe", new Version(1, 0, 0))).isEqualTo(Optional.empty());
    }

    @Test
    void descriptorNoTraeLasEtiquetasDeSubida() throws IOException {
        cargar();
        var d = new Descriptor("etiquetado", new Version(1, 0, 0), "core-etiquetas",
                ((Resultado.Exito<RangoVersion>) RangoVersion.parsear("1.x")).valor(), Map.of("pais", "co"),
                Optional.of("con etiquetas"));
        api.registrarCore("core-etiquetas", "1.0.0", IMAGEN);
        api.subirPlugin("core-etiquetas", new ByteArrayInputStream(jar(d)), Map.of("cliente", "acme"));
        assertThat(api.obtenerPlugin("core-etiquetas", "etiquetado", "1.0.0").etiquetas())
                .containsEntry("cliente", "acme");
        var leido = api.catalogoParaVerificar().descriptor("core-etiquetas", "etiquetado", new Version(1, 0, 0));
        assertThat(leido).contains(d);
        assertThat(leido.orElseThrow().etiquetas()).containsOnlyKeys("pais");
    }

    @Test
    void unaCarpetaNoVeLosPluginsDeOtra() throws IOException {
        cargar();
        var c = api.catalogoParaVerificar();
        assertThat(c.versionesPlugin("otro-core", "kyc-co")).isEmpty();
        assertThat(c.versionesPlugin("synkra-core", "kyc-xx")).isEmpty();
        assertThat(c.descriptor("synkra-core", "kyc-xx", new Version(1, 0, 0))).isEmpty();
        assertThat(c.descriptor("otro-core", "kyc-xx", new Version(1, 0, 0))).isPresent();
        assertThat(c.versionesPlugin("synkra-core", "kyc-pe"))
                .containsExactlyInAnyOrder(new Version(1, 3, 0), new Version(2, 0, 0));
    }

    @Test
    void versionesDeCoreYPluginCoincidenConLaSuite() throws IOException {
        cargar();
        var c = api.catalogoParaVerificar();
        var memoria = SuiteCombinaciones.catalogo();
        assertThat(c.versionesCore("synkra-core")).containsExactlyInAnyOrderElementsOf(memoria.versionesCore("synkra-core"));
        assertThat(c.versionesPlugin("synkra-core", "kyc-pe"))
                .containsExactlyInAnyOrderElementsOf(memoria.versionesPlugin("synkra-core", "kyc-pe"));
    }

    @Test
    void unaFilaNoParseableEsErrorInternalYNoSeOculta() throws IOException {
        cargar();
        jdbc.update("insert into cat_core(nombre) values ('core-roto')");
        jdbc.update("insert into cat_version_core values ('core-roto','no-es-version','img','x',0)");
        jdbc.update("insert into cat_plugin values ('core-roto','malo','1.0.0','no-es-rango','h',0,'r',null,'x',0)");
        jdbc.update("insert into cat_plugin values ('core-roto','malo','no-es-version','1.x','h',0,'r',null,'x',0)");
        var c = api.catalogoParaVerificar();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> c.versionesCore("core-roto"))
                .isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> c.versionesPlugin("core-roto", "malo"))
                .isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> c.descriptor("core-roto", "malo", new Version(1, 0, 0)))
                .isInstanceOf(IllegalStateException.class);
    }
}
