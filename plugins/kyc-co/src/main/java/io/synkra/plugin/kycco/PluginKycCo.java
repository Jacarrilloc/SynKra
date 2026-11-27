package io.synkra.plugin.kycco;

import io.synkra.plugin.api.*;

public final class PluginKycCo implements SynkraPlugin, PuntoKyc {
    public String nombre() { return "kyc-co"; }
    public void iniciar(ContextoPlugin c) {}
    public void detener() {}
    public ResultadoKyc validar(SolicitudKyc s) { return new ResultadoKyc(false, "sin implementar"); }
}
