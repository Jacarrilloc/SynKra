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
            return new ResultadoKyc(false, "La solicitud es nula");
        }
        String d = solicitud.documento();
        if (d == null) {
            return new ResultadoKyc(false, "El documento es nulo");
        }
        if (d.length() < MIN_DIGITOS || d.length() > MAX_DIGITOS) {
            return new ResultadoKyc(
                    false, "El documento debe tener de " + MIN_DIGITOS + " a " + MAX_DIGITOS + " caracteres");
        }
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c < '0' || c > '9') {
                return new ResultadoKyc(false, "El documento solo admite dígitos ASCII (0-9)");
            }
        }
        return new ResultadoKyc(true, "Documento válido");
    }
}
