package com.krasimirkolchev.inventory_service.service;

import com.krasimirkolchev.inventory_service.dto.InventoryResponse;
import com.krasimirkolchev.inventory_service.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> code) {
        return inventoryRepository.findByCodeIn(code).stream()
                .map(inventory ->
                    InventoryResponse.builder()
                            .code(inventory.getCode())
                            .isInStock(inventory.getQuantity() > 0)
                            .build()).toList();
    }
}
