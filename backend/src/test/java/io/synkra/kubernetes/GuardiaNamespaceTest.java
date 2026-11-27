package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.synkra.kubernetes.api.NamespaceNoPermitidoException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** U05-T02: solo el servidor simulado de Fabric8; user.home falso para no tocar ~/.kube. */
class GuardiaNamespaceTest {

    @TempDir
    Path casaFalsa;

    KubernetesMockServer servidor;
    KubernetesClient cliente;
    AdaptadorFabric8 adaptador;
    private String propiedadCasa;

    @BeforeEach
    void arranca() {
        propiedadCasa = System.getProperty("user.home");
        System.setProperty("user.home", casaFalsa.toString());
        servidor = new KubernetesMockServer(true);
        servidor.init();
        cliente = servidor.createClient();
        adaptador = new AdaptadorFabric8(cliente);
    }

    @AfterEach
    void para() {
        cliente.close();
        servidor.destroy();
        if (propiedadCasa == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", propiedadCasa);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"default", "kube-system", "synkra", "synkra-", "synkra-PE", "synkra-per", "synkra-p",
            "synkra-pe ", "synkra-pe/x", "../synkra-pe", "synkra-pe\n", "Synkra-pe", " synkra-pe", "synkra-p1"})
    void rechazaSinPeticiones(String namespace) {
        int antes = servidor.getRequestCount();
        assertThatThrownBy(() -> adaptador.existeNamespace(namespace))
                .isInstanceOf(NamespaceNoPermitidoException.class);
        assertThat(servidor.getRequestCount()).isEqualTo(antes);
    }

    @Test
    void recortaElNamespaceEnElMensaje() {
        String largo = "x".repeat(200);
        assertThatThrownBy(() -> adaptador.existeNamespace(largo))
                .isInstanceOf(NamespaceNoPermitidoException.class)
                .hasMessageContaining("x".repeat(63))
                .hasMessageNotContaining("x".repeat(64));
    }

    @ParameterizedTest
    @ValueSource(strings = {"synkra-pe", "synkra-co"})
    void aceptaYHablaConLaApi(String namespace) {
        servidor.expect().get().withPath("/api/v1/namespaces/" + namespace)
                .andReturn(200, "{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"" + namespace + "\"}}")
                .always();
        int antes = servidor.getRequestCount();
        assertThat(adaptador.existeNamespace(namespace)).isTrue();
        assertThat(servidor.getRequestCount()).isGreaterThan(antes);
    }
}
