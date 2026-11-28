package io.synkra.despliegue;

/** La imagen del core: {@code antes} (nula si el país está vacío), {@code despues} e {@code igual}. */
record CambioCore(String antes, String despues, boolean igual) {}
