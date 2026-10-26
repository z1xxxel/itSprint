package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.Category;
import com.sprint.tms_backend.model.Product;
import com.sprint.tms_backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(shopService.getProductsByCategory(categoryId));
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
    public ResponseEntity<OrderResponseDTO> purchase(@RequestBody PurchaseRequest request) {
        return new ResponseEntity<>(shopService.purchaseProduct(request), HttpStatus.CREATED);
    }

    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(shopService.getUserOrders(userId));
    }

    // === Управление заказами (Менеджер/Админ) ===
    @GetMapping("/orders/pending")
    public ResponseEntity<List<OrderResponseDTO>> getPendingOrders(@RequestParam Long actorId) {
        return ResponseEntity.ok(shopService.getPendingOrders(actorId));
    }

    @PutMapping("/orders/{orderId}/approve")
    public ResponseEntity<OrderResponseDTO> approveOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId,
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(shopService.approveOrder(actorId, orderId, comment));
    }

    @PutMapping("/orders/{orderId}/reject")
    public ResponseEntity<OrderResponseDTO> rejectOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId,
            @RequestParam String reason) {
        return ResponseEntity.ok(shopService.rejectOrder(actorId, orderId, reason));
    }

    @PutMapping("/orders/{orderId}/complete")
    public ResponseEntity<OrderResponseDTO> completeOrder(
            @PathVariable Long orderId,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(shopService.completeOrder(actorId, orderId));
    }

    // Поиск
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponseDTO>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(shopService.searchProducts(query, categoryId, minPrice, maxPrice, sortBy, sortDir));
    }

    // Избранное
    @PostMapping("/favorites/{productId}")
    public ResponseEntity<String> addFavorite(@PathVariable Long productId, @RequestParam Long userId) {
        shopService.addFavorite(userId, productId);
        return ResponseEntity.ok("Добавлено в избранное");
    }

    @DeleteMapping("/favorites/{productId}")
    public ResponseEntity<String> removeFavorite(@PathVariable Long productId, @RequestParam Long userId) {
        shopService.removeFavorite(userId, productId);
        return ResponseEntity.ok("Удалено из избранного");
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<ProductResponseDTO>> getFavorites(@RequestParam Long userId) {
        return ResponseEntity.ok(shopService.getFavorites(userId));
    }

    // Отзывы
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<String> addReview(@PathVariable Long productId, @RequestBody ReviewRequest request) {
        shopService.addReview(request.userId(), productId, request.rating(), request.comment());
        return ResponseEntity.ok("Отзыв добавлен");
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(shopService.getReviews(productId));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(shopService.updateProduct(id, request, actorId));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        shopService.deleteProduct(id, actorId);
        return ResponseEntity.ok("Товар скрыт из каталога");
    }
}