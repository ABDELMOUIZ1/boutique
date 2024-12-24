package com.microservice.inventoryservice.controller;

import com.microservice.inventoryservice.dto.InventoryResponse;
import com.microservice.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/check-stock")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStockWithQuantityCheck(
            @RequestParam List<String> skuCode,
            @RequestParam List<Integer> quantities) {
        return inventoryService.isInStockWithQuantityCheck(skuCode, quantities);
    }

    @PostMapping("/update-stock")
    @ResponseStatus(HttpStatus.OK)
    public void updateStockAfterOrder(
            @RequestParam List<String> skuCode,
            @RequestParam List<Integer> quantities) {
        inventoryService.updateStockAfterOrder(skuCode, quantities);
    }


}
