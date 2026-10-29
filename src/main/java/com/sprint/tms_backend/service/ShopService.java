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
    private final ProductVariantRepository variantRepository;
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

    @Transactional(readOnly = true)
    public List<ProductMovementDTO> getProductMovements(Long productId, Long actorId) {
        checkCatalogManageRights(actorId);
        return auditLogRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(l -> new ProductMovementDTO(
                        l.getId(), l.getAction(), l.getActorName(),
                        l.getQuantityDelta(), l.getDetails(), l.getCreatedAt()
                ))
                .toList();
    }

    private void logAction(
            User actor,
            String action,
            Product product,
            Long orderId,
            String details,
            Integer quantityDelta
    ) {
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
        log.setQuantityDelta(quantityDelta);
        auditLogRepository.save(log);
    }

    // ==========================================
    // === КАТЕГОРИИ ===
    // ==========================================

    @Transactional
    public Category createCategory(Long actorId, CreateCategoryRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        Category saved = categoryRepository.save(category);
        logAction(actor, "CATEGORY_CREATED", null, null,
                "Создана категория: " + saved.getName(), null);
        return saved;
    }

    @Transactional
    public Category updateCategory(Long categoryId, Long actorId, CreateCategoryRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        if (request.name() != null && !request.name().trim().isEmpty()) {
            category.setName(request.name().trim());
        }
        if (request.description() != null) category.setDescription(request.description());
        if (request.imageUrl() != null) category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());

        Category saved = categoryRepository.save(category);
        logAction(actor, "CATEGORY_UPDATED", null, null,
                "Обновлена категория: " + saved.getName(), null);
        return saved;
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long actorId) {
        User actor = checkCatalogManageRights(actorId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        long productCount = productRepository.findByCategoryId(categoryId).size();
        if (productCount > 0) {
            throw new RuntimeException(
                    "Нельзя удалить категорию: в ней " + productCount
                            + " товаров. Переместите или скройте товары."
            );
        }
        categoryRepository.delete(category);
        logAction(actor, "CATEGORY_DELETED", null, null,
                "Удалена категория: " + category.getName(), null);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    // ==========================================
    // === ТОВАРЫ ===
    // ==========================================

    @Transactional
    public ProductResponseDTO createProduct(Long actorId, CreateProductRequest request) {
        User actor = checkCatalogManageRights(actorId);

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Цена должна быть больше нуля");
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
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setType(ProductType.PHYSICAL);

        boolean hasVariants = Boolean.TRUE.equals(request.hasVariants());
        product.setHasVariants(hasVariants);

        if (hasVariants) {
            product.setStock(0);
            Product saved = productRepository.save(product);

            int totalStock = 0;
            if (request.variants() != null) {
                for (CreateProductVariantRequest v : request.variants()) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(saved);
                    variant.setSize(v.size() != null ? v.size() : "");
                    variant.setColor(v.color() != null ? v.color() : "");
                    variant.setStock(v.stock() != null ? v.stock() : 0);
                    variantRepository.save(variant);
                    totalStock += variant.getStock();
                }
            }
            saved.setStock(totalStock);
            saved = productRepository.save(saved);
            logAction(actor, "PRODUCT_CREATED", saved, null,
                    "Создан товар: " + saved.getName() + ", начальный остаток: " + saved.getStock() + " шт.",
                    saved.getStock());
            return mapToDto(saved, LocalDateTime.now());
        } else {
            product.setStock(request.stock() != null ? request.stock() : 0);
            Product saved = productRepository.save(product);
            logAction(actor, "PRODUCT_CREATED", saved, null,
                    "Создан товар: " + saved.getName() + ", начальный остаток: " + saved.getStock() + " шт.",
                    saved.getStock());
            return mapToDto(saved, LocalDateTime.now());
        }
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long productId, Long actorId, CreateProductRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        Integer oldStock = product.getStock();

        if (request.name() != null) product.setName(request.name());
        if (request.price() != null) product.setPrice(request.price());
        if (request.description() != null) product.setDescription(request.description());
        if (!Boolean.TRUE.equals(product.getHasVariants()) && request.stock() != null) {
            product.setStock(request.stock());
        }
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.categoryId() != null) {
            Category cat = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Категория не найдена"));
            product.setCategory(cat);
        }

        Product saved = productRepository.save(product);

        Integer delta = null;
        if (!Boolean.TRUE.equals(saved.getHasVariants()) && oldStock != null) {
            delta = saved.getStock() - oldStock;
            if (delta == 0) delta = null;
        }

        logAction(actor, "PRODUCT_UPDATED", saved, null,
                "Обновлен товар: " + saved.getName(), delta);
        return mapToDto(saved, LocalDateTime.now());
    }

    @Transactional
    public void deleteProduct(Long productId, Long actorId) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        product.setIsActive(false);
        productRepository.save(product);
        logAction(actor, "PRODUCT_DELETED", product, null,
                "Товар скрыт: " + product.getName(), null);
    }

    @Transactional
    public void restockProduct(Long productId, Long actorId, Integer amount, Long variantId) {
        User actor = checkCatalogManageRights(actorId);
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Количество должно быть положительным");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (Boolean.TRUE.equals(product.getHasVariants())) {
            if (variantId == null) {
                throw new RuntimeException("Выберите вариант для пополнения");
            }
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Вариант не найден"));
            if (!variant.getProduct().getId().equals(productId)) {
                throw new RuntimeException("Вариант не относится к этому товару");
            }
            variant.setStock(variant.getStock() + amount);
            variantRepository.save(variant);
            product.setStock(product.getStock() + amount);
        } else {
            product.setStock(product.getStock() + amount);
        }
        productRepository.save(product);
        logAction(actor, "PRODUCT_RESTOCKED", product, null,
                "Пополнение: +" + amount + " шт. Новый остаток: " + product.getStock() + " шт.",
                amount);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> mapToDto(p, now)).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        return mapToDto(p, LocalDateTime.now());
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
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
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
    // === СКИДКИ ===
    // ==========================================

    @Transactional
    public Discount createDiscount(Long productId, Long actorId, CreateDiscountRequest request) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (request.percent() == null || request.percent() < 1 || request.percent() > 99) {
            throw new RuntimeException("Скидка должна быть от 1 до 99%");
        }
        if (request.validFrom() == null || request.validTo() == null
                || request.validFrom().isAfter(request.validTo())) {
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
        logAction(actor, "DISCOUNT_CREATED", product, null,
                "Скидка " + request.percent() + "% до " + request.validTo(), null);
        return saved;
    }

    // ==========================================
    // === СПИСАНИЕ СТОКА ===
    // ==========================================

    @Transactional
    public void writeOffStock(Long productId, Long actorId, WriteOffStockRequest request) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

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

        product.setStock(product.getStock() - request.quantity());
        productRepository.save(product);

        String recipientName = null;
        if (request.recipientId() != null) {
            User recipient = userRepository.findById(request.recipientId()).orElse(null);
            if (recipient != null) recipientName = recipient.getName();
        }

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

        String details = "Списание: −" + request.quantity() + " шт. Причина: " + request.reason();
        if (recipientName != null) details += ". Получатель: " + recipientName;
        if (request.comment() != null && !request.comment().isEmpty()) {
            details += ". Комментарий: " + request.comment();
        }
        details += ". Новый остаток: " + product.getStock() + " шт.";
        logAction(actor, "STOCK_WRITTEN_OFF", product, null, details, -request.quantity());
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

        ProductVariant variant = null;

        if (Boolean.TRUE.equals(product.getHasVariants())) {
            if (request.variantId() == null) {
                throw new RuntimeException("Выберите размер и цвет");
            }
            variant = variantRepository.findById(request.variantId())
                    .orElseThrow(() -> new RuntimeException("Вариант не найден"));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new RuntimeException("Вариант не относится к этому товару");
            }
            if (variant.getStock() < request.quantity()) {
                throw new RuntimeException("Недостаточно товара на складе. В наличии: " + variant.getStock());
            }
            variant.setStock(variant.getStock() - request.quantity());
            variantRepository.save(variant);
        } else {
            if (product.getStock() < request.quantity()) {
                throw new RuntimeException("Недостаточно товара на складе. В наличии: " + product.getStock());
            }
        }

        product.setStock(product.getStock() - request.quantity());
        productRepository.save(product);

        BigDecimal finalPrice = calculateFinalPrice(product, LocalDateTime.now());
        BigDecimal totalPrice = finalPrice.multiply(BigDecimal.valueOf(request.quantity()));

        transferService.spendCoins(
                request.userId(), totalPrice,
                "Покупка: " + product.getName() + " (x" + request.quantity() + ")"
        );

        User buyer = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Order order = new Order();
        order.setUser(buyer);
        order.setProduct(product);
        order.setVariant(variant);
        order.setQuantity(request.quantity());
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        logAction(buyer, "PRODUCT_SOLD", product, savedOrder.getId(),
                "Покупка: " + request.quantity() + " шт. Заказ #" + savedOrder.getId()
                        + ". Новый остаток: " + product.getStock() + " шт.",
                -request.quantity());

        return mapToOrderDto(savedOrder);
    }

    @Transactional
    public List<OrderResponseDTO> purchaseBulk(List<PurchaseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }
        Long userId = requests.get(0).userId();
        for (PurchaseRequest r : requests) {
            if (!r.userId().equals(userId)) {
                throw new RuntimeException("Все товары должны быть от одного пользователя");
            }
        }
        List<OrderResponseDTO> result = new java.util.ArrayList<>();
        for (PurchaseRequest r : requests) {
            result.add(purchaseProduct(r));
        }
        return result;
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

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders(Long actorId) {
        checkManagerRights(actorId);
        return orderRepository.findTop100ByOrderByCreatedAtDesc().stream()
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
        logAction(manager, "ORDER_APPROVED", order.getProduct(), orderId,
                "Заказ #" + orderId + " одобрен", null);
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

        // Возврат остатка
        Product product = productRepository.findById(order.getProduct().getId()).orElseThrow();
        product.setStock(product.getStock() + order.getQuantity());
        productRepository.save(product);

        if (order.getVariant() != null) {
            ProductVariant variant = variantRepository.findById(order.getVariant().getId()).orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + order.getQuantity());
                variantRepository.save(variant);
            }
        }

        // Возврат монет
        User user = userRepository.findById(order.getUser().getId()).orElseThrow();
        user.setBalance((user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO)
                .add(order.getTotalPrice()));
        userRepository.save(user);

        // Журнал транзакций
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
        logAction(manager, "ORDER_REJECTED", order.getProduct(), orderId,
                "Заказ #" + orderId + " отклонен. Причина: " + reason
                        + ". Возврат: +" + order.getQuantity() + " шт.",
                order.getQuantity());
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
        logAction(manager, "ORDER_COMPLETED", order.getProduct(), orderId,
                "Заказ #" + orderId + " завершен (выдан)", null);
        return mapToOrderDto(saved);
    }

    // ==========================================
    // === ИЗБРАННОЕ ===
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
        favoriteRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getFavorites(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return favoriteRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(f -> mapToDto(f.getProduct(), now)).toList();
    }

    // ==========================================
    // === ОТЗЫВЫ ===
    // ==========================================

    @Transactional
    public void addReview(Long userId, Long productId, Integer rating, String comment) {
        if (rating < 1 || rating > 5) throw new RuntimeException("Оценка 1-5");
        boolean bought = orderRepository.existsByUserIdAndProductIdAndStatus(
                userId, productId, OrderStatus.COMPLETED);
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
                        r.getId(), r.getUser().getName(), r.getRating(),
                        r.getComment(), r.getCreatedAt()
                )).toList();
    }

    // ==========================================
    // === ХЕЛПЕРЫ ===
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

    private BigDecimal calculateFinalPrice(Product p, LocalDateTime now) {
        BigDecimal base = p.getPrice();
        Discount active = discountRepository.findActiveDiscount(p.getId(), now).orElse(null);
        BigDecimal finalPrice = base;
        if (active != null) {
            finalPrice = base.multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(active.getPercent())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            ));
        }
        return finalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void deleteDiscount(Long productId, Long actorId) {
        User actor = checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        Discount active = discountRepository.findActiveDiscount(productId, LocalDateTime.now())
                .orElse(null);
        if (active == null) {
            throw new RuntimeException("У товара нет активной скидки");
        }
        active.setIsActive(false);
        discountRepository.save(active);
        logAction(actor, "DISCOUNT_DELETED", product, null,
                "Удалена скидка " + active.getPercent() + "%", null);
    }

    private ProductResponseDTO mapToDto(Product p, LocalDateTime now) {
        Double avgRating = reviewRepository.getAverageRating(p.getId());
        Integer reviewCount = reviewRepository.countByProductId(p.getId());

        BigDecimal base = p.getPrice();
        Discount active = discountRepository.findActiveDiscount(p.getId(), now).orElse(null);
        BigDecimal finalPrice = base;
        Integer discountPercent = null;
        LocalDateTime validUntil = null;

        if (active != null) {
            discountPercent = active.getPercent();
            validUntil = active.getValidTo();
            finalPrice = base.multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(discountPercent)
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            )).setScale(2, RoundingMode.HALF_UP);
        }

        List<ProductVariantDTO> variants = p.getVariants() != null
                ? p.getVariants().stream()
                .map(v -> new ProductVariantDTO(v.getId(), v.getSize(), v.getColor(), v.getStock()))
                .toList()
                : List.of();

        return new ProductResponseDTO(
                p.getId(), p.getName(), p.getDescription(),
                base,
                base,                              // adjustedPrice = basePrice
                discountPercent, finalPrice, active != null, validUntil,
                p.getStock(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : "Без категории",
                avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0,
                reviewCount,
                Boolean.TRUE.equals(p.getHasVariants()),
                variants
        );
    }

    private OrderResponseDTO mapToOrderDto(Order o) {
        return new OrderResponseDTO(
                o.getId(), o.getUser().getId(), o.getUser().getName(),
                o.getProduct().getId(), o.getProduct().getName(),
                o.getVariant() != null ? o.getVariant().getId() : null,
                o.getVariant() != null ? o.getVariant().getSize() : null,
                o.getVariant() != null ? o.getVariant().getColor() : null,
                o.getQuantity(), o.getTotalPrice(), o.getStatus(),
                o.getAdminComment(), o.getCreatedAt()
        );
    }
}