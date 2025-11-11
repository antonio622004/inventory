package com.inventory.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitPublisher implements IsPublisher {

  private final RabbitTemplate rabbitTemplate;

  public RabbitPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publish(String message) {
        String logEntry = "[IS] " + message;
        rabbitTemplate.convertAndSend("log.queue", logEntry);
    }
}