package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.CreateCategoryRequest;
import com.sprint.tms_backend.DTO.CreateProductRequest;
import com.sprint.tms_backend.DTO.ProductResponseDTO;
import com.sprint.tms_backend.DTO.PurchaseRequest;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TransferService transferService;
    private final CategoryRepository categoryRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    // ==========================================
    // === УПРАВЛЕНИЕ КАТЕГОРИЯМИ ===
    // ==========================================

    @Transactional
    public Category createCategory(Long actorId, CreateCategoryRequest request) {
        checkCatalogManageRights(actorId);

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    // ==========================================
    // === УПРАВЛЕНИЕ ТОВАРАМИ ===
    // ==========================================

    @Transactional
    public Product createProduct(Long actorId, CreateProductRequest request) {
        checkCatalogManageRights(actorId);

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Цена должна быть больше нуля");
        }
        if (request.stock() == null || request.stock() < 0) {
            throw new RuntimeException("Количество не может быть отрицательным");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
        }

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setType(ProductType.PHYSICAL); // Или брать из запроса, если нужно

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Категория не найдена");
        }

        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // ==========================================
    // === ПОКУПКА И ИСТОРИЯ ЗАКАЗОВ ===
    // ==========================================

    @Transactional
    public Order purchaseProduct(PurchaseRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Количество должно быть больше нуля");
        }

        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (product.getStock() < request.quantity()) {
            throw new RuntimeException("Недостаточно товара на складе. В наличии: " + product.getStock());
        }

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        transferService.spendCoins(
                request.userId(),
                totalPrice,
                "Покупка: " + product.getName() + " (x" + request.quantity() + ")"
        );

        product.setStock(product.getStock() - request.quantity());
        productRepository.save(product);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING);

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ==========================================
    // === УПРАВЛЕНИЕ ЗАКАЗАМИ (МЕНЕДЖЕР) ===
    // ==========================================

    @Transactional(readOnly = true)
    public List<Order> getPendingOrders(Long managerId) {
        checkManagerRights(managerId);
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);
    }

    @Transactional
    public Order approveOrder(Long managerId, Long orderId, String comment) {
        checkManagerRights(managerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Можно одобрить только заказы в статусе PENDING");
        }

        order.setStatus(OrderStatus.APPROVED);
        if (comment != null && !comment.isEmpty()) {
            order.setAdminComment(comment);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order rejectOrder(Long managerId, Long orderId, String reason) {
        checkManagerRights(managerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Можно отклонить только заказы в статусе PENDING");
        }

        // 1. Возвращаем товар на склад
        Product product = productRepository.findByIdForUpdate(order.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        product.setStock(product.getStock() + order.getQuantity());
        productRepository.save(product);

        // 2. Возвращаем монеты
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setBalance(user.getBalance().add(order.getTotalPrice()));
        userRepository.save(user);

        // 3. Запись в журнал транзакций
        CoinTransaction refundTx = new CoinTransaction();
        refundTx.setFromUserId(null);
        refundTx.setToUserId(user.getId());
        refundTx.setAmount(order.getTotalPrice());
        refundTx.setType(CoinTransactionType.REFUND);
        refundTx.setComment("Возврат за заказ #" + order.getId() + ". Причина: " + reason);
        coinTransactionRepository.save(refundTx);

        // 4. Обновляем статус заказа
        order.setStatus(OrderStatus.REJECTED);
        order.setAdminComment(reason);

        return orderRepository.save(order);
    }

    @Transactional
    public Order completeOrder(Long managerId, Long orderId) {
        checkManagerRights(managerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Завершить можно только одобренные заказы");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return orderRepository.save(order);
    }

    // ==========================================
    // === ПРОВЕРКИ ПРАВ И ХЕЛПЕРЫ ===
    // ==========================================

    private void checkCatalogManageRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.CATALOG_MANAGE)) {
            throw new SecurityException("Нет прав на управление каталогом");
        }
    }

    private void checkManagerRights(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!manager.getRole().has(Permission.ORDER_MANAGE)) {
            throw new SecurityException("Только менеджер или админ может управлять заказами");
        }
    }

    private ProductResponseDTO mapToDto(Product p) {
        return new ProductResponseDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : "Без категории"
        );
    }
}