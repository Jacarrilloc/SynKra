# Respuesta a la ronda 1 — U05-T06
F-01: Corregido: la prueba !!java afirma «etiqueta de tipo» y «ScriptEngineManager», y hay casos nuevos con !!set y !!binary que solo esa defensa nombra. El mutante `Tag etiqueta = null;` (en copia) ahora mata 4 casos de 18 (bitacoras/U05-T06-evidencia/mutante-2.txt); en la ronda 1 sobrevivía.
F-02: Corregido: CLAVE_NOMBRE, CLAVE_VERSION, SUFIJO_JAR y nombreJar(nombre, version) en ConstructorObjetosPais, usadas por el escritor, por el lector y por LINEA_ARTEFACTO.
F-03: Corregido: un escalar PLAIN que se lee como int/float/bool/null falla con «ambos como texto» y no con «etiqueta». Prueba versionSinComillasQueEsNumeroSeRechazaComoNoTextoNoComoEtiqueta; rojo en rojo-2.txt, verde en verde-ca1-r2.txt.
F-04: Corregido: la prueba del formato lee con CoreSchema, como el core.
F-05: Corregido: prueba unaFalloDeLaApiAlLeerSaleComoLecturaFallida con 403 y 500 en el GET del Deployment (el código ya cumplía; la prueba lo fija).
Candidatas (colisión de nombres de jar, topes repetidos): fuera de alcance, las registra el orquestador.
