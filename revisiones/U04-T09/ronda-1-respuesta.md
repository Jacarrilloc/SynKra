F-01: Corregido en la ronda 2: S1 a S7 comparan además Verificador.alternativas con memoria y con caso.esperado().alternativas(); una mutación de coreCompatible hace fallar S4.
F-02: Corregido en la ronda 2: prueba con filas corruptas en cat_version_core y cat_plugin; versionesCore, versionesPlugin y descriptor lanzan IllegalStateException (la mutación que las oculta falla).
F-03: Corregido en la ronda 2: una sola constante SubidaPlugin.ORIGEN_DESCRIPTOR, reutilizada por CatalogoSqlite.
F-04: Corregido en la ronda 2: PluginRepositorio.findByCoreAndNombre y versionesPlugin lo usa.
F-05: Corregido en la ronda 2: CatalogoSqlite ya no es bean; CatalogoServicio lo construye y solo se obtiene por CatalogoApi.
