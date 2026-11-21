# Personas: SynKra

Cuatro personas, una por usuario del PRD §3, identificadas **solo por su rol** (decisión del
21-nov-2026: sin nombres propios). El rol en SynKra es el de la tabla de permisos del PRD §9.2.
Es un ejercicio académico: las personas describen quién usa cada función en el demo, no un
mercado.

---

## P-OPE · Operador de la plataforma

| | |
|---|---|
| **Rol en SynKra** | Operador |
| **Qué hace** | Instala SynKra en el clúster, crea los namespaces de los países, registra los cores, configura el registro de imágenes permitido y gestiona los usuarios |
| **Cómo lo usa** | Sobre todo por la API y la línea de comandos (`make`, `kubectl`, `curl`) |
| **Lo que necesita** | Saber con certeza qué permisos de escritura tiene SynKra en el clúster, y poder comprobarlo con un comando |
| **No puede** | Desplegar fuera de `synkra-*`, ni siquiera él: lo impide el RBAC |
| **Journey** | PRD §7.2 |

## P-PUB · Desarrollador de plugins

| | |
|---|---|
| **Rol en SynKra** | Publicador |
| **Qué hace** | Sube los JAR de sus plugins a la carpeta de un core y los etiqueta por país, región o cliente |
| **Cómo lo usa** | Por la API (a futuro, desde un pipeline) o desde la pantalla "Catálogo" |
| **Lo que necesita** | Que un descriptor mal hecho se rechace al subirlo, con un motivo claro, y no al desplegar |
| **No puede** | Desplegar |
| **Journey** | PRD §7.2 (paso 5) y CU1 |

## P-RES · Responsable de país

| | |
|---|---|
| **Rol en SynKra** | Desplegador |
| **Qué hace** | Crea y cambia el despliegue de su país, revisa el resumen, confirma y vuelve al despliegue anterior |
| **Cómo lo usa** | Desde el front (pantallas "Desplegar", "Vista general" y "Detalle") |
| **Lo que necesita** | Elegir el core y los plugins de su país sin escribir YAML, y saber **antes** de confirmar si la combinación sirve y por qué no |
| **No puede** | Subir compilados |
| **Journey** | PRD §7.1 y §7.3; CU2, CU3 y CU4 |

## P-SRE · SRE o revisor

| | |
|---|---|
| **Rol en SynKra** | Lector; para revertir necesita además el rol Desplegador (ver la nota) |
| **Qué hace** | Consulta qué corre en cada país, revisa el registro y el contexto de un despliegue fallido, y decide volver al anterior |
| **Cómo lo usa** | Front ("Vista general", "Detalle") o API |
| **Lo que necesita** | Que el registro coincida con lo que corre en el clúster, y que un fallo le llegue con el contexto completo para decidir |
| **No puede** | Cambiar nada con el rol Lector solo |
| **Journey** | PRD §7.4; CU5 |

> [!NOTE]
> **Contradicción del PRD resuelta aquí.** El §3 dice que el SRE es "lector más reversión con
> confirmación", y el §9.2 dice que el Lector "no puede cambiar nada" y que volver al anterior es
> del Desplegador. Se aplica el §9.2: la reversión es un despliegue más, así que exige el rol
> Desplegador. Un SRE que revierte tiene los dos roles.

---

## Mapa de personas a épicas

| Épica | P-OPE | P-PUB | P-RES | P-SRE |
|---|:---:|:---:|:---:|:---:|
| E1 Catálogo de compilados | ✔ | ✔ | consulta | consulta |
| E2 Verificación | ✔ (suite) | ✔ (sabe qué sirve) | ✔ | |
| E3 Despliegue | | | ✔ | ✔ (contexto del fallo) |
| E4 Registro y reversión | | | ✔ | ✔ |
| E5 Core y plugins | ✔ | ✔ | | |
| E6 API y front | ✔ | ✔ | ✔ | ✔ |
| E7 Instalación y límite | ✔ | | | |

El detalle historia por historia está al final de `stories.md`.
