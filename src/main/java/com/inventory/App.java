package com.inventory;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventory.db.Database;
import com.inventory.handler.InventoryHandler;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        logger.info("Logger test BEFORE gRPC start");

        Database db = new Database();

        int port = 50051;
        Server server = ServerBuilder.forPort(port)
                .addService(new InventoryHandler(db))
                .build()
                .start();

        logger.info("gRPC server started, listening on {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Shutting down gRPC server...");
            server.shutdown();
            try { db.close(); } catch (Exception e) { e.printStackTrace(); }
        }));

        server.awaitTermination();
    }
}
