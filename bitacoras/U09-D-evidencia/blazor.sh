#!/usr/bin/env bash
# Hallazgo (restauracion): la imagen sirve _framework/blazor.web.js? Desde la raiz del worktree, con la imagen 0.1.0 construida.
I=localhost:5001/synkra-front:0.1.0; N=synkra-u09d-blazor-$$
trap 'docker rm -f "$N" >/dev/null 2>&1' EXIT
docker run --pull=never -d --name "$N" --user 10001:10001 --read-only --tmpfs /tmp -e Synkra__ApiUrl=http://127.0.0.1:9/ -p 127.0.0.1::8080 "$I" >/dev/null
P=$(docker port "$N" 8080/tcp | head -1 | cut -d: -f2)
timeout 60 bash -c "until curl -sf 127.0.0.1:$P/app.css >/dev/null; do sleep 1; done"
curl -s 127.0.0.1:$P/ | grep -o '<script[^>]*>'
curl -s -o /dev/null -w 'blazor.web.js %{http_code}\n' 127.0.0.1:$P/_framework/blazor.web.js
docker run --pull=never --rm --entrypoint cat "$I" /app/Synkra.Front.deps.json | python3 -I -c 'import json,sys; print(sorted(json.load(sys.stdin)["libraries"]))'
