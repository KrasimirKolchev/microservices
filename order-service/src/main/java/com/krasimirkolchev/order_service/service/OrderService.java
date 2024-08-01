package com.krasimirkolchev.order_service.service;

import com.krasimirkolchev.order_service.dto.InventoryResponse;
import com.krasimirkolchev.order_service.dto.OrderLineItemsDto;
import com.krasimirkolchev.order_service.dto.OrderRequest;
import com.krasimirkolchev.order_service.model.Order;
import com.krasimirkolchev.order_service.model.OrderLineItems;
import com.krasimirkolchev.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public OrderService(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> codesList = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getCode)
                .toList();

        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("code", codesList).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean isInventoryInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        if (Boolean.TRUE.equals(isInventoryInStock)) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock. please try again later.");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setCode(orderLineItemsDto.getCode());
        return orderLineItems;
    }
}
