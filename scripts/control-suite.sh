#!/usr/bin/env bash
# Demuestra que la suite S1-S12 bloquea el build (H-10). Trabaja en una copia `git archive HEAD`
# dentro de un directorio temporal; nunca toca el worktree. Termina con 0 solo si:
#   (1) sin cambios                          -> check = 0
#   (2) S4 alterado en SuiteCombinaciones    -> check != 0 (falla el caso S4 de SuiteCombinacionesTest)
#   (3) @Disabled sobre SuiteCombinacionesTest -> check != 0 (falla verificarSuite)
# Uso: bash scripts/control-suite.sh
set -euo pipefail

raiz="$(git rev-parse --show-toplevel)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
echo "temporal: $tmp"

fixtures=verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java
suite=verificador/src/test/java/io/synkra/verificador/SuiteCombinacionesTest.java
ok=1

# Corre check en una copia limpia ($tmp/$1) y guarda el log en $tmp/$1.log. Imprime el código de salida.
correr() {
  local nombre="$1" rc=0
  mkdir "$tmp/$nombre"
  git -C "$raiz" archive HEAD | tar -x -C "$tmp/$nombre"
  case "$nombre" in
    s2) sed -i '/"S4"/,/"S5"/ s/new Version(1, 3, 0)/new Version(1, 2, 0)/;/"S4"/,/"S5"/ s/v(1, 3, 0)/v(1, 2, 0)/' "$tmp/$nombre/$fixtures"
        cmp -s "$raiz/$fixtures" "$tmp/$nombre/$fixtures" && { echo "no se pudo alterar S4"; exit 2; } ;;
    s3) sed -i 's/^class SuiteCombinacionesTest/@org.junit.jupiter.api.Disabled\nclass SuiteCombinacionesTest/' "$tmp/$nombre/$suite"
        cmp -s "$raiz/$suite" "$tmp/$nombre/$suite" && { echo "no se pudo desactivar la suite"; exit 2; } ;;
  esac
  (cd "$tmp/$nombre" && ./gradlew --no-daemon --project-cache-dir "$tmp/$nombre/.pc" :verificador:check --console=plain) \
    >"$tmp/$nombre.log" 2>&1 || rc=$?
  echo "$rc"
}

rc1="$(correr s1)"; echo "escenario 1 (sin cambios): check rc=$rc1 (esperado 0)"
rc2="$(correr s2)"; echo "escenario 2 (S4 alterado): check rc=$rc2 (esperado != 0)"
rc3="$(correr s3)"; echo "escenario 3 (suite @Disabled): check rc=$rc3 (esperado != 0)"

[ "$rc1" -eq 0 ] || ok=0
[ "$rc2" -ne 0 ] || ok=0
[ "$rc3" -ne 0 ] || ok=0
# Causas: el caso S4 en el escenario 2; en el 3 el mensaje de verificarSuite (con la clase desactivada corren 8 pruebas, menos que el mínimo).
grep -q '"S4" FAILED' "$tmp/s2.log" || { echo "escenario 2: el log no nombra S4"; ok=0; }
grep -q 'verificarSuite:' "$tmp/s3.log" || { echo "escenario 3: el log no trae el mensaje de verificarSuite"; ok=0; }
echo "causa 2: $(grep -m1 '"S4" FAILED' "$tmp/s2.log" || true)"
echo "causa 3: $(grep -m1 'verificarSuite:' "$tmp/s3.log" || true)"
[ "$ok" -eq 1 ]
