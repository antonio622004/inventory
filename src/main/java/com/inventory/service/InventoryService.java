package com.inventory.service;

import inventory.Inventory.Item;
import java.util.List;

import org.springframework.stereotype.Service;

import com.inventory.publisher.IsPublisher;
import com.inventory.repo.InventoryRepository;
@Service
public class InventoryService {
    private final InventoryRepository repository;
    private final IsPublisher publisher;

    public InventoryService(InventoryRepository repository, IsPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
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

    public void sendLog(String pattern, Object... args) {
        publisher.publishLog(pattern, args);
    }
}
