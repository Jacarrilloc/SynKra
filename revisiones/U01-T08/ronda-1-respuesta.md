# U01-T08, respuesta a la ronda 1
F-01: Corregido en el commit de la ronda 2: el lector es ahora `CodigoMotivoTest.nombresDeLasLineas(List<String>, String)`; encabezado y separador se saltan por posición y toda fila de datos que no empiece por un único nombre de código falla citando la línea. Regresión `lectorFallaSiLaFilaNoEsUnNombre` con `X-INVENTADO`, `` `X_INVENTADO` (nuevo) `` y `` `PLUGIN_AUSENTE`, `X_INVENTADO` ``. Con el lector viejo en una copia: 3 pruebas nuevas fallan (no lanza nada); con el nuevo pasan.
F-02: Corregido en el commit de la ronda 2: quitada la comprobación muerta de "Código"; el encabezado se trata por posición.
F-03: Corregido en el commit de la ronda 2: el lector se detiene en el siguiente `##` si aún no vio tabla y falla con "no trae una tabla"; prueba `lectorFallaSiLaSeccionNoTrajoTabla`. Además falla si la segunda línea no es separador (`lectorFallaSiFaltaElSeparador`).
F-04: Corregido en el commit de la ronda 2: `Veredicto.hayMotivos(...)` privado, usado por el constructor y por `de`.
F-05: Corregido en el commit de la ronda 2: `nulosDentroDeLasListasYAlternativasNulas` (siete caminos) y `distintosNoSonEquals`.
F-06: Aceptado: en esta ronda toda mutación (lector viejo, X_INVENTADO, PLUGIN_AUSENTE borrada) se hizo en copias de `git archive` en el scratchpad; el worktree no se editó para mutar.
