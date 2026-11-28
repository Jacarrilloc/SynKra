#!/usr/bin/env bash
# CA-6: cada regla muerde. Una copia (git archive HEAD + git init) por mutacion; el worktree no se toca.
# Uso desde la raiz: bash bitacoras/U09-B-evidencia/ca6.sh > bitacoras/U09-B-evidencia/ca6.txt 2>&1
R=$(git rev-parse --show-toplevel); cd "$R"
echo "fecha: $(date -Is)"; echo "sha: $(git rev-parse --short HEAD) (ultimo commit de codigo: $(git log -1 --format=%h -- front front.Tests))"; echo "arch: $(uname -m)"
echo "imagen: $(docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404)"
CAT=front/Components/Pages/Catalogo.razor; DET=front/Components/Pages/Detalle.razor
mut() { # nombre ruta esperada viejo nuevo
  local nombre=$1 ruta=$2 esperada=$3 viejo=$4 nuevo=$5
  echo; echo "=== mutacion $nombre (esperada: $esperada)"
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  python3 -I -c '
import sys
p, v, n = sys.argv[1:4]
s = open(p, encoding="utf-8").read()
assert s.count(v) == 1, "la mutacion no casa exactamente una vez: " + v
open(p, "w", encoding="utf-8").write(s.replace(v, n))' "$D/$ruta" "$viejo" "$nuevo" || { echo "MUTACION NO APLICADA"; rm -rf "$D"; return; }
  echo "--- diff de la mutacion"; diff <(git show HEAD:"$ruta") "$D/$ruta" | grep '^[<>]'
  (cd "$D" && scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E 'Passed!|Failed!|error ' | head -3; echo "salida=${PIPESTATUS[0]}")
  python3 -I bitacoras/U09-A-evidencia/trx.py "$D/front.Tests/TestResults/m.trx" 2>/dev/null | grep '^FALLA'
  rm -rf "$D"
}
mut orden-versiones $CAT MuestraCadaCoreConSusVersionesEImagenes '@foreach (var v in core.Versiones)' '@foreach (var v in core.Versiones.OrderByDescending(x => x.Version, StringComparer.Ordinal))'
mut filtra $CAT NoFiltraNiComparaVersiones '@foreach (var p in plugins)' '@foreach (var p in plugins.Where(q => core.Versiones.Any(v => q.CoreCompatible.StartsWith(v.Version.Split((char)46)[0]))))'
mut etiquetas $CAT EtiquetasSeOrdenanPorClave 'p.Etiquetas.OrderBy(x => x.Key, StringComparer.Ordinal)' 'p.Etiquetas'
mut error-local $CAT ErrorEnLosPluginsDeUnCoreNoEscondeLosDemas '_errores[core.Nombre] = e;' '_error = e;'
mut subir $CAT EnlaceDeSubidaPorCore 'catalogo/@Uri.EscapeDataString(core.Nombre)/subir' 'catalogo/@core.Nombre/subir'
mut historial-orden $DET MuestraElHistorialDelPaisDelDespliegue '@foreach (var h in _historial)' '@foreach (var h in _historial.OrderByDescending(x => x.Id))'
mut contexto-estado $DET FallidoSinContextoNoMuestraLaSeccion '@if (d.Contexto is { } c)' '@if (d.Estado == EstadoDespliegue.Fallido && (d.Contexto ?? new ContextoFallo("", "", "")) is { } c)'
mut volver-listo $DET CadaRenglonEnlazaAlDetalleYAVolver '<td data-campo="accion"><a data-accion="volver" href="despliegues/@h.Id/volver">Volver a este</a></td>' '<td data-campo="accion">@if (h.Estado == EstadoDespliegue.Listo) {<a data-accion="volver" href="despliegues/@h.Id/volver">Volver a este</a>}</td>'
mut sin-historial $DET MuestraElHistorialDelPaisDelDespliegue 'pais = _despliegue.Pais;' '_cargado = true; return;'
mut fecha-local $DET FechasSeMuestranEnUtcYNulaComoGuion 'v.UtcDateTime.ToString(' 'v.ToOffset(TimeSpan.FromHours(-5)).ToString('
mut veredicto $DET VeredictoSeMuestraTalCual 'JsonSerializer.Serialize(d.Veredicto, SangriaJson)' 'JsonSerializer.Serialize(d.Veredicto.GetProperty("global"), SangriaJson)'
mut enlace-vista front/Components/Pages/VistaGeneral.razor UltimoDespliegueEnlazaAlDetalle '<a href="despliegues/@id">#@id</a>' '@($"#{id}")'
mut menu front/Components/Layout/NavMenu.razor MenuEnlazaLasTresPantallas '    <NavLink href="desplegar">Desplegar</NavLink>
' ''
echo; echo "fin"
