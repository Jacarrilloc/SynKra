#!/usr/bin/env bash
# CA-7: cada regla muerde. Una copia por mutación (git archive del último commit de código + git init), sin tocar el worktree.
cd "$(git rev-parse --show-toplevel)"; . bitacoras/U09-C-evidencia/lib.sh; cabecera
SHA=$(git log -1 --format=%H -- front front.Tests)
mut() { # nombre archivo expresion-sed prueba-esperada
  local n=$1 f=$2 e=$3 esperado=$4
  local D; D=$(mktemp -d); git archive "$SHA" | tar -x -C "$D"; (cd "$D" && git init -q)
  echo "=================== MUTACION $n  ($f)"; echo "sed -i '$e' $f"
  cp "$D/$f" "$D/original"
  sed -i "$e" "$D/$f"
  if cmp -s "$D/$f" "$D/original"; then echo "!! la mutacion no cambio nada"; rm -rf "$D"; return; fi
  diff "$D/original" "$D/$f" | sed 's/^/    /'; rm "$D/original"
  (cd "$D" && scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=m.trx" --results-directory front.Tests/TestResults 2>&1 | grep -E '^(Passed|Failed)!|error' | head -5; echo "salida=${PIPESTATUS[0]}")
  python3 -I bitacoras/U09-A-evidencia/trx.py "$D/front.Tests/TestResults/m.trx" | grep '^FALLA'
  if python3 -I bitacoras/U09-A-evidencia/trx.py "$D/front.Tests/TestResults/m.trx" | grep -qE "^FALLA ($esperado)\$"; then echo "-> OK: cae $esperado"; else echo "-> !! NO cae $esperado"; fi
  echo; rm -rf "$D"
}
E=front/Api/ApiSynkra.Escrituras.cs; P=front/Components/Pages
mut ruta-verificar $E 's#"api/verificaciones"#"api/verificacion"#' VerificarEnviaLaSeleccionYLeeElVeredicto
mut usuario-siempre $E 's#if (!string.IsNullOrWhiteSpace(usuario))#if (usuario is not null)#' UsuarioEnBlancoNoMandaLaCabecera
mut usuario-recorta $E 's#TryAddWithoutValidation(CabeceraUsuario, usuario)#TryAddWithoutValidation(CabeceraUsuario, usuario.Trim())#' UsuarioSeMandaTalCual
mut confirmar-cuerpo $E 's#/confirmacion", null, usuario#/confirmacion", JsonContent.Create(planId, options: Opciones), usuario#' ConfirmarPideLaRutaDelPlanSinCuerpo
mut etiquetas-parte $E 's#if (!string.IsNullOrWhiteSpace(etiquetas))#if (true)#;s#new StringContent(etiquetas, #new StringContent(etiquetas ?? "", #' 'SubirPluginSinEtiquetasNoMandaLaParte|SinEtiquetasNoSeMandaLaParte'
mut escape-core $E 's#{Uri.EscapeDataString(core)}/plugins"#{core}/plugins"#' SubirPluginEscapaElCore
mut preparar-global $P/Desplegar.razor 's#_veredicto is { Global: "APROBADA" }#_veredicto is not null#' ConVeredictoRechazadoLosBotonesEstanDeshabilitados
mut confirmar-plan $P/Desplegar.razor 's#PuedeConfirmar => _plan is not null \&\& #PuedeConfirmar => true || #' ConVeredictoAprobadoPrepararSeHabilitaYConfirmarNo
mut descarta-plan $P/Desplegar.razor '/private void CambioSeleccion/,/^    }/ s#_plan = null;#_ = _plan;#' CambiarLaSeleccionDescartaElPlan
mut pais-verifica $P/Desplegar.razor 's#_pais = e.Value?.ToString() ?? "";#_pais = e.Value?.ToString() ?? ""; CambioSeleccion();#' ElPaisNoEntraEnLaVerificacion
mut filtra $P/Desplegar.razor 's#@foreach (var p in _plugins)#@foreach (var p in _plugins.Where(x => _versionCore == "" || x.CoreCompatible.StartsWith(_versionCore[..1])))#' NoFiltraNiComparaVersiones
mut vieja-pisa $P/Desplegar.razor 's#if (numero != _secuencia || _desechado)#if (_desechado)#' RespuestaViejaNoPisaLaNueva
mut revertir-al-abrir $P/Reversion.razor 's#_origen = await Api.DespliegueAsync(Id);#_origen = await Api.DespliegueAsync(Id); _plan = await Api.RevertirAsync(Id, "");#' AbrirLaPaginaSoloLee
mut confirma-solo $P/Reversion.razor 's#_plan = await Api.RevertirAsync(Id, _usuario);#_plan = await Api.RevertirAsync(Id, _usuario); _confirmado = await Api.ConfirmarAsync(_plan.Id, _usuario);#' LaVueltaSoloSeConfirmaDespuesDeConfirmar
mut motivo-oculto $P/SubirPlugin.razor 's#@if (_error.Detalles.Count > 0)#@if (_error.Detalles.Count > 100)#' DescriptorInvalidoMuestraElMotivo
# «arranque»: la URL fija es la dirección de la propia aplicación (puerto de ConUrlValida): cualquier otra URL sin servidor da el mismo error que la de la prueba.
mut arranque front/Program.cs 's#c.BaseAddress = urlApi#c.BaseAddress = new Uri("http://127.0.0.1:18741/")#' ConUrlValidaArrancaYSirveLaVistaGeneral
# Variante: Program.cs ignora ConfiguracionApi.UrlBase (no valida la URL).
mut arranque-sin-validar front/Program.cs 's#var urlApi = ConfiguracionApi.UrlBase(builder.Configuration);#var urlApi = new Uri("http://127.0.0.1:9/");#' ConUrlMalaNoArranca
