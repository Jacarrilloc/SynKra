# U01-T03 — respuesta a la ronda 1

F-01: Corregido en el commit de la ronda 2: `git rm --cached verificador/.jqwik-database` (antes `git ls-files` lo listaba, ahora 0 líneas). No toqué `.gitignore`: queda en la candidata C-02.
F-02: Corregido en el commit de la ronda 2: `diezEsMayorQueNueve` ahora compara `Version.parsear("2.10.0")` con `Version.parsear("2.9.0")`.
F-03: Corregido en el commit de la ronda 2: `parsear(null)` dice «Falta la versión: ...» sin el literal 'null'; prueba nueva `nuloTieneMensajePropioSinLiteralNull` (rojo: 1 de 16 fallaba; verde: 16 de 16).
