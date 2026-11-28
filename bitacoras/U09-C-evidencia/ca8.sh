#!/usr/bin/env bash
# CA-8: el diff queda acotado. ca8.txt se genera justo antes de su propio commit.
cd "$(git rev-parse --show-toplevel)"
echo "fecha=$(date -u +%FT%TZ) uname -m=$(uname -m)"
B=$(git merge-base HEAD main)
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat "$B"...HEAD
git diff --name-only "$B"...HEAD | grep -v -E '^(front/Api/(ApiSynkra|ApiSynkra\.Catalogo|ModelosCatalogo|ApiSynkra\.Escrituras|ModelosDespliegue)\.cs|front/Components/Pages/(Desplegar|Reversion|SubirPlugin)\.razor|front\.Tests/(ManejadorHttpSimulado|ApiSynkraEscriturasTest|DesplegarTest|ReversionDetalleTest|SubidaCatalogoTest|ArranqueTest)\.cs|front\.Tests/Fixtures/(desplegar|reversion|subida)-[a-z0-9-]+\.json|bitacoras/U09-C\.md|bitacoras/U09-C-evidencia/.*|revisiones/U09-C/.*)$'; echo "fuera=$?"
git diff --name-only "$B"...HEAD -- front/Components/Pages/Catalogo.razor front/Components/Pages/Detalle.razor front/Components/Pages/VistaGeneral.razor front/Components/Layout front/Program.cs front/Components/_Imports.razor front/Components/App.razor front/Components/Routes.razor front/wwwroot front/appsettings.json '*.csproj' front.Tests/ApiSynkraTest.cs front.Tests/VistaGeneralTest.cs backend deploy Makefile scripts | wc -l
git diff --name-status --diff-filter=D "$B"...HEAD | wc -l
