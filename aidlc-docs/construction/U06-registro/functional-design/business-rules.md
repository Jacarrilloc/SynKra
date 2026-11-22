# U06 · Reglas de negocio (Registro)

| ID | Regla |
|---|---|
| R-R1 | Un despliegue se registra en estado `aplicando` en la misma transacción en que U07 marca su plan como confirmado |
| R-R2 | Estados permitidos: `aplicando → listo` o `aplicando → fallido`. Ningún otro cambio; un despliegue terminado es inmutable |
| R-R3 | `fallido` exige contexto (R-R4); `listo` no lo guarda |
| R-R4 | Contexto del fallo (Q5): últimas 200 líneas del log del core, log del init container, eventos del pod y del Deployment; cada texto truncado a 64 KB con la marca `[truncado]` |
| R-R5 | Historial por país ordenado del más reciente al más antiguo |
| R-R6 | Concordancia (Q4): un país `concuerda` si su último despliegue `listo` tiene la misma versión del core, el mismo digest de imagen y el mismo conjunto de (plugin, versión, sha256) que lo leído del clúster. Sin despliegue `listo` y sin nada en el clúster: `concuerda`; uno sin el otro: no |
| R-R7 | La vista general recorre los países configurados (`SYNKRA_PAISES=co,pe`), no los namespaces que encuentre |
| R-R8 | Todo sobrevive a un reinicio: el estado vive solo en SQLite |
