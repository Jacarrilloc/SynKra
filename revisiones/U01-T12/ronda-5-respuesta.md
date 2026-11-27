# Respuesta a la ronda 5 (U01-T12)

F-01: Corregido: el umbral de «suite en synkra-core con VERSION_INCOMPATIBLE» baja a 7 (media ~30,5, 4,4x). La tabla de la bitácora da, para cada puerta, la P(fallo) con Poisson y con binomial negativa usando la varianza medida por lote (2 x 100 lotes).
F-02: Corregido: `catalogosDeTextoRegistranCoreBSegunSuOrigen` exige que haya entre 100 y 200 catálogos con `core-b` registrado de 300 y que cada versión registrada de `core-b` caiga en el rango de los descriptores de `core-b`. En una copia `git archive`, 3 corridas cada una: reg13 (1 de cada 4), reg31 (3 de cada 4) y regver (`core-b` con 0.1.0) fallan 9 de 9 en esa prueba; el control sin mutar pasa 3 de 3.
