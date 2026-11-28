#!/usr/bin/env bash
# CA-2: el script esta escrito como piden las reglas (sin cluster). Desde la raiz del worktree.
bash -n scripts/e2e-api.sh; echo "sintaxis=$?"
grep -n 'set -euo pipefail' scripts/e2e-api.sh
grep -c -- '--resolve synkra.local:443:127.0.0.1' scripts/e2e-api.sh
grep -cF -- '--cacert' scripts/e2e-api.sh
grep -nE -- '(^|[[:space:]])-k([[:space:]]|$)|--insecure|/etc/hosts|sudo|port-forward|kind get clusters|\.kube/config' scripts/e2e-api.sh; echo "prohibidos=$?"
grep -nE '(^|[^-])kubectl ' scripts/e2e-api.sh | grep -vF -e '--kubeconfig' | grep -v '^[0-9]*:[[:space:]]*#'; echo "sin-kubeconfig=$?"
grep -nE 'amd64|arm64|x86_64|aarch64' scripts/e2e-api.sh; echo "arquitectura=$?"
grep -nF 'has("codigo") and has("mensaje") and has("detalles")' scripts/e2e-api.sh
grep -n -e 'trap' -e 'mktemp' scripts/e2e-api.sh | head -3
echo "== la regla sin-kubeconfig por orden (sin_kc de U08-C ca9.sh, con el archivo de esta tarea)"
# Copiada de bitacoras/U08-C-evidencia/ca9.sh: solo cambia la lista de archivos. Sale con 0 si ENCUENTRA un kubectl sin --kubeconfig.
sin_kc() {
  (cd "${1:-.}" && python3 -I - scripts/e2e-api.sh <<'PY'
import re, sys
mal = 0
for ruta in sys.argv[1:]:
    for n, linea in enumerate(open(ruta, encoding="utf-8"), 1):
        l = linea.rstrip("\n")
        if l.lstrip().startswith("#"):
            continue
        l = re.sub(r'\becho\s+("([^"\\]|\\.)*"|\'[^\']*\')', "echo", l)
        l = re.sub(r"\s#.*$", "", l)
        for orden in re.split(r"\$\(|&&|\|\||[;|{(`]", l):
            o = re.sub(r"^\s*((if|then|else|elif|do|while|until|time|!)\s+)*", "", orden)
            if re.match(r"kubectl(\s|$)", o) and "--kubeconfig" not in o:
                print("%s:%d: %s" % (ruta, n, linea.strip()))
                mal += 1
sys.exit(0 if mal else 1)
PY
  )
}
sin_kc; echo "sin-kubeconfig-orden=$?"
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
mut() { # nombre expresion-sed: la regla tiene que detectar la mutacion
  local d="$D/mut-$1"; mkdir "$d"; git archive HEAD | tar -x -C "$d"
  cp "$d/scripts/e2e-api.sh" "$d/orig"; sed -i "$2" "$d/scripts/e2e-api.sh"
  if cmp -s "$d/scripts/e2e-api.sh" "$d/orig"; then echo "FALLA mutacion $1: el sed no cambio nada"; return; fi
  echo "== $1"; diff "$d/orig" "$d/scripts/e2e-api.sh" | sed -n '2p;4p'
  case "$1" in
    k) grep -nE -- '(^|[[:space:]])-k([[:space:]]|$)|--insecure' "$d/scripts/e2e-api.sh" >/dev/null; echo "prohibidos-mutado-$1=$?" ;;
    *) sin_kc "$d"; echo "sin-kubeconfig-mutado-$1=$?" ;;
  esac
}
mut envoltorio 's|^kc() { kubectl --kubeconfig "\$KC" |kc() { kubectl |'
mut suelto 's|^NS=synkra-co$|NS=synkra-co; kubectl get ns >/dev/null|'
mut mensaje 's|falla preparacion "falta \$h"|falla preparacion "falta --kubeconfig \$h"; kubectl get ns|'
mut k 's|curl -sS --max-time 60 --resolve|curl -k -sS --max-time 60 --resolve|'
