# Límite de autonomía del agente SynKra

## Overview
Estas reglas son restricciones bloqueantes en todas las fases de AI-DLC. No son
recomendaciones. Cada etapa DEBE verificarlas antes de presentar su mensaje de
finalización. Vienen del principio P1 y P4 del PRD (`entradas/prd.md`, sección 6).

### Default Enforcement
Todas las reglas de este documento son **bloqueantes**. Si no se cumple un criterio de
verificación, es un hallazgo bloqueante: la etapa solo puede ofrecer «Request Changes».

---

## Rule AUTONOMIA-01: El agente solo propone; nunca fusiona ni despliega

**Rule**: Ninguna unidad de trabajo puede contener una tarea en la que el agente de SynKra
fusione, apruebe, escriba en `main` o aplique cambios a un clúster de Kubernetes. El agente
solo escribe en ramas con prefijo `synkra/*` y solo abre pull requests en borrador. El
destino de la cadena es un PR con evidencia adjunta que un humano revisa y fusiona.

**Verification**:
- Ningún plan de tareas contiene un paso del agente que ejecute `git merge`, `gh pr merge`,
  `git push` a `main`, `kubectl apply`, `helm install` o equivalente
- Existe al menos una tarea cuyo criterio comprueba el límite con comandos:
  `gh api repos/Jacarrilloc/SynKra/branches/main/protection` muestra revisión requerida, y
  `kubectl auth can-i create deployments --as=system:serviceaccount:synkra:agent -n synkra`
  responde `no`

---

## Rule AUTONOMIA-02: El gasto de IA está acotado y las pruebas no llaman a la API

**Rule**: Toda tarea que use un modelo de lenguaje lo hace a través del puerto
`LlmProvider`, con presupuesto diario de tokens y modelo pequeño por defecto. Las pruebas
automáticas usan el proveedor `fake` y nunca llaman a la API de Claude.

**Verification**:
- Ninguna tarea llama directamente al SDK de Anthropic fuera del adaptador `claude`
- Existe al menos una tarea cuyo criterio demuestra que con presupuesto 0 el agente responde
  "presupuesto agotado" sin llamar a la API
- La suite de pruebas corre con `LLM_PROVIDER=fake`

---

## Rule AUTONOMIA-03: Todo criterio de aceptación se verifica con un comando

**Rule**: Cada tarea de cada unidad DEBE tener un criterio de aceptación comprobable
ejecutando un comando. Las formulaciones de opinión no son criterios de aceptación. Si el
comportamiento cambia estado, el criterio nombra la lectura de vuelta con la herramienta
real (`kubectl get`, `gh pr view`, `git log`), no el código de salida del programa.

**Verification**:
- Ningún criterio de aceptación usa formulaciones como «funciona correctamente»,
  «es usable» o «tiene buen rendimiento» sin un comando o umbral medible
- Cada tarea nombra el comando, la prueba o la comprobación que demuestra que terminó
