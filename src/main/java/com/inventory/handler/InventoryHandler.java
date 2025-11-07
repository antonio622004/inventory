package com.inventory.handler;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventory.db.Database;
import com.inventory.repo.InventoryRepository;
import com.inventory.service.InventoryService;

import inventory.InventoryServiceGrpc;
import inventory.Inventory;

public class InventoryHandler extends InventoryServiceGrpc.InventoryServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(InventoryHandler.class);
    private final InventoryService service;

    public InventoryHandler(Database db) {
        this.service = new InventoryService(new InventoryRepository(db.getConnection()));
    }

    @Override
    public void checkAvailability(Inventory.CheckAvailabilityRequest request,
                                  StreamObserver<Inventory.CheckAvailabilityResponse> responseObserver) {
        logger.info("Received availability check for {} items", request.getItemsCount());

        boolean available = service.checkAvailability(request.getItemsList());

        Inventory.CheckAvailabilityResponse resp = Inventory.CheckAvailabilityResponse.newBuilder()
                .setAvailable(available)
                .build();

        logger.info("Availability result: {}", available);
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveItems(Inventory.ReserveItemsRequest request,
                             StreamObserver<Inventory.ReserveItemsResponse> responseObserver) {

        logger.info("Reserving {} items for orderId={}, customerId={}",
                request.getItemsCount(),
                request.getOrderId(),
                request.getCustomerId());

        try {
            request.getItemsList().forEach(item -> service.reserveItems(
                    request.getOrderId(),
                    request.getCustomerId(),
                    item.getProductId(),
                    item.getQuantity()
            ));

            logger.info("Items reserved: orderId={}", request.getOrderId());
            Inventory.ReserveItemsResponse resp = Inventory.ReserveItemsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage(String.format("Items reserved for orderId=%s", request.getOrderId()))
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            logger.error("Failed to reserve items for orderId={}", request.getOrderId(), ex);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to reserve items")
                    .withCause(ex)
                    .asRuntimeException());
        }
    }

    @Override
    public void releaseReservation(Inventory.ReleaseReservationRequest request,
                                   StreamObserver<Inventory.ReleaseReservationResponse> responseObserver) {
        String orderId = request.getOrderId();
        logger.info("Releasing reservation for orderId={}", orderId);
        boolean released = service.releaseReservation(orderId);
        Inventory.ReleaseReservationResponse response = Inventory.ReleaseReservationResponse.newBuilder()
                .setSuccess(released)
                .setMessage(released ? "Reservation released" : "No reservation found")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
