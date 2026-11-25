package io.synkra.plataforma;

/** Un motivo concreto de un error: el campo afectado y por que (H-24). */
public record DetalleError(String campo, String motivo) {
}
