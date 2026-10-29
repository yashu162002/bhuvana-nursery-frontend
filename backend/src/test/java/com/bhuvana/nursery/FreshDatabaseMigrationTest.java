package com.bhuvana.nursery;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations in an empty, throw-away schema so a fresh
 * PostgreSQL/Neon database is proven to be reproducible. The schema is dropped
 * afterwards; the application's own schema and data are never touched.
 */
@SpringBootTest
@ActiveProfiles("test")
class FreshDatabaseMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void allMigrationsApplyToAnEmptySchema() throws Exception {
        String schema = "migration_check_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        execute("CREATE SCHEMA " + schema);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .load();

            MigrateResult result = flyway.migrate();

            assertThat(result.migrationsExecuted)
                    .as("V1 through V6 must all run on a fresh database")
                    .isEqualTo(6);

            assertThat(tableExists(schema, "plant_categories")).isTrue();
            assertThat(tableExists(schema, "plants")).isTrue();
            assertThat(tableExists(schema, "hero_slides")).isTrue();
            assertThat(tableExists(schema, "admin_users")).isTrue();
            assertThat(tableExists(schema, "password_reset_tokens")).isTrue();
            assertThat(tableExists(schema, "flyway_schema_history")).isTrue();
        } finally {
            execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private boolean tableExists(String schema, String table) throws Exception {
        String sql = """
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
