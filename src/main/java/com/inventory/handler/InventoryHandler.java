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
        String msgIn = "Received availability check for itemsCount= " + request.getItemsCount();
        logger.info(msgIn);
        service.publishLog(msgIn);

        boolean available = service.checkAvailability(request.getItemsList());

        Inventory.CheckAvailabilityResponse resp = Inventory.CheckAvailabilityResponse.newBuilder()
                .setAvailable(available)
                .build();

        String msgOut = "Availability result= " + available;
        logger.info(msgOut);
        service.publishLog(msgOut);
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveItems(Inventory.ReserveItemsRequest request,
                             StreamObserver<Inventory.ReserveItemsResponse> responseObserver) {

        String msgIn = "Reserving itemsCount= " + request.getItemsCount()
        + " orderId= " + request.getOrderId()
        + " customerId= " + request.getCustomerId();
        logger.info(msgIn);
        service.publishLog(msgIn);

        try {
            request.getItemsList().forEach(item -> service.reserveItems(
                    request.getOrderId(),
                    request.getCustomerId(),
                    item.getProductId(),
                    item.getQuantity()
            ));

            String msgOut = "Items reserved: orderId= " + request.getOrderId();
                logger.info(msgOut);
                service.publishLog(msgOut);
            Inventory.ReserveItemsResponse resp = Inventory.ReserveItemsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage(String.format("Items reserved for orderId=%s", request.getOrderId()))
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            String msgErr = "Failed to reserve items for orderId= " + request.getOrderId()
                + " error= " + ex.getMessage();
                logger.error(msgErr, ex);
                service.publishLog(msgErr);
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
        String msgOut = "Releasing reservation for orderId= " + orderId;
        logger.info(msgOut);
        service.publishLog(msgOut);
        boolean released = service.releaseReservation(orderId);
        Inventory.ReleaseReservationResponse response = Inventory.ReleaseReservationResponse.newBuilder()
                .setSuccess(released)
                .setMessage(released ? "Reservation released" : "No reservation found")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
