package com.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.inventory.db.Database;
import java.sql.Connection;

@Configuration
public class DatabaseConfig {
    
    @Bean
    public Database database() {
        try {
            return new Database();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Database bean", e);
        }
    }

    @Bean
    public Connection connection(Database database) {
        try {
            return database.getConnection();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Connection from Database", e);
        }
    }
}

