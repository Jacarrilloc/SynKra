# U04 · Reglas de negocio (Catálogo)

| ID | Regla | Error |
|---|---|---|
| R-C1 | Registrar un core: nombre con el patrón de U01, versión semver (R-V1), imagen con digest `@sha256:` de 64 hex. Si el core no existe, se crea su carpeta | 422 por campo |
| R-C2 | Una versión de core ya registrada no se vuelve a registrar | 409 |
| R-C3 | *(Should, H-31)* La imagen debe venir del registro permitido configurado (`SYNKRA_REGISTRO_PERMITIDO`) | 422 |
| R-C4 | Subir un plugin: la carpeta (core) debe existir | 404 |
| R-C5 | Tamaño máximo 20 MB, configurable (`SYNKRA_MAX_ARTEFACTO`); se corta la lectura al pasarlo, sin escribir en disco | 413 |
| R-C6 | El JAR no puede tener entradas con ruta absoluta, `..` ni enlaces; se revisa **antes** de guardar | 422 `RUTA_NO_PERMITIDA` |
| R-C7 | El descriptor se lee con `LectorDescriptor` de U01; sus motivos se devuelven todos | 422 con los códigos de U01 |
| R-C8 | `descriptor.core` debe ser el core de la carpeta (P4) | 422 `CORE_EQUIVOCADO` |
| R-C9 | Mismo `nombre` y `version` en la carpeta: rechazo (inmutable, Q2) | 409 |
| R-C10 | Etiquetas (Q1): las del descriptor, más las de la petición; una clave repetida con otro valor es un choque | 422 `ETIQUETA_EN_CONFLICTO` |
| R-C11 | El SHA-256 se calcula mientras se escribe el archivo; se guarda primero en un archivo temporal y se mueve a su ruta final solo si todo lo anterior pasó | |
| R-C12 | Listar una carpeta filtra por cualquier combinación de `clave=valor` (todas deben cumplirse); una carpeta solo muestra sus plugins (P4) | |
| R-C13 | Servir por hash: si no existe, 404; la respuesta trae `Content-Length` y el hash en una cabecera | 404 |
| R-C14 | No hay borrados en el MVP (Q3) | |
