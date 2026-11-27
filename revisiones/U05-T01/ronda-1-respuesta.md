# U05-T01: respuesta a la ronda 1

- F-01: Corregido en 45e627c: se quita `synkra.kubernetes.url`; la URL sale solo de `System.getenv` (`KUBERNETES_SERVICE_HOST`/`PORT`), siempre https, vía el tipo `VariablesPod` que solo el código de prueba puede sustituir; pruebas `laUrlNoSaleDeLasPropiedadesDeSpring` y `conVariablesDelPodLaPropiedadDeUrlNoCambiaElServidor`; mutación M1 en rojo (bitácora).
- F-02: Corregido en 45e627c: `elKubeconfigDeLaMaquinaNoSeUsa` afirma `getFile()`, `getCurrentContext()` y `getNamespace()` nulos con kubeconfig falso en `@TempDir` y `user.home` falso; mutación `withAutoConfigure(true)` en rojo (con HOME y KUBECONFIG falsos).
- F-03: Corregido en 45e627c: todas las pruebas van por TLS (`KubernetesMockServer(true)`) con su cadena como `ca.crt`; `sinLaCaDeLaCuentaLaConexionTlsFalla`; mutación sin `withCaCertFile` en rojo.
- F-04: Corregido en 45e627c: un error al leer el token cuenta como sin token; prueba `unTokenIlegibleCuentaComoSinToken`; mutación en rojo.
- F-05: Corregido en 45e627c: caso `conTokenPeroSinKubernetesServiceHostNoHayCliente` (mutación en rojo) e imports ordenados; `disponible()` dinámico y la variable `KUBECONFIG` real quedan como están (opcional, anotado por el revisor).
