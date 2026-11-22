# U07 · Reglas de negocio (Despliegue)

| ID | Regla | Error |
|---|---|---|
| R-D1 | El país debe estar en `SYNKRA_PAISES` y tener el formato de S4; el namespace es siempre `synkra-<país>`, nunca viene del usuario | 422 `PAIS_NO_PERMITIDO` |
| R-D2 | Verificar = U01 `verificar` + `alternativas` sobre el catálogo (U04); no crea nada | |
| R-D3 | Crear plan: R-D1, luego R-D2; si `RECHAZADA`, 422 con el veredicto y no se guarda nada | 422 |
| R-D4 | El resumen lleva namespace, imagen con digest, plugins con versión y hash; los cambios se calculan contra el último `listo` del país (U06). Un plan idéntico a lo que corre se permite y lo dice (Q8) | |
| R-D5 | Confirmar: el plan debe estar `pendiente` y no vencido; si no, 409 o 410 `PLAN_VENCIDO` | 409, 410 |
| R-D6 | Al confirmar se **vuelve a verificar** (Q6); si ahora es `RECHAZADA`, 422 y el plan queda `pendiente` | 422 |
| R-D7 | Un solo despliegue `aplicando` por país (Q7) | 409 `DESPLIEGUE_EN_CURSO` |
| R-D8 | Cualquier Desplegador confirma (Q9); se registran `creado_por` y `confirmado_por` | |
| R-D9 | Confirmar = U05 `construir` + `aplicar` → U06 `registrar` (`aplicando`) → lanzar la vigilancia → 202 | |
| R-D10 | Vigilancia: consulta U05 `estado` cada 5 s hasta 3 min (configurables); listo si todas las réplicas están `Ready`; si vence o hay `CrashLoopBackOff`/`Init:Error`, `fallido` con el contexto de U05. **Nunca reintenta ni revierte** | |
| R-D11 | Reversión: el origen debe ser un despliegue `listo` del mismo país; se crea un plan `reversion` con su combinación y pasa por R-D3 a R-D10 igual que cualquier otro | 422 si el origen no es `listo` |
| R-D12 | Si el backend se reinicia con despliegues `aplicando`, al arrancar retoma su vigilancia (no los reaplica) | |
| R-D13 | Un plan `pendiente` nunca llama a U05 `aplicar` (AUTONOMIA-01) | |
