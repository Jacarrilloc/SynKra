r() { echo "\$ $*"; eval "$@"; }
r 'bash scripts/imagenes.sh; echo "imagenes=$?"'
r 'for i in synkra-backend:0.1.0 synkra-core-ejemplo:2.1.0 synkra-init-descarga:1.0.0; do docker image inspect -f "{{index .RepoTags 0}} user={{.Config.User}} arch={{.Architecture}}" localhost:5001/$i; done'
r "docker version -f '{{.Server.Arch}}'"
r 'bash scripts/verificar-imagenes.sh; echo "verificar=$?"'
r "grep -n '^FROM' backend/Dockerfile core/Dockerfile init-descarga/Dockerfile"
r "grep -c '^FROM' init-descarga/Dockerfile backend/Dockerfile core/Dockerfile"
r "grep -n 'shellcheck' init-descarga/Dockerfile"
r "grep -nE '0\.1\.0|2\.1\.0|1\.0\.0' scripts/imagenes.sh scripts/verificar-imagenes.sh; echo \"literales=\$?\""
