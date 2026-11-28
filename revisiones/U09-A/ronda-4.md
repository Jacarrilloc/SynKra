# Ronda 4 — U09-A (ronda corta: solo el CA-1 enmendado)

VEREDICTO: VERDE

## Comprobaciones previas
- **Hash de la tarea:** `git hash-object tareas/U09-A-proyecto-cliente-vista.md` da `0d01d83a7c08a791f308010c8fb6c4cd692e418e`, el valor esperado. El archivo no tiene cambios sin commit (`git status` y `git diff` salen vacíos).
- **La enmienda solo toca el CA-1:** está en el commit `fff434c`, y su diff sobre el archivo de la tarea son 5 líneas (+2/−3), todas dentro del CA-1:
  - se quita `command -v dotnet; echo "dotnet-local=$?"`;
  - el esperado `arm64` pasa a `amd64`, con la nota sobre 10.0.404 frente a 10.0.115;
  - se quita `dotnet-local=1`.

  Ningún otro CA cambia. El commit también toca `aidlc-docs/audit.md` (la entrada de las 05:31:12Z) y `loop/CONTINUAR.md`, que son documentos del orquestador y no del alcance del código.
- **Worktree:** la punta es `68738cb`. `git diff --stat c41f004 HEAD -- front front.Tests scripts` sale vacío (código de salida 0). El código no cambió desde la ronda 3, así que los CA-2 a CA-7 que verifiqué allí siguen vigentes.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Compila sin avisos, con versiones fijas (enmendado) | el bloque literal del CA-1 (líneas 163 a 172 de la tarea) | **pasa** |
| 2–7 | (verificados en la ronda 3) | `git diff c41f004 HEAD -- front front.Tests scripts` | vacío, siguen vigentes |

Detalle del CA-1 (máquina `x86_64`; el `dotnet --version` del sistema da `10.0.115`):
- **Imagen:** `mcr.microsoft.com/dotnet/sdk@sha256:8c6edfb26c85153f236d26efebd9ad349aec403583f343f201ee67838b23658d amd64`. Queda anotado el digest.
- **SDK:** `scripts/dotnet-docker.sh --version` da `10.0.404`. Como es distinto del SDK del sistema, el build corrió dentro del contenedor.
- **Build:** `Build succeeded.`, `0 Warning(s)`, `0 Error(s)`. Se generaron `Synkra.Front.dll` y `Synkra.Front.Tests.dll`; la restauración corrió de verdad (unos 540 ms por proyecto), así que no es un build vacío.
- **Proyectos:**
  - `net10.0`: 1 y 1.
  - `PackageReference` en el front: 0.
  - Paquetes del proyecto de pruebas: exactamente las cuatro líneas esperadas (bunit 2.15.2, Microsoft.NET.Test.Sdk 18.12.0, xunit 2.9.3, xunit.runner.visualstudio 3.1.5). `sort` las ordena según el locale, pero el conjunto es el mismo.
  - `TreatWarningsAsErrors`: 1 y 1.
- **Script:** `IMAGEN` aparece 1 vez y el archivo es `ejecutable`; `sobrantes=1`.

**Dónde corrí el bloque:** los comandos de archivos y de Docker corrieron sobre una copia de `HEAD` (`git archive HEAD`) en el scratchpad, para no generar `bin/` ni `obj/` en el worktree. La línea de `git ls-files` corrió en el propio worktree, porque necesita el índice. Los archivos rastreados de la copia son los mismos que los de `HEAD`.

## Hallazgos
- **F-08 (ROJO, ronda 3): cerrado** por la enmienda del CA-1. Con el criterio enmendado, el resultado es el esperado.
- **F-07 (AMARILLO): sigue como estaba.** No bloquea.
- No hay hallazgos ROJO ni NARANJA en pie.

## Tareas candidatas
- No hay nuevas.

## Estado del worktree
`git status --short --ignored` sale vacío antes y después de la corrida (`diff` dice `iguales`). No escribí nada en el worktree ni toqué el clúster kind.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r4/ca1.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r4/status-antes.txt
- /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U09-A-r4/status-despues.txt

**Informe sin escribir:** mis herramientas son de solo lectura y no permiten escribir en el worktree, así que este informe no está en `revisiones/U09-A/ronda-4.md`. Hay que pasarlo tal cual a esa ruta.

```
VEREDICTO: VERDE
AMARILLO|F-07 (ronda 3)|sin cambios, no bloquea
INFORME: revisiones/U09-A/ronda-4.md
```
