package io.synkra.registro.api;

import java.util.List;
import java.util.Optional;

/** API pública del módulo Registro (R-R1 a R-R4). */
public interface RegistroApi {

    /**
     * R-R1: registra el despliegue siempre en {@code aplicando}, en la transacción de quien llama.
     *
     * @throws IllegalArgumentException si un argumento es inválido (no se guarda nada)
     */
    void registrar(NuevoDespliegue nuevo);

    /**
     * R-R2 y R-R3: solo {@code aplicando → listo} (sin contexto) o {@code aplicando → fallido} (con contexto).
     *
     * @throws java.util.NoSuchElementException si el id no existe
     * @throws TransicionNoPermitidaException si ya terminó o se pide {@code APLICANDO}
     * @throws IllegalArgumentException si falta el contexto de un fallo o sobra en un listo
     */
    void actualizarResultado(long id, EstadoDespliegue resultado, ContextoFallo contexto);

    /** El despliegue registrado con ese id; vacío si no existe (o si id ≤ 0). Trae el contexto si es un fallido. */
    Optional<DespliegueRegistrado> obtener(long id);

    /**
     * R-R5: los despliegues del país, del más reciente al más antiguo (empate: id mayor primero). Sin contexto.
     *
     * @throws IllegalArgumentException si el país no son dos letras minúsculas (nulo incluido)
     */
    List<DespliegueRegistrado> historial(String pais);

    /**
     * R-R6: el último despliegue {@code listo} del país (por el orden de R-R5), igual que {@link #obtener} (sin
     * contexto). Un {@code aplicando} o un {@code fallido} más reciente no cuenta. No mira {@code synkra.paises}.
     *
     * @throws IllegalArgumentException si el país no son dos letras minúsculas (nulo incluido)
     */
    Optional<DespliegueRegistrado> ultimoListo(String pais);
}
