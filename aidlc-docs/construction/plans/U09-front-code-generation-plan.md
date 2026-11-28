# Plan de tareas: U09 Front

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian. Sin Functional Design: el front no tiene reglas propias (D3), toda decisión viene de la API.

Raíz del proyecto. Crea `front/` (Blazor Server, .NET 8) y `front.Tests/`, y añade
`deploy/base/front/`. Resumen en `aidlc-docs/construction/U09-front/code/`.

## Contexto

- **Historia dueña:** H-25.
- **Depende de:** U04 (catálogo), U06 (registro), U07 (despliegue) por el contrato de la API, y U08
  (Ingress y manifiestos).
- **Corte por riesgo R8:** si falta tiempo, la demo se hace con `./scripts/e2e-api.sh`; ninguna otra
  unidad depende de esta.

## Tareas

- [x] **U09-T01 · Proyecto Blazor Server y cliente de la API**
  `ApiSynkra` tipado sobre `HttpClient` con la URL del backend por configuración; los errores de la API se muestran con su `codigo` y `mensaje`.
  **Criterio:** `dotnet test front.Tests --filter ApiSynkraTest` pasa contra un servidor HTTP simulado. · H-25

- [x] **U09-T02 · Pantalla "Vista general"**
  Un renglón por país con core, versión, plugins activos y estado del último despliegue.
  **Criterio:** `dotnet test front.Tests --filter VistaGeneralTest` pasa (bUnit) con dos países simulados. · H-25

- [x] **U09-T03 · Pantalla "Catálogo"**
  Cores, versiones y plugins por core con sus etiquetas; subida de JAR que muestra el motivo de rechazo que devuelve la API.
  **Criterio:** `dotnet test front.Tests --filter CatalogoTest` pasa, incluido el caso de descriptor inválido con el motivo visible. · H-25

- [x] **U09-T04 · Pantalla "Desplegar" con verificación en vivo**
  Elegir país, core y plugins; cada cambio llama al endpoint de verificación y muestra el veredicto y los motivos; el botón Confirmar solo se habilita con un plan válido.
  **Criterio:** `dotnet test front.Tests --filter DesplegarTest` pasa: con veredicto incompatible el botón está deshabilitado. · H-25

- [x] **U09-T05 · Pantalla "Detalle"**
  Historial del país, contexto del fallo (eventos y estado de los pods que da la API) y "Volver al anterior" con confirmación.
  **Criterio:** `dotnet test front.Tests --filter DetalleTest` pasa y la reversión llama a la API solo después de confirmar. · H-25

- [x] **U09-T06 · Sin reglas de versión en el front**
  **Criterio:** `grep -rniE "semver|compatib" front/ --include='*.cs' --include='*.razor'` solo encuentra llamadas a `ApiSynkra`. · H-25

- [x] **U09-T07 · Imagen multi-stage sin root**
  **Criterio:** `docker build -t localhost:5001/synkra-front front/ && docker inspect -f '{{.Config.User}}' localhost:5001/synkra-front` no es vacío, `root` ni `0`. · H-28

- [x] **U09-T08 · Manifiesto del front**
  `deploy/base/front/`: Deployment, Service `synkra-front:8080` y la entrada en el `kustomization.yaml`.
  **Criterio:** `kubectl kustomize deploy/overlays/kind | kubeconform -strict -summary` sin errores y `make install` deja `synkra-front` `Running`. · H-26

- [ ] **U09-T09 · Recorrido de la demo y resumen**
  **Criterio:** `aidlc-docs/construction/U09-front/code/summary.md` registra el recorrido de CU2, CU4 y CU5 en `https://synkra.local` junto al log de la API, paso por paso, y la salida de `dotnet test`. · H-25

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-25 | T01 a T06, T09 |
| H-28 (contribuye) | T07 |
| H-26 (contribuye) | T08 |

**Terminada cuando:** `dotnet test front.Tests` pasa, el `grep` de T06 está limpio y el recorrido de T09 coincide con la API.
