W=$PWD; D=$(mktemp -d); git archive 0fa392b | tar -x -C $D; mkdir $D/stub
for t in kind kubectl docker; do printf '#!/bin/sh\necho "stub: %s bloqueado en el rojo (AUTONOMIA-01)" >&2; exit 97\n' $t > $D/stub/$t; chmod +x $D/stub/$t; done
git show --stat 0fa392b | head -12
cd $D && PATH=$D/stub:$PATH KC=$D/no-hay.yaml bash bitacoras/U08-A-evidencia/ca4.sh
echo "--- verificar-base.py sobre kustomize inexistente (deploy/base no existe)"
kubectl kustomize deploy/base 2>&1 | python3 -I scripts/verificar-base.py; echo "salida=${PIPESTATUS[1]}"
cd /; rm -rf $D
