# U02-T07 — respuesta a la ronda 1
F-01: Corregido en este commit (ronda 2): la copia del ObjectMapper activa FAIL_ON_TRAILING_TOKENS, STRICT_DUPLICATE_DETECTION y FAIL_ON_UNKNOWN_PROPERTIES (desconocidas = 400, decisión del orquestador); `cuerpoInvalido...` suma clave duplicada, basura, dos objetos y propiedad desconocida. Rojo en rojo-r2.txt; mutantes sin-trailing, sin-duplicados y sin-desconocidas mueren; CA-2 por proceso real da 400 en los cuatro.
F-02: Corregido en este commit (ronda 2): `invocar()` devuelve 500 PLUGIN_FALLO con el nombre si el plugin devuelve null; prueba `pluginQueDevuelveNullDa500...` (rojo en rojo-r2.txt, mutante null-es-200 muere; proceso real kyc-n -> 500).
F-03: Corregido en este commit (ronda 2): plugin de prueba `KYC_LINKAGE` (NoClassDefFoundError) y prueba que exige 500 PLUGIN_FALLO sin filtrar el texto; el mutante solo-runtime muere.
F-04: Corregido en este commit (ronda 2): los 404 comprueban que el mensaje contiene el nombre y `/kyc/KYC-A` da 404; mutantes 404-sin-nombre e igual-ignorando-mayusculas mueren.
F-05: Corregido en este commit (ronda 2): constante `PREFIJO_FALLO_KYC` en EndpointsCore.
F-06: Corregido en este commit (ronda 2): ca2-r2.txt pega fuente, synkra-plugin.yaml, META-INF/services y `jar tf` de cada plugin.
