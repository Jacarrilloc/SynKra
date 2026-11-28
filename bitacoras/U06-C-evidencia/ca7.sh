echo "# fecha: $(date -u +%FT%TZ)  (tomada justo antes del commit que la agrega; el sha es el de su padre)"
PS4='$ '; set -x
git rev-parse --short HEAD; git diff --stat e0f5c62...HEAD
git diff --name-status e0f5c62...HEAD -- backend/src/test
git diff e0f5c62...HEAD -- backend/src/test/java/io/synkra/FronterasModulosTest.java | grep -c '^-[^-]'
