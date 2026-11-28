package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** U07-E, C-83: el contexto trae también los eventos del Deployment {@code synkra-core} y de sus ReplicaSet. */
class ContextoEventosTest {

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
    void sinPodsLlegaElFailedCreateDelReplicaSetYElEventoDelDeployment() {
        k.evento("e2", "ReplicaSet", "synkra-core-7d9f", "Warning", "FailedCreate", "exceeded quota: cuota-pe",
                "2026-01-01T00:00:02Z");
        k.evento("e1", "Deployment", "synkra-core", "Normal", "ScalingReplicaSet", "Scaled up replica set synkra-core-7d9f to 1",
                "2026-01-01T00:00:01Z");
        var c = k.adaptador.contextoFallo(LecturasDePrueba.NS);
        assertThat(c.eventos()).isEqualTo("Normal ScalingReplicaSet: Scaled up replica set synkra-core-7d9f to 1\n"
                + "Warning FailedCreate: exceeded quota: cuota-pe");
        assertThat(c.logCore()).isEmpty();
        assertThat(c.logInit()).isEmpty();
    }

    @Test
    void losEventosDeObjetosAjenosNoAparecen() {
        k.evento("e1", "ReplicaSet", "otro-5c4b", "Warning", "FailedCreate", "ajeno uno", "2026-01-01T00:00:01Z");
        k.evento("e2", "Deployment", "otro", "Normal", "ScalingReplicaSet", "ajeno dos", "2026-01-01T00:00:02Z");
        k.evento("e3", "Deployment", "synkra-core-7d9f", "Normal", "ScalingReplicaSet", "ajeno tres",
                "2026-01-01T00:00:03Z");
        k.evento("e4", "ReplicaSet", "synkra-core-7d9f", "Warning", "FailedCreate", "mio", "2026-01-01T00:00:04Z");
        assertThat(k.adaptador.contextoFallo(LecturasDePrueba.NS).eventos()).isEqualTo("Warning FailedCreate: mio");
    }

    @Test
    void conUnPodSusEventosYLosDelReplicaSetSalenMezcladosPorHora() {
        k.podFalla("synkra-core-7d9f-abc", 2);
        k.evento("e3", "Pod", "synkra-core-7d9f-abc", "Warning", "BackOff", "del pod", "2026-01-01T00:00:03Z");
        k.evento("e1", "ReplicaSet", "synkra-core-7d9f", "Normal", "SuccessfulCreate", "del replicaset",
                "2026-01-01T00:00:01Z");
        k.evento("e2", "Deployment", "synkra-core", "Normal", "ScalingReplicaSet", "del deployment",
                "2026-01-01T00:00:02Z");
        assertThat(k.adaptador.contextoFallo(LecturasDePrueba.NS).eventos().lines().toList()).containsExactly(
                "Normal SuccessfulCreate: del replicaset", "Normal ScalingReplicaSet: del deployment",
                "Warning BackOff: del pod");
    }
}
