#!/usr/bin/env bash
# CA-6: cada regla muerde. Una copia (git archive HEAD) por mutación, sin git stash ni cambios en el worktree.
# Uso, desde la raíz del repo: bash bitacoras/U09-A-evidencia/ca6.sh > bitacoras/U09-A-evidencia/ca6.txt 2>&1
REPO="$PWD"
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD)"
echo "imagen: $(docker image inspect --format '{{index .RepoDigests 0}} {{.Architecture}}' mcr.microsoft.com/dotnet/sdk:10.0.404)"

mutar() { # nombre, archivo, comando sed (se anota literalmente), pruebas esperadas separadas por espacio
  local nombre="$1" archivo="$2" sedcmd="$3" esperadas="$4"
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  echo; echo "=== mutación: $nombre"
  echo "\$ sed -i '$sedcmd' $archivo"
  sed -i "$sedcmd" "$D/$archivo"
  (cd "$D" && diff <(cd "$REPO" && git show "HEAD:$archivo") "$archivo")
  (cd "$D" && scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults > salida.txt 2>&1; echo "salida=$?"
   grep -E '^(Passed|Failed)!|error CS' salida.txt | cut -c1-220
   python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/m.trx | grep -E '^(total|FALLA)')
  (cd "$D" && python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/m.trx > trx.txt 2>&1)
  for p in $esperadas; do grep -q "^FALLA $p\$" "$D/trx.txt" && echo "ok: cae $p" || echo "NO CAE: $p"; done
  rm -rf "$D"
}

mutar ruta front/Api/ApiSynkra.cs 's|"api/paises"|"api/pais"|' "VistaGeneralLeeLosDosPaisesConTodosSusCampos MuestraUnRenglonPorPaisConLosDatosDeLaApi"
mutar nombre front.Tests/Fixtures/paises-dos.json 's/"versionCore"/"version_core"/' "VistaGeneralLeeLosDosPaisesConTodosSusCampos MuestraUnRenglonPorPaisConLosDatosDeLaApi"
mutar estricto front/Api/ApiSynkra.cs '/UnmappedMemberHandling/d' "CampoDesconocidoEnLaRespuestaDaRespuestaInvalida"
mutar error front/Api/ApiSynkra.cs 's|error = JsonSerializer.Deserialize<ErrorApi>(cuerpo, Opciones);|error = null;|' "Error404DaCodigoYMensajeDeLaApi Error422DaLosDetallesConCampoYMotivo ErrorDeLaApiMuestraCodigoYMensaje"
mutar escape front/Api/ApiSynkra.cs 's|Uri.EscapeDataString(pais)|pais|' "HistorialEscapaElPais"
mutar config front/Api/ConfiguracionApi.cs 's|UriKind.Absolute|UriKind.RelativeOrAbsolute|' "ConfiguracionConUrlRelativaNoArranca"
mutar concuerda front/Components/Pages/VistaGeneral.razor 's|null => "No se sabe"|null => "No"|' "PaisSinLecturaMuestraNoDisponibleYNoSeSabe"
mutar otra-llamada front/Components/Pages/VistaGeneral.razor 's|_paises = await Api.VistaGeneralAsync();|_paises = await Api.VistaGeneralAsync(); await Api.HistorialAsync("co");|' "SoloLlamaAGetApiPaises"
