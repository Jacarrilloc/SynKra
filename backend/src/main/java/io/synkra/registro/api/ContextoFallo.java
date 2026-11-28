package io.synkra.registro.api;

/** Contexto de un fallo (R-R4). Tipo propio: el registro no depende de {@code kubernetes}. Un nulo cuenta como "". */
public record ContextoFallo(String logCore, String logInit, String eventos) {

    public ContextoFallo {
        logCore = logCore == null ? "" : logCore;
        logInit = logInit == null ? "" : logInit;
        eventos = eventos == null ? "" : eventos;
    }
}
