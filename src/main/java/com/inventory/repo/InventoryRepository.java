package com.inventory.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Repository;
@Repository
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
            logger.info("Inserted reservation: orderId= " + orderId + ", productId= " + productId + ", quantity= " + quantity);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert reservation: orderId= " + orderId + ", productId= " + productId, e);
        }

    String updateSql = "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
            updateStmt.setInt(1, quantity);
            updateStmt.setString(2, productId);
            updateStmt.executeUpdate();
            logger.info("Updated inventory: productId=" + productId + ", decremented by " + quantity);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update inventory: productId= " + productId, e);
        }
    }

    public boolean releaseReservation(String orderId) {
        String selectSql = "SELECT product_id, quantity FROM reserved_inventory WHERE order_id = ?";
        java.util.List<java.util.Map.Entry<String, Integer>> items = new java.util.ArrayList<>();
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, orderId);
            ResultSet rs = selectStmt.executeQuery();
            while (rs.next()) {
                items.add(new java.util.AbstractMap.SimpleEntry<>(
                        rs.getString("product_id"),
                        rs.getInt("quantity")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load reservation for release: orderId=" + orderId, e);
            return false;
        }

        if (items.isEmpty()) {
            logger.info("No reservations found to release for orderId=" + orderId);
            return false;
        }

        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            String updateSql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                for (java.util.Map.Entry<String, Integer> item : items) {
                    updateStmt.setInt(1, item.getValue());
                    updateStmt.setString(2, item.getKey());
                    updateStmt.addBatch();
                }
                updateStmt.executeBatch();
            }

            String deleteSql = "DELETE FROM reserved_inventory WHERE order_id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, orderId);
                deleteStmt.executeUpdate();
            }

            connection.commit();
            logger.info("Released reservation for orderId=" + orderId);
            return true;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Rollback failed while releasing reservation: orderId=" + orderId, rollbackEx);
            }
            logger.log(Level.SEVERE, "Failed to release reservation: orderId=" + orderId, e);
            return false;
        } finally {
            try { connection.setAutoCommit(autoCommit); } catch (SQLException ignored) {}
        }
    }
}
