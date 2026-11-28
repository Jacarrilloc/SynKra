./gradlew :backend:test --rerun --tests '*PlanPendienteTest' --tests '*RelojTest' 2>&1 | grep -E "BUILD|tests completed|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PlanPendienteTest.xml backend/build/test-results/test/TEST-*RelojTest.xml
python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, estado, creado_en from dep_plan order by pais, creado_en').fetchall())" backend/build/sqlite-pruebas/PlanPendienteTest/synkra.db
grep -rn "CONFIRMACION_NO_DISPONIBLE\|NOT_IMPLEMENTED" backend/src; echo "501=$?"
git diff ade68f6 HEAD -- backend/src/test/java/io/synkra/despliegue/PlanPendienteTest.java | grep -E '^[-+][^-+]'
