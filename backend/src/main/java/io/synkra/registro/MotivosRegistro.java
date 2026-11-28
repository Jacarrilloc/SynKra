package io.synkra.registro;

/** Textos de {@code motivo} y {@code mensaje} de los errores del registro: un solo sitio dentro del módulo. */
final class MotivosRegistro {

    static final String MSG_PAIS_INVALIDO = "El país no es válido";
    /** No repite lo recibido (C-34). */
    static final String PAIS_FORMA = "El país debe ser un código de dos letras en minúscula (S4)";

    private MotivosRegistro() {
    }
}
