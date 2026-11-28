export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)"
PS4='$ '; set -x
./gradlew :backend:test --rerun --tests '*VistaGeneralTest' 2>&1 | grep -E "BUILD|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*VistaGeneralTest.xml
python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, pais, estado from reg_despliegue order by id').fetchall()); print(c.execute(\"select d.pais, d.id from reg_despliegue d where d.id = (select e.id from reg_despliegue e where e.pais = d.pais and e.estado = 'listo' order by e.confirmado_en desc, e.id desc limit 1) order by d.pais\").fetchall()); print(c.execute('select despliegue_id, group_concat(nombre) from (select * from reg_plugin order by despliegue_id, nombre) group by despliegue_id having count(*) > 1').fetchall())" backend/build/sqlite-pruebas/VistaGeneralTest/synkra.db
