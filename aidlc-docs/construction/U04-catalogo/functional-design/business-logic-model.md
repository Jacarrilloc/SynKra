# U04 · Modelo de lógica (Catálogo)

## Subida de un plugin

```mermaid
flowchart TD
    A["POST /api/cores/c/plugins con JAR y etiquetas"] --> B{"existe el core c"}
    B -->|no| E404["404"]
    B -->|si| C["leer a temporal con limite de tamano y SHA-256"]
    C -->|pasa el limite| E413["413"]
    C --> D{"rutas del JAR seguras"}
    D -->|no| E1["422 RUTA_NO_PERMITIDA"]
    D -->|si| F["LectorDescriptor de U01"]
    F -->|motivos| E2["422 con motivos"]
    F --> G{"core del descriptor igual a c"}
    G -->|no| E3["422 CORE_EQUIVOCADO"]
    G --> H{"ya existe nombre y version"}
    H -->|si| E409["409"]
    H --> I{"etiquetas sin choque"}
    I -->|no| E4["422 ETIQUETA_EN_CONFLICTO"]
    I --> J["mover a la ruta final y guardar metadatos"]
    J --> K["201 con descriptor y sha256"]
```

_Texto alternativo:_ la subida comprueba en orden que el core existe, el tamaño, las rutas del
JAR, el descriptor, que el descriptor sea de ese core, que no exista ya esa versión y que las
etiquetas no choquen. Solo si todo pasa, el archivo temporal se mueve a su ruta final y se
guardan los metadatos. Cada fallo tiene su código.

## Interfaz pública del módulo (`catalogo.api`)

`registrarCore`, `obtenerCore`, `subirPlugin`, `listarPlugins`, `obtenerPlugin`,
`abrirArtefacto(sha256)`, y la implementación de `Catalogo` de U01 (`versionesCore`,
`versionesPlugin`, `descriptor`) que usa U07.

## Cumplimiento

AUTONOMIA-01: N/A (no toca el clúster). AUTONOMIA-02: cada regla R-C tiene su prueba y su `curl`
en el plan de tareas. PBT parcial: la comprobación de rutas del JAR (R-C6) admite una propiedad
"ninguna entrada aceptada resuelve fuera del directorio" (PBT-03).
