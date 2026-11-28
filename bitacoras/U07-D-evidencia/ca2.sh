./gradlew :backend:test --rerun --tests '*CarpetaEquivocadaPlanTest' 2>&1 | grep -E "BUILD|tests completed|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CarpetaEquivocadaPlanTest.xml
python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, core, estado, plugins from dep_plan order by id').fetchall()); print(c.execute('select count(*) from reg_despliegue').fetchone())" backend/build/sqlite-pruebas/CarpetaEquivocadaPlanTest/synkra.db
