package com.inventory.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Repository;
import com.inventory.publisher.IsPublisher;
@Repository
public class InventoryRepository {

    private static final Logger logger = Logger.getLogger(InventoryRepository.class.getName());
    private final Connection connection;
    private final IsPublisher publisher;

    public InventoryRepository(Connection connection, IsPublisher publisher) {
        this.connection = connection;
        this.publisher = publisher;
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
            publisher.publishLog("Inserted reservation: orderId= {}, productId= {}, quantity= {}", orderId, productId, quantity);
        
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert reservation: orderId= " + orderId + ", productId= " + productId, e);
        }

        String updateSql = "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?";
        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
            updateStmt.setInt(1, quantity);
            updateStmt.setString(2, productId);
            updateStmt.executeUpdate();
            publisher.publishLog("Updated inventory: productId= {}, decremented by ", productId, quantity);
        
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update inventory: productId= " + productId, e);
        }
    }

    public boolean releaseReservation(String orderId) {
        boolean auto = true;
        try {
            auto = connection.getAutoCommit();
            connection.setAutoCommit(false);

            var items = loadReservedItems(orderId);
            if (items.isEmpty()){
                logger.log(Level.WARNING, "No reserved items found for orderId= " + orderId);
                return false;
            }
            

            updateInventory(items);
            deleteReservation(orderId);

            connection.commit();
            return true;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { connection.setAutoCommit(auto); } catch (SQLException ignored) {}
        }
    }

    private List<Map.Entry<String, Integer>> loadReservedItems(String orderId) throws SQLException {
        var items = new ArrayList<Map.Entry<String, Integer>>();
        String sql = "SELECT product_id, quantity FROM reserved_inventory WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new AbstractMap.SimpleEntry<>(
                        rs.getString("product_id"),
                        rs.getInt("quantity")
                    ));
                }
            }
        }
        return items;
    }


    private void updateInventory(List<Map.Entry<String, Integer>> items) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ? WHERE product_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (var item : items) {
                stmt.setInt(1, item.getValue());
                stmt.setString(2, item.getKey());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }


    private void deleteReservation(String orderId) throws SQLException {
        String sql = "DELETE FROM reserved_inventory WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            stmt.executeUpdate();
        }
    }
}
  
  
 