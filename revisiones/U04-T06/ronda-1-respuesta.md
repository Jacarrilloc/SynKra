# U04-T06 — respuesta a la ronda 1

F-01: Corregido en 1933324: la validación ya usaba `Descriptor.NOMBRE` de U01 (sin copia). Se añadió la prueba paramétrica `claveDeLaPeticion_seAceptaSiYSoloSiCasaConElPatronDeU01`, que, para `a-`, `a--b`, `á`, `a_b`, `-a`, `A`, `""`, `" "`, `a b`, `a-\n`, `pais`, `a-b`, `a1`, `1` y `x-1-y`, compara el resultado de `subirPlugin` con `Descriptor.NOMBRE.matcher(clave).matches()`. M23 (la copia con `-?`) cae en `[1] clave=a-`. Quité la aserción que solo probaba U01.
F-02: Corregido en 1933324: el JAR se coloca con `Files.createLink(destino, temporal)`. El enlace no reemplaza nunca: si el destino ya existe, sale `FileAlreadyExistsException` y da 409 `CONFLICTO` con el mismo detalle `version` de R-C9, sin tocar el archivo del otro. Al deshacer, solo se borra el enlace si lo creó esta subida (`colocado`). Así, la exclusión ya no depende del pool de una conexión. Pruebas:
  - `destinoYaOcupadoPorUnArchivoSinFila_da409_yNoSeReemplazaNiSeBorra` es la vista determinista del perdedor: el ganador ya colocó el archivo y no hizo commit. Se comprueba el sha256 del ganador.
  - `dosSubidasSimultaneasDeLaMismaVersion_...`: dos hilos con una `CyclicBarrier`, 10 rondas. Hay un ganador y un 409, y el sha256 del disco coincide con el de la fila y con el del ganador.
  - El mutante «borrar el destino aunque no sea nuestro» cae en las tres pruebas de R-C9.
F-03: Corregido en 1933324: `<core>` y `<core>/<nombre>` se crean con `createDirectory`, un nivel a la vez, y se anota solo lo que creó esta subida. Si algo falla, eso se borra en orden inverso, y un directorio no vacío se deja. Pruebas:
  - `alFallar_seBorranLosDirectoriosQueCreoEstaSubida`;
  - `alFallar_noSeBorraElDirectorioDelCoreQueYaExistiaVacio`;
  - `alFallar_noSeTocaLaCarpetaDelPluginConOtraVersion`;
  - `siNoSePuedeCrearLaCarpeta_...`: el estorbo, un archivo donde va la carpeta, queda intacto.
F-04: Corregido en 1933324: el `Files.move(ATOMIC_MOVE)` se cambió por el enlace duro, que es atómico y no reemplaza (es la indicación del orquestador para F-02 y F-04, y se aparta de la letra de la decisión 6). Lo atan `destinoYaOcupadoPorUnArchivoSinFila_...` y `destinoOcupadoPorUnDirectorio_da409_...`. Los dos mutantes caen, con 2 fallos cada uno: M14 (`move` con `REPLACE_EXISTING`) y el `move` con `ATOMIC_MOVE` de la ronda 1. No hay respaldo con `move` para volúmenes sin enlaces duros. Las razones están en la bitácora, ronda 2.
F-05: Corregido en 1933324: un mapa de etiquetas nulo se trata como vacío. El Javadoc de `CatalogoApi.subirPlugin` ahora dice «un mapa nulo es un mapa vacío; una clave nula o fuera del patrón de U01, o un valor nulo, dan 422». La prueba es `mapaDeEtiquetasNulo_esUnMapaVacio` (antes daba NPE).

Candidatas que propuso el revisor, sin tocar:
- `a/` junto a `a` (U07, con C-46);
- nombres que solo difieren en mayúsculas;
- valores de etiqueta sin tope de longitud (U04-T07);
- C-13, que sigue abierta.

M42 es un mutante equivalente: sin la comprobación de nulos, `obtener` sigue dando 404. No se cambia.
