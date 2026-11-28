r() { echo "\$ $*"; eval "$@"; }
I=localhost:5001/synkra-init-descarga:1.0.0
r 'docker run --rm --entrypoint id "$I"'
r "docker run --rm --entrypoint sh \"\$I\" -c 'command -v apk; echo \"apk=\$?\"; command -v curl sha256sum'"
r "docker run --rm --entrypoint sh \"\$I\" -c 'ls -l /usr/local/bin/descargar.sh'"
r '[ "$(docker run --rm --entrypoint cat "$I" /usr/local/bin/descargar.sh | sha256sum)" = "$(sha256sum < init-descarga/descargar.sh)" ] && echo "mismo-script"'
r "docker run --rm --entrypoint sh \"\$I\" -c 'command -v sha256sum'"
r 'sh init-descarga/test.sh; echo "test=$?"'
