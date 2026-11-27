# U02-T03 — respuesta a la ronda 1
F-01: Corregido en ba7727d: `try`/`finally` cierra el cargador en toda salida que no sea éxito; `LinkageError` y `ServiceConfigurationError` se envuelven en `ErrorCargaPlugin` con el nombre del JAR; pruebas `proveedorInexistente...` y `claseFaltante...` comprueban cierre y nombre (la de clase faltante falló antes con NoClassDefFoundError, ver bitácora).
F-02: Corregido en ba7727d: solo se cuentan y devuelven proveedores con `type().getClassLoader() == cargador`; prueba `ignoraLosProveedoresDelCargadorPadre` (falló antes: un JAR vacío devolvía el plugin del padre).
F-03: Corregido en ba7727d: CA-2 pegado literal en la bitácora, con `grep -Ho` para imprimir el nombre del archivo (cambio del bucle declarado), incluida la línea de plugin-api.
F-04: Corregido en ba7727d: `crearCargador` es de paquete y sin Javadoc de extensión.
F-05: Anotado en la bitácora como límite de la prueba (mutantes equivalentes bajo Gradle).
F-06: Corregido en ba7727d: `valorComun` eliminado, `Map` importado, conteo 22/14/10 corregido en la bitácora; los dos encabezados CA-3 se aclaran (el histórico no se reescribe).
