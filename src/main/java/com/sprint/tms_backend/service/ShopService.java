package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    // === УПРАВЛЕНИЕ ТОВАРАМИ И ПОИСК ===
    // ==========================================

    @Transactional
    public Product createProduct(Long actorId, CreateProductRequest request) {
        checkCatalogManageRights(actorId);

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
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setType(ProductType.PHYSICAL);

        // Устанавливаем скидку, если передана
        if (Boolean.TRUE.equals(request.isSpecialOffer())) {
            product.setIsSpecialOffer(true);
            product.setDiscountPercent(request.discountPercent() != null ? request.discountPercent() : 0);
        } else {
            product.setIsSpecialOffer(false);
            product.setDiscountPercent(0);
        }

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Категория не найдена");
        }
        return productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                .map(this::mapToDto).toList();
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

        // ИСПРАВЛЕНИЕ: заменяем null на пустую строку, чтобы PostgreSQL не ругался на bytea
        String safeQuery = (query == null || query.trim().isEmpty()) ? "" : query.trim();

        return productRepository.searchProducts(
                        safeQuery, categoryId, minPrice, maxPrice,
                        Sort.by(direction, sortField))
                .stream().map(this::mapToDto).toList();
    }

    // ==========================================
    // === ИЗБРАННОЕ ===
    // ==========================================

    @Transactional
    public void addFavorite(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new RuntimeException("Товар уже в избранном");
        }

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
        return favoriteRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(fav -> mapToDto(fav.getProduct()))
                .toList();
    }

    // ==========================================
    // === ОТЗЫВЫ ===
    // ==========================================

    @Transactional
    public void addReview(Long userId, Long productId, Integer rating, String comment) {
        if (rating < 1 || rating > 5) throw new RuntimeException("Оценка должна быть от 1 до 5");

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Товар не найден"));

        // Проверка: куплен ли товар (статус COMPLETED)
        boolean bought = orderRepository.existsByUserIdAndProductIdAndStatus(userId, productId, OrderStatus.COMPLETED);
        if (!bought) {
            throw new RuntimeException("Оставлять отзывы могут только те, кто купил и получил этот товар");
        }

        if (reviewRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new RuntimeException("Вы уже оставили отзыв на этот товар");
        }

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
    // === ПОКУПКА И ИСТОРИЯ ЗАКАЗОВ ===
    // ==========================================

    @Transactional
    public OrderResponseDTO purchaseProduct(PurchaseRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new RuntimeException("Количество должно быть больше нуля");
        }

        // Блокируем строку товара
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (product.getStock() < request.quantity()) {
            throw new RuntimeException("Недостаточно товара на складе. В наличии: " + product.getStock());
        }

        BigDecimal finalPrice = calculateFinalPrice(product);
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

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderDto).toList();
    }

    // ==========================================
    // === УПРАВЛЕНИЕ ЗАКАЗАМИ (МЕНЕДЖЕР) ===
    // ==========================================

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getPendingOrders(Long managerId) {
        checkManagerRights(managerId);
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING).stream()
                .map(this::mapToOrderDto).toList();
    }

    @Transactional
    public OrderResponseDTO approveOrder(Long managerId, Long orderId, String comment) {
        checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) throw new RuntimeException("Можно одобрить только заказы в статусе PENDING");

        order.setStatus(OrderStatus.APPROVED);
        if (comment != null && !comment.isEmpty()) order.setAdminComment(comment);
        return mapToOrderDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDTO rejectOrder(Long managerId, Long orderId, String reason) {
        checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.PENDING) throw new RuntimeException("Можно отклонить только заказы в статусе PENDING");

        // Возврат на склад
        Product product = productRepository.findById(order.getProduct().getId()).orElseThrow();
        product.setStock(product.getStock() + order.getQuantity());
        productRepository.save(product);

        // Возврат монет
        User user = userRepository.findById(order.getUser().getId()).orElseThrow();
        user.setBalance(user.getBalance().add(order.getTotalPrice()));
        userRepository.save(user);

        // Лог транзакции
        CoinTransaction refundTx = new CoinTransaction();
        refundTx.setFromUserId(null);
        refundTx.setToUserId(user.getId());
        refundTx.setAmount(order.getTotalPrice());
        refundTx.setType(CoinTransactionType.REFUND);
        refundTx.setComment("Возврат за заказ #" + order.getId() + ". Причина: " + reason);
        coinTransactionRepository.save(refundTx);

        order.setStatus(OrderStatus.REJECTED);
        order.setAdminComment(reason);
        return mapToOrderDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDTO completeOrder(Long managerId, Long orderId) {
        checkManagerRights(managerId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));

        if (order.getStatus() != OrderStatus.APPROVED) throw new RuntimeException("Завершить можно только одобренные заказы");

        order.setStatus(OrderStatus.COMPLETED);
        return mapToOrderDto(orderRepository.save(order));
    }

    // ==========================================
    // === ХЕЛПЕРЫ И МАППИНГ ===
    // ==========================================

    private void checkCatalogManageRights(Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.CATALOG_MANAGE)) throw new SecurityException("Нет прав на управление каталогом");
    }

    private void checkManagerRights(Long managerId) {
        User manager = userRepository.findById(managerId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!manager.getRole().has(Permission.ORDER_MANAGE)) throw new SecurityException("Только менеджер или админ может управлять заказами");
    }

    // Считает цену с учетом скидки
    private BigDecimal calculateFinalPrice(Product p) {
        if (Boolean.TRUE.equals(p.getIsSpecialOffer()) && p.getDiscountPercent() != null && p.getDiscountPercent() > 0) {
            BigDecimal discount = p.getPrice()
                    .multiply(BigDecimal.valueOf(p.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return p.getPrice().subtract(discount);
        }
        return p.getPrice();
    }

    private ProductResponseDTO mapToDto(Product p) {
        Double avgRating = reviewRepository.getAverageRating(p.getId());
        Integer reviewCount = reviewRepository.countByProductId(p.getId());

        return new ProductResponseDTO(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), calculateFinalPrice(p), p.getIsSpecialOffer(),
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

    // Редактирование товара (Менеджер/Админ)
    @Transactional
    public Product updateProduct(Long productId, CreateProductRequest request, Long actorId) {
        checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (request.name() != null) product.setName(request.name());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stock() != null) product.setStock(request.stock());
        if (request.description() != null) product.setDescription(request.description());

        return productRepository.save(product);
    }

    // Скрытие товара (Менеджер/Админ)
    @Transactional
    public void deleteProduct(Long productId, Long actorId) {
        checkCatalogManageRights(actorId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        product.setIsActive(false); // Мягкое удаление
        productRepository.save(product);
    }
}