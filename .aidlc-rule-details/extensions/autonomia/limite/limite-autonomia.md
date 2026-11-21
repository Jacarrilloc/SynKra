# Límite de autonomía de SynKra

## Overview
Estas reglas son restricciones bloqueantes en todas las fases de AI-DLC. No son
recomendaciones. Cada etapa DEBE verificarlas antes de presentar su mensaje de
finalización. Vienen del principio **P2** (límite de autonomía verificable) y de los
principios P1 y P7 del PRD v2 (`Modulo 3/specs/prd.md`, segmento 6).

SynKra no tiene un agente de IA, pero **sí actúa sobre infraestructura**: despliega en el
clúster. El límite que protegen estas reglas es doble: el del producto (SynKra solo despliega
con confirmación humana y solo en `synkra-*`) y el del proceso (ninguna tarea del plan aplica
cambios a un clúster sin aprobación humana).

> Adaptada el 21-nov-2026 (Requirements Analysis, pregunta 1). La versión anterior, escrita
> para el agente de IA del alcance archivado, queda en el historial de Git.

### Default Enforcement
Todas las reglas de este documento son **bloqueantes**. Si no se cumple un criterio de
verificación, es un hallazgo bloqueante: la etapa solo puede ofrecer «Request Changes».

---

## Rule AUTONOMIA-01: SynKra solo despliega con confirmación humana y solo en `synkra-*`

**Rule**: Ningún requisito, historia, componente ni tarea puede permitir que SynKra aplique
cambios a un clúster sin una confirmación humana explícita y registrada (en el front o con un
comando explícito de la API o la CLI), ni fuera de los namespaces `synkra-*`. Ningún flujo
reintenta, revierte ni despliega por su cuenta. Además, ninguna tarea del plan aplica cambios
a un entorno compartido sin un paso previo de aprobación humana: el clúster `kind` local de
desarrollo y las pruebas efímeras sí, cualquier otro no.

**Verification**:
- La API separa crear un plan de despliegue de confirmarlo; un plan sin confirmar nunca toca
  el clúster, y existe una tarea cuyo criterio lo demuestra con `curl` y `kubectl get`
- Existe al menos una tarea cuyo criterio comprueba el RBAC con
  `kubectl auth can-i create deployments -n default --as=system:serviceaccount:synkra:deployer`
  y espera `no`
- Ningún plan de tareas contiene `kubectl apply`, `helm install` o equivalente contra un
  entorno que no sea el `kind` local o un clúster efímero de pruebas, sin un paso previo de
  aprobación humana
- Ningún requisito o historia describe un reintento, una reversión o un despliegue automático
  sin confirmación

---

## Rule AUTONOMIA-02: Todo criterio de aceptación se verifica con un comando

**Rule**: Cada historia y cada tarea de cada unidad DEBE tener un criterio de aceptación
comprobable ejecutando un comando. Las formulaciones de opinión no son criterios de
aceptación. Si el comportamiento cambia estado, el criterio nombra la lectura de vuelta con
la herramienta real (`kubectl get`, `curl` a la API, `sqlite3`), no solo el código de salida
del programa.

**Verification**:
- Ningún criterio de aceptación usa formulaciones como «funciona correctamente»,
  «es usable» o «tiene buen rendimiento» sin un comando o umbral medible
- Cada tarea nombra el comando, la prueba o la comprobación que demuestra que terminó
