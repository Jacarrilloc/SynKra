package io.synkra.verificador;

import java.util.List;
import java.util.Objects;

/** Resultado de verificar una selección: {@code APROBADA} si y solo si no hay ningún motivo. */
public record Veredicto(Global global, List<VeredictoPlugin> porPlugin, List<Motivo> motivosGlobales) {

    /** Veredicto global. */
    public enum Global { APROBADA, RECHAZADA }

    public Veredicto {
        Objects.requireNonNull(global, "global");
        Objects.requireNonNull(porPlugin, "porPlugin");
        Objects.requireNonNull(motivosGlobales, "motivosGlobales");
        porPlugin = List.copyOf(porPlugin);
        motivosGlobales = List.copyOf(motivosGlobales);
        boolean hayMotivos = !motivosGlobales.isEmpty() || porPlugin.stream().anyMatch(p -> !p.motivos().isEmpty());
        if (global == Global.APROBADA && hayMotivos) {
            throw new IllegalArgumentException("APROBADA con motivos");
        }
        if (global == Global.RECHAZADA && !hayMotivos) {
            throw new IllegalArgumentException("RECHAZADA sin motivos");
        }
    }

    /** Calcula {@code global}: {@code APROBADA} solo si no hay ningún motivo. */
    public static Veredicto de(List<VeredictoPlugin> porPlugin, List<Motivo> motivosGlobales) {
        Objects.requireNonNull(porPlugin, "porPlugin");
        Objects.requireNonNull(motivosGlobales, "motivosGlobales");
        boolean hayMotivos = !motivosGlobales.isEmpty() || porPlugin.stream().anyMatch(p -> !p.motivos().isEmpty());
        return new Veredicto(hayMotivos ? Global.RECHAZADA : Global.APROBADA, porPlugin, motivosGlobales);
    }
}
