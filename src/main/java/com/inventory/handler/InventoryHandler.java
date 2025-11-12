package com.inventory.handler;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.inventory.service.InventoryService;

import inventory.InventoryServiceGrpc;
import inventory.Inventory;
@Component
public class InventoryHandler extends InventoryServiceGrpc.InventoryServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(InventoryHandler.class);
    private final InventoryService service;

    public InventoryHandler(InventoryService service) {
        this.service = service;
    }

    @Override
    public void checkAvailability(Inventory.CheckAvailabilityRequest request,
                                  StreamObserver<Inventory.CheckAvailabilityResponse> responseObserver) {
        service.sendLog("Received availability check for itemsCount: {}", request.getItemsCount());

        boolean available = service.checkAvailability(request.getItemsList());

        Inventory.CheckAvailabilityResponse resp = Inventory.CheckAvailabilityResponse.newBuilder()
                .setAvailable(available)
                .build();

        service.sendLog("Availability result: {}", available);
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveItems(Inventory.ReserveItemsRequest request,
                             StreamObserver<Inventory.ReserveItemsResponse> responseObserver) {

        service.sendLog(
            "Reserving itemsCount= {} orderId= {} customerId= {}",
            request.getItemsCount(), 
            request.getOrderId(), 
            request.getCustomerId()
        );

        try {
            request.getItemsList().forEach(item -> service.reserveItems(
                    request.getOrderId(),
                    request.getCustomerId(),
                    item.getProductId(),
                    item.getQuantity()
            ));

            service.sendLog("Items reserved: orderId: {}", request.getOrderId());
            Inventory.ReserveItemsResponse resp = Inventory.ReserveItemsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage(String.format("Items reserved for orderId=%s", request.getOrderId()))
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            String msgErr = "Failed to reserve items for orderId= " + request.getOrderId();
            logger.error(msgErr, ex);
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
        service.sendLog("Releasing reservation for orderId: {}", orderId);
        boolean released = service.releaseReservation(orderId);
        Inventory.ReleaseReservationResponse response = Inventory.ReleaseReservationResponse.newBuilder()
                .setSuccess(released)
                .setMessage(released ? "Reservation released" : "No reservation found")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
