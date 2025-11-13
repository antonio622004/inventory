package com.inventory;

import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

import com.inventory.handler.InventoryHandler;

@SpringBootApplication
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public Server grpcServer(InventoryHandler handler) throws Exception {
        int port = 50051;
        Server server = io.grpc.ServerBuilder.forPort(port)
                .addService(handler)
                .build()
                .start();

        logger.info("gRPC server started, listening on {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Shutting down gRPC server...");
            server.shutdown();
        }));

        return server;
    }

    @Bean
    public ApplicationRunner serverRunner(Server server) {
        return args -> {
            server.awaitTermination();
        };
    }
}
