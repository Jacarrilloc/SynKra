package io.synkra.despliegue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * El único sitio con {@code @Scheduled} del backend. {@code fixedDelay}: una pasada empieza cuando terminó la
 * anterior, así que dos nunca se solapan; el planificador de Spring Boot tiene un solo hilo, así que {@code vigilar}
 * y {@code vencer} tampoco corren a la vez. Sin {@code initialDelay}: la primera pasada corre al arrancar, y por eso
 * un reinicio retoma lo que estaba {@code aplicando} (R-D12).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "synkra.programacion.activa", havingValue = "true")
class TareasProgramadas {

    private final VigilanciaDespliegues vigilancia;
    private final VencimientoPlanes vencimiento;

    TareasProgramadas(VigilanciaDespliegues vigilancia, VencimientoPlanes vencimiento) {
        this.vigilancia = vigilancia;
        this.vencimiento = vencimiento;
    }

    @Scheduled(fixedDelayString = "${synkra.vigilancia.intervalo}")
    void vigilar() {
        vigilancia.revisar();
    }

    /** C-109: los planes que nadie toca vencen solos. */
    @Scheduled(fixedDelayString = "${synkra.planes.barrido}")
    void vencer() {
        vencimiento.vencerPendientes();
    }
}
