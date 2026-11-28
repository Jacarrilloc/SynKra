#!/usr/bin/env bash
# CA-7. Uso desde la raiz, con el arbol limpio: bash bitacoras/U09-B-evidencia/ca7.sh > bitacoras/U09-B-evidencia/ca7.txt 2>&1
echo "fecha: $(date -Is)"; set -x
B=$(git merge-base HEAD main)
git rev-parse --short HEAD; git status --porcelain | wc -l
git diff --stat "$B"...HEAD
git diff --name-only "$B"...HEAD | grep -v -E '^(front/Api/(ApiSynkra|ApiSynkra\.Catalogo|ModelosCatalogo)\.cs|front/Components/Pages/(Catalogo|Detalle|VistaGeneral)\.razor|front/Components/Layout/NavMenu\.razor|front\.Tests/(ApiSynkraCatalogoTest|CatalogoTest|DetalleTest|MenuTest|VistaGeneralTest)\.cs|front\.Tests/Fixtures/(catalogo|detalle)-[a-z0-9-]+\.json|bitacoras/U09-B\.md|bitacoras/U09-B-evidencia/.*|revisiones/U09-B/.*)$'; echo "fuera=$?"
git diff --name-only "$B"...HEAD -- front.Tests/ManejadorHttpSimulado.cs front/Program.cs front/Components/_Imports.razor front/Components/App.razor front/Components/Routes.razor front/wwwroot front/appsettings.json '*.csproj' backend deploy Makefile scripts | wc -l
git diff --numstat "$B"...HEAD -- front.Tests/VistaGeneralTest.cs front.Tests/Fixtures | awk '$2 != 0' | wc -l
git diff --name-status --diff-filter=D "$B"...HEAD | wc -l
