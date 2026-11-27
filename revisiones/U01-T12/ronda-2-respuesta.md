# Respuesta a la ronda 2 (U01-T12)

F-01: Corregido (opción i): `core-b` se registra en 1 de cada 2 catálogos de texto. «NO registrado» sigue en `esperados`. Umbrales nuevos, elegidos para media >= 3x y P(fallo) < 1e-6 (Poisson): registrado >= 3 (media ~21, 7x, P 1,8e-7) y NO registrado >= 2 (media ~17,8, 8,9x, P 3,5e-7). Los de U01-T11 (15, 6, 10) no cambian. Tabla en la bitácora, «Ronda 3».
F-02: Corregido: `Caso` exige que `origen` concuerde con la variante de `Entrada`; `Esperado` rechaza FUERA_DE_U01 con motivos o alternativas, LECTOR con alternativas o sin motivos, APROBADA con motivos o alternativas y RECHAZADA sin motivos. Prueba `esperadoYEntradaIncoherentesSeRechazan` con 7 combinaciones que esperan `IllegalArgumentException`.
F-03: Corregido: `SuiteCombinaciones.contenido()` expone (inmutable) los cores, sus versiones y sus descriptores; `catalogo()` y `catalogoDeLaSuite()` salen de esa lista y `catalogoDeLaSuiteCubreTodoElCatalogo` compara contra ella y contra `catalogo()`. Mutación cat5 (añadir `kyc-ar 1.0.0` en `synkra-core` sin nombrarlo en ninguna selección): `Datos` la incluye y la prueba lo afirma (pasa con la aserción añadida en la copia); ya no se pierde.
F-04: Corregido: el javadoc huérfano se fundió en el de `catalogoDeLaSuite()`.
F-05: Corregido: la fila C-26 dice que `core-b` se registra en la mitad de los catálogos de texto.
