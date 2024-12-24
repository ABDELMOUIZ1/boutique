package com.microService.orderservice.service;

import com.microService.orderservice.dto.InventoryResponse;
import com.microService.orderservice.dto.OrderLineItemsDto;
import com.microService.orderservice.dto.OrderRequest;
import com.microService.orderservice.event.OrderPlacedEvent;
import com.microService.orderservice.model.Order;
import com.microService.orderservice.model.OrderLineItems;
import com.microService.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;


    public String placeOrder(OrderRequest orderRequest) {
        System.out.println(orderRequest);
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        List<Integer> quantities = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getQuantity)
                .toList();

        // Vérification des stocks et mise à jour
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("localhost")
                        .port(59914)
                        .path("/api/inventory/check-stock")
                        .queryParam("skuCode", skuCodes)
                        .queryParam("quantities", quantities)
                        .build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::isInStock);

        if (!allProductsInStock) {
            throw new IllegalArgumentException("Product is not in stock or insufficient quantity, please try again later");
        }

        // Mettre à jour les stocks après validation
        webClientBuilder.build().post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("localhost")
                        .port(63648)
                        .path("/api/inventory/update-stock")
                        .queryParam("skuCode", skuCodes)
                        .queryParam("quantities", quantities)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        // Sauvegarde de la commande
        orderRepository.save(order);
        kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
        return "Order Placed Successfully";
    }




    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}