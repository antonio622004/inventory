package com.inventory.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryRepository {

    private static final Logger logger = Logger.getLogger(InventoryRepository.class.getName());
    private final Connection connection;

    public InventoryRepository(Connection connection) {
        this.connection = connection;
    }

    public Integer getInventory(String productId) {
        String sql = "SELECT quantity FROM inventory WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("quantity");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error fetching inventory for product: " + productId, e);
        }
        return null;
    }

public void postReservation(String orderId, String customerId, String productId, int quantity) {
        String insertSql = "INSERT INTO reserved_inventory (order_id, customer_id, product_id, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, orderId);
            insertStmt.setString(2, customerId);
            insertStmt.setString(3, productId);
            insertStmt.setInt(4, quantity);
            insertStmt.executeUpdate();
            logger.info("Inserted reservation: orderId=" + orderId + ", productId=" + productId + ", quantity=" + quantity);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert reservation: orderId=" + orderId + ", productId=" + productId, e);
        }

        String updateSql = "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
            updateStmt.setInt(1, quantity);
            updateStmt.setString(2, productId);
            updateStmt.executeUpdate();
            logger.info("Updated inventory: productId=" + productId + ", decremented by " + quantity);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update inventory: productId=" + productId, e);
        }
    }
}
