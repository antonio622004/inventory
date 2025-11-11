package com.inventory.publisher;

@FunctionalInterface
public interface IsPublisher {
    void publish(String message);
}