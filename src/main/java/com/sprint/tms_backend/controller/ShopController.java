package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.Category;
import com.sprint.tms_backend.model.Order;
import com.sprint.tms_backend.model.Product;
import com.sprint.tms_backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    // === Категории ===
    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(
            @RequestParam Long actorId,
            @RequestBody CreateCategoryRequest request) {
        return new ResponseEntity<>(shopService.createCategory(actorId, request), HttpStatus.CREATED);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(shopService.getAllCategories());
    }

    // === Товары ===
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(
            @RequestParam Long actorId,
            @RequestBody CreateProductRequest request) {
        Product product = shopService.createProduct(actorId, request);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        return ResponseEntity.ok(shopService.getAllProducts());
    }

    // === Покупка и история ===
    @PostMapping("/purchase")
    public ResponseEntity<Order> purchase(@RequestBody PurchaseRequest request) {
        Order order = shopService.purchaseProduct(request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(shopService.getUserOrders(userId));
    }

    // === Управление заказами (Менеджер/Админ) ===
    @GetMapping("/orders/pending")
    public ResponseEntity<List<Order>> getPendingOrders(@RequestParam Long actorId) {
        return ResponseEntity.ok(shopService.getPendingOrders(actorId));
    }

    @PutMapping("/orders/{orderId}/approve")
    public ResponseEntity<Order> approveOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(shopService.approveOrder(actorId, orderId, comment));
    }

    @PutMapping("/orders/{orderId}/reject")
    public ResponseEntity<Order> rejectOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId,
            @RequestParam String reason) {
        return ResponseEntity.ok(shopService.rejectOrder(actorId, orderId, reason));
    }

    @PutMapping("/orders/{orderId}/complete")
    public ResponseEntity<Order> completeOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(shopService.completeOrder(actorId, orderId));
    }
}