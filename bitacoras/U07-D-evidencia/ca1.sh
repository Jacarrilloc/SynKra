./gradlew :backend:test --rerun --tests '*ConfirmarTest' 2>&1 | grep -E "BUILD|tests completed|FAILED"
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ConfirmarTest.xml
python3 -I - backend/build/sqlite-pruebas/ConfirmarTest/synkra.db <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
q = lambda s: print(c.execute(s).fetchall())
q("select p.pais, p.estado, r.estado, r.creado_por, r.confirmado_por, json_extract(r.veredicto, '$.global') from dep_plan p"
  " left join reg_despliegue r on r.id = p.id where p.pais != 'ec' order by p.pais, p.creado_en")
q("select p.estado, r.estado from dep_plan p left join reg_despliegue r on r.id = p.id where p.pais = 'ec' order by p.estado")
q("select id, pais, estado from reg_despliegue where id not in (select id from dep_plan) order by id")
q("select x.log_core, x.log_init, x.eventos from reg_contexto x join dep_plan p on p.id = x.despliegue_id where p.pais = 'bo'")
q("select (select count(*) from reg_despliegue r join dep_plan p on p.id = r.id where p.estado != 'confirmado'),"
  " (select count(*) from dep_plan where estado = 'confirmado' and id not in (select id from reg_despliegue)),"
  " (select count(*) from reg_despliegue r join dep_plan p on p.id = r.id where r.confirmado_en < p.creado_en or r.confirmado_en >= p.vence_en)")
q("select r.confirmado_en - p.creado_en, r.imagen = json_extract(p.resumen, '$.imagen') from dep_plan p join reg_despliegue r on r.id = p.id where p.pais = 'pe'")
EOF
