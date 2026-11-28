package io.synkra.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import io.synkra.kubernetes.api.AdaptadorKubernetes;
import io.synkra.kubernetes.api.PlanPais;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** U05-T03: la propiedad {@code synkra.kubernetes.imagen-init} llega al adaptador del contexto (sin clúster). */
class ImagenInitConfiguradaTest {

    private static final PlanPais PLAN = new PlanPais("pe", "localhost:5001/core-pe@sha256:" + "a".repeat(64), List.of());

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ConfiguracionKubernetes.class);

    private static String imagenInit(AdaptadorKubernetes adaptador) {
        return ObjetosPaisFabric8.de(adaptador.construir(PLAN)).deployment().getSpec().getTemplate().getSpec()
                .getInitContainers().get(0).getImage();
    }

    @Test
    void laPropiedadLlegaAlAdaptador() {
        runner.withPropertyValues("synkra.kubernetes.imagen-init=registro.local:5000/init:2.3.4")
                .run(ctx -> assertThat(imagenInit(ctx.getBean(AdaptadorKubernetes.class)))
                        .isEqualTo("registro.local:5000/init:2.3.4"));
    }

    @Test
    void sinPropiedadSeUsaElDefectoDelCodigo() {
        runner.run(ctx -> assertThat(imagenInit(ctx.getBean(AdaptadorKubernetes.class)))
                .isEqualTo(ConstructorObjetosPais.IMAGEN_INIT_POR_DEFECTO));
    }

    @Test
    void unValorInvalidoImpideArrancarElContexto() {
        for (var malo : List.of("", "init:1.0.0", "synkra/init:1.0.0", "localhost:5001/x:latest", "localhost:5001/x")) {
            runner.withPropertyValues("synkra.kubernetes.imagen-init=" + malo).run(ctx -> {
                assertThat(ctx).as("valor '%s'", malo).hasFailed();
                assertThat(ctx.getStartupFailure()).hasStackTraceContaining("synkra.kubernetes.imagen-init");
            });
        }
    }
}
