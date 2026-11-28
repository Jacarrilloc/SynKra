package io.synkra.registro.api;

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
}
