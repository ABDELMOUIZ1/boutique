package com.microservice.inventoryservice.service;

import com.microservice.inventoryservice.dto.InventoryResponse;
import com.microservice.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStockWithQuantityCheck(List<String> skuCodes, List<Integer> quantities) {
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(inventory -> {
                    int index = skuCodes.indexOf(inventory.getSkuCode());
                    int requestedQuantity = quantities.get(index);

                    return InventoryResponse.builder()
                            .skuCode(inventory.getSkuCode())
                            .isInStock(inventory.getQuantity() >= requestedQuantity) // Vérifie la quantité
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateStockAfterOrder(List<String> skuCodes, List<Integer> quantities) {
        for (int i = 0; i < skuCodes.size(); i++) {
            String skuCode = skuCodes.get(i);
            int requestedQuantity = quantities.get(i);

            var inventory = inventoryRepository.findBySkuCode(skuCode)
                    .orElseThrow(() -> new IllegalArgumentException("Product with SKU " + skuCode + " not found"));

            if (inventory.getQuantity() < requestedQuantity) {
                throw new IllegalArgumentException("Insufficient quantity for SKU " + skuCode);
            }

            // Réduction de la quantité
            inventory.setQuantity(inventory.getQuantity() - requestedQuantity);
            inventoryRepository.save(inventory);
        }
    }


}