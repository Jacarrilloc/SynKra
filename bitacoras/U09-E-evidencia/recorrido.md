# Recorrido de la demo de SynKra por el front (U09-E)

Este documento se sigue en el navegador, paso por paso, sin haber estado en el proyecto. Todo ocurre en `https://synkra.local`, en el país `pe`. Al terminar cada paso se corre **una orden en la terminal** que guarda la evidencia de ese paso.

## Antes de empezar (una sola vez)

### 1. Que `synkra.local` apunte a tu máquina

El clúster `kind-synkra` publica el Ingress en `127.0.0.1:443`. Añade la línea al archivo de resolución (necesita `sudo`; el agente no lo hace):

```bash
echo '127.0.0.1 synkra.local' | sudo tee -a /etc/hosts
getent hosts synkra.local        # debe decir: 127.0.0.1  synkra.local
```

Al terminar todo el recorrido, quítala:

```bash
sudo sed -i '/^127\.0\.0\.1[[:space:]]\+synkra\.local$/d' /etc/hosts
getent hosts synkra.local        # no debe imprimir nada
```

(Alternativa sin tocar `/etc/hosts`: abrir Chromium con `--host-resolver-rules="MAP synkra.local 127.0.0.1"`.)

### 2. Que el navegador acepte el certificado

El certificado es autofirmado y está en `.tls/tls.crt` de este worktree (la clave `.tls/tls.key` no se toca ni se pega en ningún sitio). Elige una de las dos formas:

- **Importarlo (recomendado, evita avisos).** Chrome/Chromium: Configuración > Privacidad y seguridad > Seguridad > Administrar certificados > Autoridades > Importar, elige `.tls/tls.crt` y marca «Confiar en este certificado para identificar sitios web». Firefox: Ajustes > Privacidad y seguridad > Certificados > Ver certificados > Autoridades > Importar, y marca «Confiar en esta CA para identificar sitios web». Para quitarlo después, bórralo de la misma lista.
- **Aceptar la excepción.** Abre `https://synkra.local`, pulsa «Avanzado» y «Continuar a synkra.local (no seguro)».

Comprobación: `https://synkra.local/actuator/health` debe mostrar `{"status":"UP",...}` y la página `https://synkra.local/` debe mostrar «Vista general».

### 3. Dónde corres las órdenes

Todas las órdenes `bash bitacoras/U09-E-evidencia/paso.sh ...` se corren **desde la raíz de este worktree** (la carpeta que contiene `Makefile` y `.kube/`). `paso.sh` solo lee: no cambia el clúster ni la API; escribe un archivo `paso-<n>-<nombre>.txt` en `aidlc-docs/construction/U09-front/code/evidencia/recorrido/`.

### 4. Datos que necesitas

- **`VC`** es la versión del core: se lee de `core/build.gradle.kts` con `sed -n 's/^version = "\(.*\)"$/\1/p' core/build.gradle.kts` (también la imprime la línea `VC=` de `paso-00-inicio.txt`). No la escribas de memoria.
- Los números de despliegue **D1, D2 y D3** son los que la pantalla te muestre («Ver el despliegue #N»); no son fijos. Anótalos.
- Los usuarios son texto libre en el campo «Usuario»: **`ana`** prepara y **`luis`** confirma. **El campo solo se registra al salir de él** (pulsa Tab o Enter, o haz clic fuera) antes de pulsar el botón.
- Si en algún paso la pantalla no muestra lo que se espera, **detente ahí**, no sigas, y anota el paso y lo que viste.
- El front a veces pide dos veces una lectura al abrir una página (Blazor Server pinta primero en el servidor y luego abre el circuito interactivo). No es un fallo.

## Estado inicial (paso 0, ya hecho por el codificador)

El paso 0 (`inicio`) ya se corrió al preparar la demo. Al empezar, `pe` está vacío (lectura `Vacío`, sin despliegues) y `co` ya tiene su despliegue de la corrida de `scripts/e2e-api.sh`. El catálogo ya tiene `synkra-core` (con `VC` y `3.0.0`), `kyc-co 1.0.0`, `kyc-pe 1.3.0` y `kyc-pe 2.0.0`. Si `paso-00-inicio.txt` dice otra cosa, avisa antes de seguir.

Menú del front (arriba): **Vista general**, **Catálogo**, **Desplegar**.

## Los pasos

Después de cada paso, desde la raíz del worktree, corre la orden indicada. Debe imprimir `ok paso <n> <nombre> -> ...`; si imprime `FALLA`, anótalo y avisa.

### Paso 1: vista (`bash bitacoras/U09-E-evidencia/paso.sh 01 vista`)

- **Haz:** abre `https://synkra.local/` (Vista general).
- **Debes ver:** la tabla «Vista general» con un renglón por país: `co` con «Clúster» `Leído` y «Concuerda» `Sí` y su último despliegue; `pe` con `Vacío` (sin despliegue).
- **Log del front:** `GET /api/paises`.

### Paso 2: catalogo (`... paso.sh 02 catalogo`)

- **Haz:** pulsa «Catálogo». Solo se mira; no subas nada.
- **Debes ver:** el core `synkra-core` con las versiones `VC` y `3.0.0`, y sus plugins: `kyc-pe` `1.3.0` (core compatible `2.x`) y `kyc-pe` `2.0.0` (core compatible `3.x`), además de `kyc-co 1.0.0`.
- **Log del front:** `GET /api/cores` y un `GET /api/cores/<core>/plugins` por core.

### Paso 3: cu2-verifica (`... paso.sh 03 cu2-verifica`)

- **Haz:** pulsa «Desplegar». País `pe`, Core `synkra-core`, «Versión del core» `VC`. En «Plugins», escribe `pais=pe` en el filtro y pulsa «Filtrar». Marca la casilla `kyc-pe 1.3.0`.
- **Debes ver:** en «Verificación», el veredicto `APROBADA` (la fila `kyc-pe 1.3.0` con «Aceptado» `Sí`), y el botón «Preparar plan» habilitado.
- **Log del front:** `GET /api/cores/synkra-core/plugins?pais=pe` (puede salir redactada como `?*`) y `POST /api/verificaciones` (una por cada cambio de selección).

### Paso 4: cu2-plan (`... paso.sh 04 cu2-plan`)

- **Haz:** en «Usuario» escribe `ana` y sal del campo; pulsa «Preparar plan».
- **Debes ver:** el «Plan #N» con namespace `synkra-pe`, la imagen por digest (`...@sha256:...`), y `kyc-pe 1.3.0` en la lista de añadidos; «Creado por» `ana`.
- **Log del front:** `POST /api/despliegues` `201`.

### Paso 5: cu2-confirma (`... paso.sh 05 cu2-confirma`)

- **Haz:** cambia «Usuario» a `luis` y sal del campo; pulsa «Confirmar».
- **Debes ver:** el enlace «Ver el despliegue #D1». Anota D1.
- **Log del front:** `POST /api/despliegues/<D1>/confirmacion` `202`.

### Paso 6: cu2-listo (`... paso.sh 06 cu2-listo`)

- **Haz:** pulsa «Ver el despliegue #D1» (Detalle) y pulsa «Actualizar» cada pocos segundos hasta que el estado sea `Listo` (puede tardar un par de minutos). Después abre «Vista general».
- **Debes ver:** en el detalle, estado `Listo`, creado por `ana`, confirmado por `luis`, el plugin `kyc-pe 1.3.0`. En «Vista general», `pe` con `Leído`, «Concuerda» `Sí` y último despliegue `#D1`.
- **Log del front:** `GET /api/despliegues/<D1>` (una o varias veces) y `GET /api/paises/pe/despliegues`; en la vista, `GET /api/paises`.

### Paso 7: cu4-rechazo (`... paso.sh 07 cu4-rechazo`)

- **Haz:** recarga «Desplegar» (F5 en `https://synkra.local/desplegar`). País `pe`, core `synkra-core`, versión `VC`, filtro `pais=pe` y «Filtrar». Marca `kyc-pe 2.0.0` (y deja sin marcar `1.3.0`).
- **Debes ver:** veredicto `RECHAZADA`, el motivo `VERSION_INCOMPATIBLE: ...` y las alternativas; los botones «Preparar plan» y «Confirmar» **deshabilitados**. No pulses nada más: nada debe llegar al clúster.
- **Log del front:** `POST /api/verificaciones` `200` y **ningún** `POST /api/despliegues`.

### Paso 8: cu5-fallo (`... paso.sh 08 cu5-fallo`)

- **Haz:** recarga «Desplegar». País `pe`, core `synkra-core`, versión **`3.0.0`**, filtro `pais=pe`, «Filtrar», marca `kyc-pe 2.0.0`. Usuario `ana`, «Preparar plan»; usuario `luis`, «Confirmar».
- **Debes ver:** `APROBADA` (el catálogo declara `3.0.0`), el plan, y «Ver el despliegue #D2». Anota D2.
- **Log del front:** `POST /api/verificaciones`, `POST /api/despliegues` `201`, `POST /api/despliegues/<D2>/confirmacion` `202`.

### Paso 9: cu5-fallido (`... paso.sh 09 cu5-fallido`)

- **Haz:** abre el detalle de D2 y pulsa «Actualizar» hasta que el estado sea `Fallido` (el core arranca mal con el plugin 2.0.0; tarda unos minutos). Después abre «Vista general».
- **Debes ver:** estado `Fallido` y una sección de contexto cuyo `logCore` nombra `kyc-pe`. En «Vista general», `pe` con «Concuerda» **`No`** y último despliegue `#D1`.
- **Log del front:** `GET /api/despliegues/<D2>` y `GET /api/paises/pe/despliegues`; en la vista, `GET /api/paises`.

### Paso 10: cu5-vuelta (`... paso.sh 10 cu5-vuelta`)

- **Haz:** abre `Vista general`, pulsa `pe` (el historial del país) y, en el renglón de D1, pulsa «Volver a este». Usuario `ana`, «Preparar la vuelta»; usuario `luis`, «Confirmar la vuelta».
- **Debes ver:** el plan con tipo `Reversión` y origen `#D1`; luego «Ver el despliegue #D3». Anota D3.
- **Log del front:** `GET /api/despliegues/<D1>`, `POST /api/despliegues/<D1>/reversion` `201`, `POST /api/despliegues/<D3>/confirmacion` `202`.

### Paso 11: cu5-listo (`... paso.sh 11 cu5-listo`)

- **Haz:** abre el detalle de D3 y pulsa «Actualizar» hasta `Listo`. Vuelve a «Vista general».
- **Debes ver:** D3 `Listo`. En «Vista general», `pe` con «Concuerda» `Sí` y último despliegue `#D3`. El historial de `pe` termina así (el más nuevo primero): D3 `Reversión` `Listo`, D2 `Despliegue` `Fallido`, D1 `Despliegue` `Listo`.
- **Log del front:** `GET /api/despliegues/<D3>`, `GET /api/paises/pe/despliegues` y `GET /api/paises`.

## Al terminar

1. Quita la línea de `/etc/hosts` (arriba) y, si importaste el certificado, bórralo del navegador.
2. Entrega: lo que viste en cada paso (en una o dos líneas), los números D1, D2 y D3, cómo resolviste `synkra.local` y cómo aceptaste el certificado. No pegues la clave.
