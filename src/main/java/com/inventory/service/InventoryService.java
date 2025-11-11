package com.inventory.service;

import inventory.Inventory.Item;
import java.util.List;

import com.inventory.publisher.IsPublisher;
import com.inventory.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository repository;
    private final IsPublisher publisher;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
        this.publisher = msg -> {};
    }

    public boolean checkAvailability(List<Item> items) {
        return items.stream().allMatch(item -> {
            String productId = item.getProductId();
            int quantity = item.getQuantity();
            if (productId == null || productId.isEmpty() || quantity <= 0) return false;
            Integer stock = repository.getInventory(productId);
            return stock != null && stock >= quantity;
        });
    }

    public void reserveItems(String orderId, String customerId, String productId, int quantity) {
        repository.postReservation(orderId, customerId, productId, quantity);
    }

    public boolean releaseReservation(String orderId) {
        return repository.releaseReservation(orderId);
    }

    public void publishLog(String message) {
    publisher.publish(message);
  }
}
