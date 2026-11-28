#!/usr/bin/env bash
# CA-8: cada regla nueva muerde (una copia de HEAD por mutacion). Desde la raiz del worktree, con JAVA_HOME y SynKra instalado.
W=$PWD; KC=$W/.kube/kind-synkra.yaml; E=$W/bitacoras/U08-C-evidencia
D=""
trap '[ -z "$D" ] || rm -rf "$D"' EXIT
nueva() { # nombre ruta sed-script : crea la copia, aplica la mutacion y muestra el cambio (si no cambia nada, FALLA)
  [ -z "$D" ] || rm -rf "$D"
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  echo; echo "=== mutacion $1"; echo "\$ sed -i '$3' $2"
  sed -i "$3" "$D/$2"
  if diff <(git show "HEAD:$2") "$D/$2"; then echo "FALLA mutacion $1: el sed no cambio nada"; fi
}
sin_lista() { grep -v '^\[('; }

nueva base-sin-backend deploy/base/kustomization.yaml '/^  - backend$/d'
(cd "$D" && bash "$E/ca2.sh" 2>&1 | sin_lista | grep -E '^(FALLA|OK|base=|overlay=|solo-backend=)')

nueva overlay-solo-backend deploy/overlays/kind/kustomization.yaml 's|^  - ../../base$|  - ../../base/backend|'
(cd "$D" && bash "$E/ca2.sh" 2>&1 | sin_lista | grep -E '^(FALLA|OK|base=|overlay=|solo-backend=)')

nueva pull-ifnotpresent deploy/overlays/kind/backend-kind.yaml '/imagePullPolicy: Always/d'
(cd "$D" && bash "$E/ca2.sh" 2>&1 | sin_lista | grep -E '^(FALLA|OK|base=|overlay=|solo-backend=)')

nueva ingress-sin-nodo deploy/ingress-nginx/kustomization.yaml '/controlador-kind.yaml/d;/^patches:/d'
(cd "$D" && bash "$E/ca2.sh" 2>&1 | sin_lista | grep -E '^(ingress-nginx|2 True|ingress-ok|manifiesto-ok)'; echo "(sin 'ingress-ok' arriba: cae)")

nueva manifiesto-tocado deploy/ingress-nginx/deploy.yaml '1i # comentario anadido'
(cd "$D" && bash "$E/ca2.sh" 2>&1 | sin_lista | grep -E '^(manifiesto-ok|ingress-ok)'; echo "(sin 'manifiesto-ok' arriba: cae)")

nueva aplicar-base-k Makefile 's|apply -f deploy/base/namespaces.yaml -f deploy/base/rbac.yaml|apply -k deploy/base|'
(cd "$D" && KUBECONFIG_SYNKRA="$KC" make -n aplicar-base | grep -c -- '-k deploy/base')

nueva install-sin-limite Makefile '/\$(MAKE) verificar-limite/d'
(cd "$D" && sed -n '/^install:/,/^$/p' Makefile | grep -c 'verificar-limite')

nueva limite-sin-as scripts/verificar-limite.sh 's| --as=system:serviceaccount:synkra:deployer||'
(cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/verificar-limite.sh; echo "salida=$?")

nueva limite-sin-comparar scripts/verificar-limite.sh 's|elif \[ "\$respuesta" = "\$esperado" \]; then|elif true; then|'
(cd "$D" && KC="$KC" KUBECONFIG_SYNKRA="$KC" bash "$E/ca7.sh")

nueva desplegar-sin-digest scripts/desplegar.sh 's#^if \[ "\$EN_REGISTRO" != "\$EN_POD" \]; then#if false; then#; s#^\[ -n "\$EN_REGISTRO" \] && \[ "\$EN_REGISTRO" = "\$EN_POD" \] ||#[ -n "$EN_REGISTRO" ] ||#'
(cd "$D" && W="$W" KUBECONFIG_SYNKRA="$KC" bash "$E/ca5-variante.sh" 2>&1 | grep -E '^(variante=|registro-cambio|desplegar=|corre-la-variante|restaurado=|corre-lo-subido|OK desplegar|desplegar:)')

echo; echo "=== despues de las mutaciones que tocan el cluster, desde el worktree"
(cd "$W" && KUBECONFIG_SYNKRA="$KC" make desplegar 2>&1 | tail -2; echo "desplegar=${PIPESTATUS[0]}")
(cd "$W" && KUBECONFIG_SYNKRA="$KC" make verificar-limite 2>&1 | tail -2; echo "limite=${PIPESTATUS[0]}")
