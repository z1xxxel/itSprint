package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final FavoriteProductRepository favoriteRepository;
    private final ProductReviewRepository reviewRepository;
    private final DiscountRepository discountRepository;
    private final ShopAuditLogRepository auditLogRepository;
    private final StockWriteOffRepository writeOffRepository;

    // ==========================================
    // === ЛОГИ ===
    // ==========================================
    @Transactional(readOnly = true)
    public Page<ShopAuditLogDTO> getAuditLogs(Long actorId, int page, int size) {
        checkCatalogManageRights(actorId);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(l -> new ShopAuditLogDTO(
                        l.getId(), l.getAction(),
                        l.getActorId(), l.getActorName(),
                        l.getProductId(), l.getProductName(),
                        l.getOrderId(), l.getDetails(), l.getCreatedAt()
                ));
    }

    private void logAction(User actor, String action, Product product, Long orderId, String details) {
        ShopAuditLog log = new ShopAuditLog();
        log.setAction(action);
        log.setActorId(actor.getId());
        log.setActorName(actor.getName());
        if (product != null) {
            log.setProductId(product.getId());
            log.setProductName(product.getName());
        }
        log.setOrderId(orderId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    // ==========================================
    // === КАТЕГОРИИ ===
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
    // === ТОВАРЫ ===
    // ==========================================
    @Transactional
    public Product createProduct(Long actorId, CreateProductRequest request) {
        User actor = checkCatalogManageRights(actorId);

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Базовая цена должна быть больше нуля");
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
        product.setPriceAdjustmentPercent(request.priceAdjustmentPercent() != null ? request.priceAdjustmentPercent() : 0);
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setType(ProductType.PHYSICAL);

        Product saved = productRepository.save(product);
        logAction(actor, "PRODUCT_CREATED", saved, null, "Создан товар: " + saved.getName());
        return saved;
    }

    @Transactional
    public Product updateProduct(Long productId, Long actorId, CreateProductRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (request.name() != null) product.setName(request.name());
        if (request.price() != null) product.setPrice(request.price());
        if (request.priceAdjustmentPercent() != null) product.setPriceAdjustmentPercent(request.priceAdjustmentPercent());
        if (request.stock() != null) product.setStock(request.stock());
        if (request.description() != null) product.setDescription(request.description());

        Product saved = productRepository.save(product);
        logAction(actor, "PRODUCT_UPDATED", saved, null, "Обновлен товар: " + saved.getName());
        return saved;
    }

    @Transactional
    public void deleteProduct(Long productId, Long actorId) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        product.setIsActive(false);
        productRepository.save(product);
        logAction(actor, "PRODUCT_DELETED", product, null, "Товар скрыт: " + product.getName());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> mapToDto(p, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Категория не найдена");
        }
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findByCategoryId(categoryId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> mapToDto(p, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String query, Long categoryId,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = switch (sortBy) {
            case "price" -> "price";
            case "name" -> "name";
            default -> "id";
        };
        String safeQuery = (query == null || query.trim().isEmpty()) ? "" : query.trim();
        LocalDateTime now = LocalDateTime.now();

        return productRepository.searchProducts(safeQuery, categoryId, minPrice, maxPrice,
                        Sort.by(direction, sortField)).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> mapToDto(p, now)).toList();
    }

    // ==========================================
    // === СКИДКИ (АКЦИИ) ===
    // ==========================================
    @Transactional
    public Discount createDiscount(Long productId, Long actorId, CreateDiscountRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (request.percent() == null || request.percent() < 1 || request.percent() > 99) {
            throw new RuntimeException("Скидка должна быть от 1 до 99%");
        }
        if (request.validFrom() == null || request.validTo() == null || request.validFrom().isAfter(request.validTo())) {
            throw new RuntimeException("Неверные даты скидки");
        }
        if (discountRepository.existsActiveDiscount(productId, LocalDateTime.now())) {
            throw new RuntimeException("У товара уже есть активная скидка");
        }

        Discount discount = new Discount();
        discount.setProduct(product);
        discount.setPercent(request.percent());
        discount.setValidFrom(request.validFrom());
        discount.setValidTo(request.validTo());
        discount.setIsActive(true);

        Discount saved = discountRepository.save(discount);
        logAction(actor, "DISCOUNT_CREATED", product, null, "Скидка " + request.percent() + "% до " + request.validTo());
        return saved;
    }

    // ==========================================
    // === СПИСАНИЕ СТОКА (WRITE-OFF) ===
    // ==========================================
    @Transactional
    public void writeOffStock(Long productId, Long actorId, WriteOffStockRequest request) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Кто может списывать: ADMIN, HR, MANAGER, ORGANIZER
        if (actor.getRole() != UserRole.ADMIN &&
                actor.getRole() != UserRole.HR &&
                actor.getRole() != UserRole.MANAGER &&
                actor.getRole() != UserRole.ORGANIZER) {
            throw new SecurityException("Нет прав на списание стока");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Количество должно быть больше нуля");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (product.getStock() < request.quantity()) {
            throw new RuntimeException("Недостаточно товара на складе. В наличии: " + product.getStock());
        }

        // Уменьшаем сток
        product.setStock(product.getStock() - request.quantity());
        productRepository.save(product);

        // Получаем имя получателя (если подарок конкретному человеку)
        String recipientName = null;
        if (request.recipientId() != null) {
            User recipient = userRepository.findById(request.recipientId()).orElse(null);
            if (recipient != null) recipientName = recipient.getName();
        }

        // Записываем в историю write-offs
        StockWriteOff writeOff = new StockWriteOff();
        writeOff.setProduct(product);
        writeOff.setQuantity(request.quantity());
        writeOff.setReason(request.reason() != null ? request.reason() : "OTHER");
        writeOff.setRecipientId(request.recipientId());
        writeOff.setRecipientName(recipientName);
        writeOff.setActorId(actor.getId());
        writeOff.setActorName(actor.getName());
        writeOff.setComment(request.comment());
        writeOffRepository.save(writeOff);

        // Логируем действие
        String details = "Списано " + request.quantity() + " шт. Причина: " + request.reason();
        if (recipientName != null) details += ". Получатель: " + recipientName;
        logAction(actor, "STOCK_WRITTEN_OFF", product, null, details);
    }

    // ==========================================
    // === ПОКУПКА ===
    // ==========================================
    @Transactional
    public OrderResponseDTO purchaseProduct(PurchaseRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Количество должно быть больше нуля");
        }

        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (product.getStock() < request.quantity()) {
            throw new RuntimeException("Недостаточно товара на складе. В наличии: " + product.getStock());
        }

        BigDecimal finalPrice = calculateFinalPrice(product, LocalDateTime.now());
        BigDecimal totalPrice = finalPrice.multiply(BigDecimal.valueOf(request.quantity()));

        transferService.spendCoins(
                request.userId(), totalPrice,
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

        return mapToOrderDto(orderRepository.save(order));
    }

    // ==========================================
    // === УПРАВЛЕНИЕ ЗАКАЗАМИ ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getPendingOrders(Long managerId) {
        checkManagerRights(managerId);
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING).stream()
                .map(this::mapToOrderDto).toList();
    }

    @Transactional
    public OrderResponseDTO approveOrder(Long managerId, Long orderId, String comment) {
        User manager = checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Можно одобрить только заказы в статусе PENDING");
        }

        order.setStatus(OrderStatus.APPROVED);
        if (comment != null && !comment.isEmpty()) order.setAdminComment(comment);
        Order saved = orderRepository.save(order);

        logAction(manager, "ORDER_APPROVED", order.getProduct(), orderId, "Заказ одобрен");
        return mapToOrderDto(saved);
    }

    @Transactional
    public OrderResponseDTO rejectOrder(Long managerId, Long orderId, String reason) {
        User manager = checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Можно отклонить только заказы в статусе PENDING");
        }

        // Возврат товара на склад
        Product product = productRepository.findById(order.getProduct().getId()).orElseThrow();
        product.setStock(product.getStock() + order.getQuantity());
        productRepository.save(product);

        // Возврат монет
        User user = userRepository.findById(order.getUser().getId()).orElseThrow();
        user.setBalance((user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO).add(order.getTotalPrice()));
        userRepository.save(user);

        // Журнал
        CoinTransaction refundTx = new CoinTransaction();
        refundTx.setFromUserId(null);
        refundTx.setToUserId(user.getId());
        refundTx.setAmount(order.getTotalPrice());
        refundTx.setType(CoinTransactionType.REFUND);
        refundTx.setComment("Возврат за заказ #" + order.getId() + ". Причина: " + reason);
        coinTransactionRepository.save(refundTx);

        order.setStatus(OrderStatus.REJECTED);
        order.setAdminComment(reason);
        Order saved = orderRepository.save(order);

        logAction(manager, "ORDER_REJECTED", order.getProduct(), orderId, "Заказ отклонен: " + reason);
        return mapToOrderDto(saved);
    }

    @Transactional
    public OrderResponseDTO completeOrder(Long managerId, Long orderId) {
        User manager = checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new RuntimeException("Завершить можно только одобренные заказы");
        }

        order.setStatus(OrderStatus.COMPLETED);
        Order saved = orderRepository.save(order);

        logAction(manager, "ORDER_COMPLETED", order.getProduct(), orderId, "Заказ выдан");
        return mapToOrderDto(saved);
    }

    // ==========================================
    // === ОТЗЫВЫ, ИЗБРАННОЕ ===
    // ==========================================
    @Transactional
    public void addFavorite(Long userId, Long productId) {
        if (favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new RuntimeException("Товар уже в избранном");
        }
        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        FavoriteProduct fav = new FavoriteProduct();
        fav.setUser(user);
        fav.setProduct(product);
        favoriteRepository.save(fav);
    }

    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteRepository.findByUserIdAndProductId(userId, productId).ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getFavorites(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return favoriteRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(f -> mapToDto(f.getProduct(), now)).toList();
    }

    @Transactional
    public void addReview(Long userId, Long productId, Integer rating, String comment) {
        if (rating < 1 || rating > 5) throw new RuntimeException("Оценка 1-5");
        boolean bought = orderRepository.existsByUserIdAndProductIdAndStatus(userId, productId, OrderStatus.COMPLETED);
        if (!bought) throw new RuntimeException("Только купившие могут оставить отзыв");
        if (reviewRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new RuntimeException("Вы уже оставили отзыв");
        }
        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(r -> new ReviewResponseDTO(
                        r.getId(), r.getUser().getName(), r.getRating(), r.getComment(), r.getCreatedAt()
                )).toList();
    }

    // ==========================================
    // === ХЕЛПЕРЫ И РАСЧЕТ ЦЕНЫ ===
    // ==========================================
    private User checkCatalogManageRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.CATALOG_MANAGE)) {
            throw new SecurityException("Нет прав на управление каталогом");
        }
        return actor;
    }

    private User checkManagerRights(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!manager.getRole().has(Permission.ORDER_MANAGE)) {
            throw new SecurityException("Только менеджер или админ может управлять заказами");
        }
        return manager;
    }

    // Расчет итоговой цены с учетом наценки и активной скидки
    private BigDecimal calculateFinalPrice(Product p, LocalDateTime now) {
        BigDecimal base = p.getPrice();

        // 1. Применяем наценку/уценку (постоянную)
        int adj = p.getPriceAdjustmentPercent() != null ? p.getPriceAdjustmentPercent() : 0;
        BigDecimal adjusted = base.multiply(BigDecimal.ONE.add(
                BigDecimal.valueOf(adj).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        ));

        // 2. Применяем активную скидку (если есть)
        Discount active = discountRepository.findActiveDiscount(p.getId(), now).orElse(null);
        BigDecimal finalPrice = adjusted;
        if (active != null) {
            finalPrice = adjusted.multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(active.getPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            ));
        }

        return finalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private ProductResponseDTO mapToDto(Product p, LocalDateTime now) {
        Double avgRating = reviewRepository.getAverageRating(p.getId());
        Integer reviewCount = reviewRepository.countByProductId(p.getId());

        // Считаем цены
        BigDecimal base = p.getPrice();
        int adj = p.getPriceAdjustmentPercent() != null ? p.getPriceAdjustmentPercent() : 0;
        BigDecimal adjusted = base.multiply(BigDecimal.ONE.add(
                BigDecimal.valueOf(adj).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        )).setScale(2, RoundingMode.HALF_UP);

        Discount active = discountRepository.findActiveDiscount(p.getId(), now).orElse(null);
        BigDecimal finalPrice = adjusted;
        Integer discountPercent = null;
        LocalDateTime validUntil = null;

        if (active != null) {
            discountPercent = active.getPercent();
            validUntil = active.getValidTo();
            finalPrice = adjusted.multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            )).setScale(2, RoundingMode.HALF_UP);
        }

        return new ProductResponseDTO(
                p.getId(), p.getName(), p.getDescription(),
                base, adj, adjusted,
                discountPercent, finalPrice, active != null, validUntil,
                p.getStock(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : "Без категории",
                avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0,
                reviewCount
        );
    }

    private OrderResponseDTO mapToOrderDto(Order o) {
        return new OrderResponseDTO(
                o.getId(), o.getUser().getId(), o.getUser().getName(),
                o.getProduct().getId(), o.getProduct().getName(),
                o.getQuantity(), o.getTotalPrice(), o.getStatus(), o.getAdminComment(), o.getCreatedAt()
        );
    }
}