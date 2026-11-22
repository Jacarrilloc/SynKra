# Mapa de historias a unidades: SynKra

Cada historia tiene **una sola unidad dueña**: la que la da por terminada cuando sus criterios
pasan. "Contribuyen" son las unidades que construyen piezas que la historia necesita; sus tareas
trazan a la historia, pero no la cierran.

## E1 · Catálogo de compilados

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-01 Registrar un core | Must | U04 | U03 |
| H-02 Subir un plugin | Must | U04 | U01 (descriptor) |
| H-03 Rechazar un descriptor inválido | Must | U04 | U01 (validación) |
| H-04 Rechazar artefactos peligrosos | Must | U04 | |
| H-05 Filtrar por etiqueta | Must | U04 | |
| H-06 Carpetas independientes | Must | U04 | U07 (rechazo en el plan) |

## E2 · Motor de verificación

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-07 Una regla, en una librería | Must | U01 | |
| H-08 Explicar por qué no sirve | Must | U01 | U07 (expone las alternativas) |
| H-09 Verificar sin desplegar | Must | U07 | U01 |
| H-10 La suite bloquea el build | Must | U01 | U02 (la corre en el core) |

## E3 · Motor de despliegue

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-11 Preparar y ver el resumen | Must | U07 | U06 (lo que corre hoy) |
| H-12 Confirmar y desplegar | Must | U07 | U05 (objetos y aplicación) |
| H-13 Nada fuera de `synkra-*` | Must | U07 | U05 (segunda capa), U08 (RBAC) |
| H-14 Listo o fallido, sin reintentos | Must | U07 | U05 (estado) |
| H-15 El pod solo recibe lo elegido | Must | U05 | |
| H-16 Contexto de un fallo | Must | U07 | U05 (log y eventos), U06 (lo guarda) |
| H-17 Abandonar no despliega | Must | U07 | |

## E4 · Registro y reversión

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-18 Todo queda registrado y sobrevive al reinicio | Must | U06 | U03 (SQLite en `/data`), U08 (PVC) |
| H-19 Vista general leída del clúster | Must | U06 | U05 (lectura) |
| H-20 Volver al anterior | Must | U07 | U06 |

## E5 · Core y plugins

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-21 El core verifica al arrancar | Must | U02 | U01 |
| H-22 Un plugin que falla no deja el core a medias | Must | U02 | |
| H-23 Plugins de Colombia y Perú | Must | U02 | |

## E6 · API y front

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-24 Los cinco casos de uso sin el front | Must | U08 | U03 (formato de error), U04, U06, U07 |
| H-25 Usar SynKra desde el front | Must | U09 | |

## E7 · Instalación y límite

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-26 Instalar con un comando | Must | U08 | |
| H-27 Comprobar el límite de la cuenta | Must | U08 | |
| H-28 Imágenes mínimas y sin root | Must | U08 | U02, U03, U05, U09 (sus Dockerfile) |

## E8 · Should

| Historia | Prio. | Dueña | Contribuyen |
|---|---|---|---|
| H-29 API documentada | Should | U03 (tarea final Should) | U04, U06, U07 (anotaciones) |
| H-30 Iniciar sesión y roles | Should | U10 | |
| H-31 Solo imágenes del registro permitido | Should | U04 (tarea final Should) | |
| H-32 Endurecer los namespaces | Should | U11 | |
| H-33 Entorno de desarrollo y escaneo | Should | U11 | |
| H-34 SynKra con GitOps | Should | U11 | |

## Resumen de cobertura

| Unidad | Dueña de | Total |
|---|---|---:|
| U01 | H-07, H-08, H-10 | 3 |
| U02 | H-21, H-22, H-23 | 3 |
| U03 | H-29 | 1 |
| U04 | H-01 a H-06, H-31 | 7 |
| U05 | H-15 | 1 |
| U06 | H-18, H-19 | 2 |
| U07 | H-09, H-11 a H-14, H-16, H-17, H-20 | 8 |
| U08 | H-24, H-26, H-27, H-28 | 4 |
| U09 | H-25 | 1 |
| U10 | H-30 | 1 |
| U11 | H-32, H-33, H-34 | 3 |
| **Total** | | **34** |

**Las 34 historias tienen dueña y ninguna tiene dos.** Comprobación:

```text
$ grep -oE '^\| H-[0-9]{2}' unit-of-work-story-map.md | sort -u | wc -l
34
```

## Dos cosas que mirar en este mapa

1. **U07 es la unidad más cargada** (8 historias). Es el corazón de P2: plan, confirmación,
   vigilancia y reversión. Separarla rompería la regla de que solo un módulo aplica cambios al
   clúster; en su lugar, su plan de tareas va en capas (plan → confirmación → vigilancia →
   reversión), cada una con su prueba.
2. **U03 y U05 son dueñas de casi nada** pero están en el camino crítico. Es lo esperable de una
   base y un adaptador: sus tareas trazan a las historias a las que contribuyen (H-18, H-24, H-12,
   H-14, H-16, H-19).
