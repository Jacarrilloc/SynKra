package io.synkra.plataforma;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.LockOptions;

/**
 * Dialecto de Spring Data JDBC para SQLite: paginacion {@code LIMIT n OFFSET m} y sin
 * bloqueo de filas (un solo escritor, RNF-07: SQLite serializa las escrituras).
 */
final class DialectoSqlite extends AbstractDialect {

    static final DialectoSqlite INSTANCE = new DialectoSqlite();

    private static final LimitClause LIMITE = new LimitClause() {
        @Override
        public String getLimit(long limit) {
            return "LIMIT " + limit;
        }

        @Override
        public String getOffset(long offset) {
            // SQLite exige LIMIT para usar OFFSET; -1 significa sin tope.
            return "LIMIT -1 OFFSET " + offset;
        }

        @Override
        public String getLimitOffset(long limit, long offset) {
            return "LIMIT " + limit + " OFFSET " + offset;
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    private static final LockClause BLOQUEO = new LockClause() {
        @Override
        public String getLock(LockOptions lockOptions) {
            return "";
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    private DialectoSqlite() {
    }

    @Override
    public LimitClause limit() {
        return LIMITE;
    }

    @Override
    public LockClause lock() {
        return BLOQUEO;
    }
}
