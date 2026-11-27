package io.synkra.plugin.api;

/** Contrato de ciclo de vida de todo plugin. Una excepción en {@code iniciar} impide que el core quede listo. */
public interface SynkraPlugin {
    String nombre();

    void iniciar(ContextoPlugin contexto);

    void detener();
}
