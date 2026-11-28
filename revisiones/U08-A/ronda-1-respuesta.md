# U08-A — respuesta a la ronda 1
F-01: Corregido en el commit de esta ronda (sin tocar código): `ca4.txt` pega `make -n install` con el falso; `ca2-tras-ca9.txt` y `ca3-tras-ca9.txt` corridos después de `ca9.txt` (00:32:34 y 00:32:36 contra 00:32:31, sha 16740a8); `ca6.txt` termina con `T=<redactado>`. Detalle en la bitácora, «Ronda 2».
F-02: Ratificado por el orquestador: el paso «2b» de la guardia (kubeconfig sin contextos aceptado solo con `--antes-de-crear`) queda como está.
F-03: Ratificado (por el orquestador): no se añade `containerdConfigPatches`; CA-3 demuestra que hosts.toml funciona en las dos arquitecturas (aarch64 y x86_64).
F-04: Aceptado sin cambios (decisión del orquestador).
Nota de especificación: CA-1 (`arm64`) y CA-7 (`true` para los ajenos) dependen de la máquina; aquí dan `amd64` y `false/false`. Es una diferencia de máquina, no se arregla.
C-135 / C-136: caché de `kubectl` en `$HOME/.kube/cache` y los esperados de entorno quedan como candidatas del orquestador; no se tocaron.
