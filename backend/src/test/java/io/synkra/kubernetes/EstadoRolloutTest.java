package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.kubernetes.api.FaseDespliegue;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** U07-E, C-82: {@code estado} juzga la versión nueva del rollout, no solo las réplicas listas. */
class EstadoRolloutTest {

    @TempDir
    Path casa;

    LecturasDePrueba k;

    @BeforeEach
    void arranca() {
        k = new LecturasDePrueba(casa);
    }

    @AfterEach
    void para() {
        k.close();
    }

    @Test
    void unPodViejoListoNoTapaAlNuevoQueFalla() {
        k.deployment(1, 2L, 1, 1, 1, 2L);
        k.podListo("synkra-core-viejo");
        k.podFalla("synkra-core-nuevo", 3);
        var e = k.adaptador.estado(LecturasDePrueba.NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.FALLIDO);
        assertThat(e.razon()).isEqualTo("CrashLoopBackOff");
        assertThat(e.replicasListas()).isEqualTo(e.replicasDeseadas()).isEqualTo(1);
    }

    @Test
    void unPodNuevoPendienteConElViejoListoEsEnCurso() {
        k.deployment(1, 2L, 2, 1, 1, 2L);
        k.podListo("synkra-core-viejo");
        k.podPendiente("synkra-core-nuevo");
        var e = k.adaptador.estado(LecturasDePrueba.NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.EN_CURSO);
        assertThat(e.razon()).isNull();
    }

    @Test
    void conPodsViejosSobrandoEsEnCurso() {
        k.deployment(1, 2L, 2, 1, 1, 2L);
        k.podListo("synkra-core-viejo");
        assertThat(k.adaptador.estado(LecturasDePrueba.NS).fase()).isEqualTo(FaseDespliegue.EN_CURSO);
    }

    @Test
    void sinTodasLasReplicasActualizadasEsEnCurso() {
        k.deployment(1, 2L, 1, 1, 0, 2L);
        k.podListo("synkra-core-viejo");
        assertThat(k.adaptador.estado(LecturasDePrueba.NS).fase()).isEqualTo(FaseDespliegue.EN_CURSO);
    }

    @Test
    void unaGeneracionSinObservarEsEnCurso() {
        k.deployment(1, 2L, 1, 1, 1, 1L);
        k.podListo("synkra-core-viejo");
        assertThat(k.adaptador.estado(LecturasDePrueba.NS).fase()).isEqualTo(FaseDespliegue.EN_CURSO);
    }

    @Test
    void conTodosLosCamposCoherentesEsListo() {
        k.deployment(1, 2L, 1, 1, 1, 2L);
        k.podListo("synkra-core-nuevo");
        var e = k.adaptador.estado(LecturasDePrueba.NS);
        assertThat(e.fase()).isEqualTo(FaseDespliegue.LISTO);
        assertThat(e.razon()).isNull();
    }
}
