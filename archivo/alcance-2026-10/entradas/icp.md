# Ideal Customer Profile — SynKra

> Hipótesis explícita y falsable, derivada de la sección 2 del PVB. No es un hallazgo de
> investigación.

## Beachhead

Equipos de plataforma o de entrega de **5 a 20 ingenieros**, en empresas de software que
despliegan **un producto en 3 o más países o reguladores** y que **ya operan Kubernetes con
GitOps** en producción. Vertical inicial: vendors de software financiero, salud y govtech en
Colombia y LatAm, y fintechs multi-país.

### Señales de calificación

1. Tienen Argo CD o Flux funcionando, aunque sea a medias.
2. Tienen más variantes por país que personas capaces de explicar en qué se diferencian.
3. Alguien mantiene a mano una hoja de "qué país tiene qué".
4. Su política de seguridad limita enviar código a un SaaS externo.

### Fuera del ICP

- Productos de un solo país (no hay nada que variar).
- Equipos sin Kubernetes ni GitOps.
- Empresas que ya resolvieron la variación solo con parametrización (tipo Temenos).

## Buyer personas

| Persona | Rol | Qué le importa | Poder |
|---|---|---|---|
| Arquitecto de seguridad / CISO | Veto de confianza | "¿El código sale de mi perímetro?" | **Veto** |
| Tech lead / release manager | Dueño de las ramas protegidas | "¿Qué puede escribir el agente en mi repo?" | **Veto** |
| Analista de compliance | Registra el cambio regulatorio ya interpretado | Saber a qué países y módulos aplica | Usuario |
| Product Owner | Recibe historias en borrador | No tener que investigar dónde va el cambio | Usuario |
| Desarrollador | Implementa sobre el PR en borrador | Que el borrador apunte al plugin correcto | Usuario |

## Triggers de compra

- Llega una norma nueva con fecha límite que afecta a varios países.
- Una auditoría encuentra un país que se quedó sin un cambio obligatorio.
- Rota la persona que sabía qué país tenía qué.

## Objeciones

| Objeción | Respuesta |
|---|---|
| "No mando mi código a una API externa" | Adaptador de modelo local; el modo Claude es solo para el piloto |
| "Copilot ya hace PRs" | Copilot no sabe qué plugin de qué país toca una norma; SynKra parte del cambio regulatorio |
| "¿Y si el agente se equivoca?" | Solo abre PRs en borrador en `synkra/*` y no puede fusionar, verificable en la protección de ramas |
