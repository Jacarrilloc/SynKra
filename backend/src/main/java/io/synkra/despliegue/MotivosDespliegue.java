package io.synkra.despliegue;

/** Textos de {@code motivo} de los errores del módulo: constantes, sin repetir lo recibido (C-34). */
final class MotivosDespliegue {

    static final String PAIS_MENSAJE = "El país no está permitido";
    static final String PAIS_FORMA = "El país debe ser un código de dos letras en minúscula (S4)";
    static final String PAIS_FUERA_DE_LISTA = "El país no está entre los configurados (SYNKRA_PAISES)";

    private MotivosDespliegue() {
    }
}
