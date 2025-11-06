package com.inventory.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:inventory.db";
    private final Connection connection;

    public Database() throws Exception {
        this.connection = DriverManager.getConnection(DB_URL);
        setupSchema();
    }

    private void setupSchema() throws Exception {
        Path schemaPath = Path.of("src/main/resources/db/schema.sql");
        String sql = Files.readString(schemaPath);

        try (Statement stmt = connection.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null) connection.close();
    }
}
