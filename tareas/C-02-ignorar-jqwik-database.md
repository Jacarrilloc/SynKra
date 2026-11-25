# C-02: Ignorar la base de datos de jqwik

**Unidad:** U01: `Verificador y suite` (candidata C-02 de `CANDIDATAS.md`, aprobada por el responsable)
**Historias que implementa:** ninguna (mantenimiento del build)
**Depende de:** U01-T04

---

## Alcance

**Dentro** (una línea, concreta):

> Que la base de datos de semillas de jqwik (`.jqwik-database`) no se pueda versionar por accidente en ningún subproyecto, añadiendo `**/.jqwik-database` al `.gitignore` de la raíz.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Mover la base de jqwik a `build/` con `junit-platform.properties` u otra configuración de jqwik.
- Cualquier otro cambio en `.gitignore`, en archivos de Gradle o en código.
- Las demás candidatas (C-03 a C-08).

---

## Archivos de contexto

- `CANDIDATAS.md` (fila C-02)
- `revisiones/U01-T03/ronda-1.md` (F-01: el archivo llegó a versionarse)

---

## Criterios de aceptación

- [ ] **CA-1**: git ignora la base de jqwik en cualquier subproyecto
  ```bash
  git check-ignore -v verificador/.jqwik-database backend/.jqwik-database
  ```
  Esperado: dos líneas que nombran `.gitignore` y el patrón `**/.jqwik-database`.

- [ ] **CA-2**: lectura de vuelta tras correr jqwik: el archivo existe en disco y git no lo ve
  ```bash
  ./gradlew :verificador:test --rerun
  test -f verificador/.jqwik-database && echo existe
  git status --porcelain --untracked-files=all | grep -c jqwik
  ```
  Esperado: `BUILD SUCCESSFUL`, `existe` y `0`.

- [ ] **CA-3**: no está rastreado
  ```bash
  git ls-files | grep -c '\.jqwik-database$'
  ```
  Esperado: `0`.

---

## Plan de pruebas

Sin lógica: las pruebas son los tres criterios.

**Rojo primero:** antes de tocar `.gitignore`, corre CA-1 y la última línea de CA-2 y registra que fallan
(`check-ignore` sale con 1 y el conteo da 1 o más).

---

## Notas

- Se modifica en su sitio: `.gitignore`.
