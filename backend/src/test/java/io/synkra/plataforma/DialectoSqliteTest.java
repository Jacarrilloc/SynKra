package io.synkra.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

/** El SQL que genera el dialecto del bean debe ejecutarse en SQLite. */
class DialectoSqliteTest {

    private final Dialect dialecto = new BaseDeDatos().jdbcDialect();

    private String render(Select select) {
        RenderContext ctx = new org.springframework.data.relational.core.dialect.RenderContextFactory(dialecto).createRenderContext();
        return SqlRenderer.create(ctx).render(select);
    }

    private void ejecutar(String sql) throws Exception {
        try (var c = DriverManager.getConnection("jdbc:sqlite::memory:");
                var st = c.createStatement()) {
            st.execute("create table t (x integer)");
            st.executeQuery(sql).close();
        }
    }

    @Test
    void paginacionConLimitYOffsetSeEjecutaEnSqlite() throws Exception {
        var t = Table.create("t");
        Select s = Select.builder().select(t.column("x")).from(t).limitOffset(5, 10).build();
        String sql = render(s);
        assertThat(sql).containsIgnoringCase("LIMIT 5").containsIgnoringCase("OFFSET 10");
        ejecutar(sql);
    }

    @Test
    void bloqueoDeFilasNoEmiteNada() throws Exception {
        var t = Table.create("t");
        Select s = Select.builder().select(t.column("x")).from(t).lock(
                org.springframework.data.relational.core.sql.LockMode.PESSIMISTIC_WRITE).build();
        String sql = render(s);
        assertThat(sql).doesNotContainIgnoringCase("FOR UPDATE");
        ejecutar(sql);
    }
}
