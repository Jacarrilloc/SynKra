#!/usr/bin/env bash
# F-01: ningun comando externo queda mudo. Mutaciones en copias de HEAD. Desde la raiz del worktree, con SynKra instalado.
W=$PWD; KC=$W/.kube/kind-synkra.yaml
copia() { D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  cp -r .tls "$D/"; ln -s "$W/plugins/kyc-co/build" "$D/plugins/kyc-co/build"; ln -s "$W/plugins/kyc-pe/build" "$D/plugins/kyc-pe/build"
  mkdir -p "$D/.kube"; ln -s "$KC" "$D/.kube/kind-synkra.yaml"; }
echo "################ version-core: la linea version de core/build.gradle.kts cambia (el caso del revisor)"
copia; sed -i 's/^version = "2.1.0"/versión = "2.1.0"/' "$D/core/build.gradle.kts"; diff <(git show HEAD:core/build.gradle.kts) "$D/core/build.gradle.kts"
(cd "$D" && bash scripts/e2e-api.sh) 2>&1 | tail -3; echo "salida=${PIPESTATUS[0]}"; rm -rf "$D"
echo "################ version-core-2x: la version ya no cae en 2.x"
copia; sed -i 's/^version = "2.1.0"/version = "4.0.0"/' "$D/core/build.gradle.kts"
(cd "$D" && bash scripts/e2e-api.sh) 2>&1 | tail -3; echo "salida=${PIPESTATUS[0]}"; rm -rf "$D"
echo "################ jq-no-json: el cuerpo de un 200 no es JSON (en esperar_estado)"
copia; sed -i '0,/^    jq_c .estado$/s//    echo "<html>no es json<\/html>" > "$CUERPO"\n    jq_c .estado/' "$D/scripts/e2e-api.sh"; diff <(git show HEAD:scripts/e2e-api.sh) "$D/scripts/e2e-api.sh"
(cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/e2e-api.sh) 2>&1 | grep -v '^ok ' | tail -4; echo "salida=${PIPESTATUS[0]}"; rm -rf "$D"
echo "################ restaurado"
./scripts/e2e-api.sh 2>&1 | tail -2; echo "restaurado=${PIPESTATUS[0]}"
