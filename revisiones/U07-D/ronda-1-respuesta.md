# U07-D, respuesta a la ronda 1

F-01: Corregido en 45b4248: pruebas nuevas (excepción inesperada de `aplicar` en `ConfirmarSinClusterTest`; `reservar` con el reloj en `venceEn` en el caso `ar`; lectura de vuelta de plugins, sha256 y etiquetas por la API y por `reg_plugin` en el caso `cl`); M1 a M5 caen todas con BUILD FAILED (`bitacoras/U07-D-evidencia/ca5-r2.txt`).
F-02: Corregido en la bitácora y en `ca0.txt` (el diagnóstico era falso: el `0` salió del arnés que expandió `${...}`); `ca0-r2.txt` pega el comando con comillas simples como archivo, da 1.
F-03: Corregido en 45b4248: `Plan.java` revertido (`git diff ade68f6 -- Plan.java` vacío); el defecto del `grep` de `reg_` queda declarado en la bitácora, con un `grep` sobre literales SQL que da 1 (`tablas-reg-sql=1`); C-122 lo registra el orquestador.
F-04: Corregido en 45b4248: `ec` compara método y ruta con `tresPatch("ec")`.
F-05: Corregido en 45b4248: `EstadoDespliegue.APLICANDO` y la constante `RUTA_DESPLIEGUES` en `RutasConfirmacion`.
