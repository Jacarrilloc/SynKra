#!/usr/bin/env bash
# CA-1 adaptado: la union ya esta en main (05a709e), por aclaracion del orquestador. Desde la raiz del worktree.
A=9272914; B=d6ca8b9; BASE=05a709e; COMUN=8306ee7
M=$(git rev-list --merges -1 "$BASE"); git log -1 --format='%H %P' "$M"
echo "# padres: el primero lleva la punta de U08-A (via su PR), el segundo es la punta de U08-B"
git rev-parse "$B"; git merge-base --is-ancestor "$A" "$BASE"; echo "a=$?"; git merge-base --is-ancestor "$B" "$BASE"; echo "b=$?"
git rev-parse "$(git log -1 --format=%P "$M" | cut -d' ' -f2)" | grep -q "^$(git rev-parse "$B")$" && echo "segundo-padre-es-B"
git log -1 --format='%H %P' 2eb1fe7
git merge-base --is-ancestor "$A" 2eb1fe7; echo "A-en-2eb1fe7=$?"
echo "# merges entre la punta de A y la base (la tarea esperaba 1; main trae ademas U09-A):"
git rev-list --merges "$A..$BASE" | xargs -n1 git log -1 --format='%h %s'
echo "# merge-limpio adaptado: los archivos de cada punta (frente a su base comun $COMUN) son identicos en $BASE"
for t in "$A" "$B"; do
  git diff --quiet "$t" "$BASE" -- $(git diff --name-only "$COMUN" "$t") && echo "archivos-de-$t-identicos-en-$BASE ($(git diff --name-only "$COMUN" "$t" | wc -l) archivos)"
done
echo "# la rama m6r/U08-C sale de $BASE y no tiene merges propios:"
git rev-list --merges "$BASE"..HEAD | wc -l
git log --oneline --graph "$A"..HEAD | head -40
