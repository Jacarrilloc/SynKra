package io.synkra.plataforma;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.IdGeneration;
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

    /** sqlite-jdbc no devuelve las claves generadas de un insert por lotes: saveAll inserta uno a uno. */
    @Override
    public IdGeneration getIdGeneration() {
        return new IdGeneration() {
            @Override
            public boolean supportedForBatchOperations() {
                return false;
            }
        };
    }

    /** SQLite no tiene tipo booleano: devuelve 0 o 1 como entero. */
    @ReadingConverter
    enum EnteroABooleano implements Converter<Integer, Boolean> {
        INSTANCE;

        @Override
        public Boolean convert(Integer origen) {
            return origen != 0;
        }
    }

    /**
     * sqlite-jdbc guarda las fechas como milisegundos desde la epoca (columna INTEGER), con la zona
     * horaria del sistema para las fechas sin zona; al leer vuelven como Long.
     */
    @ReadingConverter
    enum EnteroAInstant implements Converter<Long, Instant> {
        INSTANCE;

        @Override
        public Instant convert(Long origen) {
            return Instant.ofEpochMilli(origen);
        }
    }

    @ReadingConverter
    enum EnteroAFecha implements Converter<Long, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(Long origen) {
            return Instant.ofEpochMilli(origen).atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    @ReadingConverter
    enum EnteroAFechaHora implements Converter<Long, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(Long origen) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(origen), ZoneId.systemDefault());
        }
    }

    @Override
    public Collection<Object> getConverters() {
        return List.of(EnteroABooleano.INSTANCE, EnteroAInstant.INSTANCE, EnteroAFecha.INSTANCE,
                EnteroAFechaHora.INSTANCE);
    }
}
