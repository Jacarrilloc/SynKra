package io.synkra.plugin.api;

/** Punto de extensión de ejemplo: validación KYC por país. */
public interface PuntoKyc {
    ResultadoKyc validar(SolicitudKyc solicitud);
}
