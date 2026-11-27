package io.synkra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SynkraAppTest {

    @TempDir
    static Path datos;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("synkra.data", () -> datos.toString());
    }

    @Autowired
    ApplicationContext contexto;

    @Test
    void elContextoCarga() {
    }

    @Test
    void laAppDePruebaDeFronterasNoEntraAlContextoReal() {
        // Se filtra por el paquete del TIPO de cada bean, no por su nombre.
        var intrusos = Arrays.stream(contexto.getBeanDefinitionNames())
                .map(n -> contexto.getType(n))
                .filter(t -> t != null && t.getName().startsWith("io.synkra.fronteras"))
                .map(Class::getName)
                .toList();
        assertThat(intrusos).isEmpty();
    }

    /** C-14: los fixtures de plataforma no entran al contexto de los @SpringBootTest. */
    @Test
    void losFixturesDePruebaDePlataformaNoEntranAlContexto() {
        assertThat(contexto.getBeanNamesForType(fixtures.plataforma.ControladorDePrueba.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(fixtures.plataforma.ElementoRepositorio.class)).isEmpty();
    }

    /** U05-T09 (C-70): en la aplicación real el cliente de Fabric8 no se puede inyectar desde otro módulo. */
    @Test
    void elClienteDeKubernetesNoEsUnBeanDeLaAplicacion() {
        assertThat(contexto.getBeanNamesForType(io.fabric8.kubernetes.client.KubernetesClient.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(io.synkra.kubernetes.api.AdaptadorKubernetes.class)).hasSize(1);
    }
}
