package io.synkra.kycco;

import io.synkra.plugin.api.ContextoPlugin;
import io.synkra.plugin.api.PuntoKyc;
import io.synkra.plugin.api.ResultadoKyc;
import io.synkra.plugin.api.SolicitudKyc;
import io.synkra.plugin.api.SynkraPlugin;

/** Plugin de Colombia: regla KYC simulada (documento numérico de 6 a 10 dígitos). */
public final class PluginKycCo implements SynkraPlugin, PuntoKyc {
    static final String NOMBRE = "kyc-co";
    static final int MIN_DIGITOS = 6;
    static final int MAX_DIGITOS = 10;

    static final String MOTIVO_SOLICITUD_NULA = "La solicitud es nula";
    static final String MOTIVO_DOCUMENTO_NULO = "El documento es nulo";
    static final String MOTIVO_LONGITUD =
            "El documento debe tener de " + MIN_DIGITOS + " a " + MAX_DIGITOS + " caracteres";
    static final String MOTIVO_NO_DIGITOS = "El documento solo admite dígitos ASCII (0-9)";
    static final String MOTIVO_VALIDO = "Documento válido";

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
        // Defensa: SolicitudKyc ya prohíbe documento nulo en su constructor; no se confía en ello para no lanzar.
        if (d == null) {
            return new ResultadoKyc(false, MOTIVO_DOCUMENTO_NULO);
        }
        if (d.length() < MIN_DIGITOS || d.length() > MAX_DIGITOS) {
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
