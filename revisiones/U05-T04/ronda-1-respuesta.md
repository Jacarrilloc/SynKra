# Respuesta a la ronda 1 — U05-T04

F-01: Corregido: `AdaptadorFabric8.aplicar` usa `ConstructorObjetosPais.GESTOR` en vez del literal; `AplicarTest` usa `GESTOR`, `NOMBRE_CORE` y `NOMBRE_CONFIG` en las aserciones y en `arranca()`. `grep -n '"synkra"' ` ya no encuentra literales fuera del constructor.
F-02: Corregido: `aplicar` lanza `IllegalStateException` («No hay cliente de Kubernetes...») sin peticiones cuando el cliente es nulo, tras la guardia de namespaces; el `catch` se acotó a `KubernetesClientException`. Prueba nueva `sinClienteAplicarFallaConErrorPropioYSinAcusarALaApi`, vista en rojo antes del arreglo (AssertionError en AplicarTest.java:227) y en verde después.
F-03: Corregido: la prueba ahora exige que las peticiones sean exactamente `PATCH, PATCH` (ConfigMap, Service). Visto en rojo contra una copia con un `DELETE` del ConfigMap tras el fallo (AssertionFailedError en AplicarTest.java:216) y en verde sobre el código real.
F-04: Corregido: el Javadoc de la clase dice ahora modo de expectativas, no CRUD.
F-05: Corregido: `peticiones()` lee exactamente `getRequestCount()` menos las ya leídas, sin cortar por 10 ms.
Candidatas del informe (apply contra servidor real; reintentos de Fabric8 vs U07): fuera de alcance, no tocadas; las registra el orquestador.
