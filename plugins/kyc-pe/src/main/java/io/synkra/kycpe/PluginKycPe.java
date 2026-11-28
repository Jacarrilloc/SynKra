package io.synkra.kycpe;

import io.synkra.plugin.api.ContextoPlugin;
import io.synkra.plugin.api.PuntoKyc;
import io.synkra.plugin.api.ResultadoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import io.synkra.plugin.api.SynkraPlugin;

/** Plugin de Perú: regla KYC simulada (DNI de exactamente 8 dígitos ASCII). */
public final class PluginKycPe implements SynkraPlugin, PuntoKyc {
    static final String NOMBRE = "kyc-pe";
    static final int DIGITOS_DNI = 8;

    static final String MOTIVO_SOLICITUD_NULA = "La solicitud es nula";
    static final String MOTIVO_DOCUMENTO_NULO = "El documento es nulo";
    static final String MOTIVO_LONGITUD = "El DNI debe tener exactamente " + DIGITOS_DNI + " caracteres";
    static final String MOTIVO_NO_DIGITOS = "El DNI solo admite dígitos ASCII (0-9)";
    static final String MOTIVO_VALIDO = "DNI válido";

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public void iniciar(ContextoPlugin contexto) {}

    @Override
    public void detener() {}

    @Override
    public ResultadoKyc validar(SolicitudKyc solicitud) {
        if (solicitud == null) {
            return new ResultadoKyc(false, MOTIVO_SOLICITUD_NULA);
        }
        String d = solicitud.documento();
        // Defensa: SolicitudKyc ya prohíbe documento nulo; no se confía en ello para no lanzar.
        if (d == null) {
            return new ResultadoKyc(false, MOTIVO_DOCUMENTO_NULO);
        }
        if (d.length() != DIGITOS_DNI) {
            return new ResultadoKyc(false, MOTIVO_LONGITUD);
        }
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c < '0' || c > '9') {
                return new ResultadoKyc(false, MOTIVO_NO_DIGITOS);
            }
        }
        return new ResultadoKyc(true, MOTIVO_VALIDO);
    }
}
