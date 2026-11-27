package io.synkra.kubernetes;

import java.util.Map;
import org.springframework.context.annotation.ConditionContext;

/**
 * Variables de entorno del proceso con las que se ubica el clúster. En producción son siempre
 * {@link System#getenv()}, nunca el {@code Environment} de Spring (propiedades, argumentos, archivos).
 * El tipo es privado del paquete: solo el código de prueba puede registrar un bean que lo sustituya.
 */
record VariablesPod(Map<String, String> valores) {

    static VariablesPod de(ConditionContext contexto) {
        var fabrica = contexto.getBeanFactory();
        if (fabrica != null && fabrica.getBeanNamesForType(VariablesPod.class).length > 0) {
            return fabrica.getBean(VariablesPod.class);
        }
        return delProceso();
    }

    static VariablesPod delProceso() {
        return new VariablesPod(System.getenv());
    }
}
