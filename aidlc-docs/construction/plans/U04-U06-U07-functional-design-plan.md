# U04, U06 y U07 · Plan de Functional Design (ronda conjunta)

Las tres unidades del backend con lógica de negocio, en una sola ronda de preguntas (propuesta
aceptada por Julian el 22-nov-2026). Cada unidad produce sus propios artefactos en
`aidlc-docs/construction/<unidad>/functional-design/` (`business-logic-model.md`,
`business-rules.md`, `domain-entities.md`).

## Pasos

- [ ] 1. U04 Catálogo: entidades y tablas (cores, versiones, plugins, etiquetas), reglas de subida (preguntas 1 a 3)
- [ ] 2. U06 Registro: entidades y tablas (despliegues, contexto), concordancia y contexto del fallo (preguntas 4 y 5)
- [ ] 3. U07 Despliegue: entidades y tablas (planes), máquina de estados, concurrencia y reversión (preguntas 6 a 9)
- [ ] 4. Comprobar que ningún módulo lee las tablas de otro (D14)
- [ ] 5. Revisar cumplimiento de extensiones (AUTONOMIA-01 en U07 sobre todo)

---

## Preguntas

Letra después de `[Answer]:`, o "aprobado" en el chat para aceptar las recomendaciones.

### U04 · Catálogo

## Question 1
¿De dónde salen las etiquetas de país, región o cliente de un plugin?

A) Del descriptor, y la subida puede **añadir** etiquetas nuevas pero no contradecir las del descriptor (si chocan, 422)

B) Solo del descriptor

C) Solo de la petición de subida

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El desarrollador declara lo esencial en el JAR, y el publicador puede
etiquetar por cliente sin recompilar; el choque se rechaza para que no haya dos verdades.

[Answer]: 

## Question 2
¿Qué pasa si se sube otra vez el mismo plugin con la misma versión?

A) Se rechaza con 409: un artefacto publicado es inmutable (mismo nombre y versión = mismo hash, siempre)

B) Se reemplaza

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Si una versión pudiera cambiar de contenido, el registro de despliegues
dejaría de decir qué se desplegó (P7) y el hash del ConfigMap quedaría viejo.

[Answer]: 

## Question 3
¿Se pueden borrar cores o plugins del catálogo en el MVP?

A) No. El catálogo solo crece; retirar algo queda fuera del MVP

B) Sí, solo si nunca se desplegó

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Ninguna historia lo pide, y sin borrados una reversión siempre encuentra sus
artefactos. El criterio de H-20 sobre "el anterior ya no es compatible" se mantiene como prueba
defensiva con un registro manipulado.

[Answer]: 

### U06 · Registro

## Question 4
¿Qué compara la marca de concordancia de la vista general?

A) Versión del core, **digest de la imagen**, y el conjunto de plugins con versión y hash leídos del ConfigMap, contra el último despliegue `listo` del país

B) Solo la versión del core y los nombres de los plugins

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El digest detecta que alguien cambió la imagen a mano, y el hash que alguien
cambió un plugin sin pasar por SynKra (RNF-11).

[Answer]: 

## Question 5
¿Cuánto contexto se guarda de un despliegue fallido?

A) Las últimas 200 líneas del log del core, el log del init container, los eventos del pod y del Deployment, la combinación, el veredicto y quién confirmó; cada texto truncado a 64 KB

B) Todo el log, sin límite

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Alcanza para diagnosticar y no llena SQLite con un pod en bucle de reinicios.

[Answer]: 

### U07 · Despliegue

## Question 6
¿Qué pasa con un plan `pendiente` que nadie confirma?

A) Vence a los 30 minutos (estado `vencido`); al confirmar se **vuelve a verificar** la combinación, por si el catálogo cambió entre medias

B) No vence nunca; se confirma tal como se creó

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Un plan viejo confirmado sin volver a verificar rompería P1, y que venza deja
limpio el caso del journey 7.3.

[Answer]: 

## Question 7
¿Qué pasa si se confirma un despliegue en un país que ya tiene otro `aplicando`?

A) Se rechaza con 409: un solo despliegue en curso por país

B) Se encola y se aplica al terminar el anterior

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Encolar sería aplicar algo sin que un humano mire el resultado del anterior
(P2).

[Answer]: 

## Question 8
¿Se puede crear un plan idéntico a lo que ya corre (por ejemplo, para reintentar a mano tras un
`fallido`)?

A) Sí; el resumen dice "sin cambios frente a lo que corre" y requiere la misma confirmación

B) No; 422 "sin cambios"

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Es la forma de reintentar que respeta P2: el reintento lo decide y lo
confirma una persona, no SynKra.

[Answer]: 

## Question 9
¿Quién puede confirmar un plan?

A) Cualquier usuario con rol Desplegador (no tiene que ser quien lo creó); el registro guarda quién creó y quién confirmó

B) Solo quien lo creó

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Permite que el SRE con rol Desplegador confirme una reversión que preparó
otro, y la traza conserva a los dos.

[Answer]: 
