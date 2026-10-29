package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final RatingTransactionRepository ratingTxRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final SeasonService seasonService;

    // ==========================================
    // === АВТОРИЗАЦИЯ ===
    // ==========================================
    @Transactional(readOnly = true)
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Неверный email или пароль"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Неверный email или пароль");
        }

        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new RuntimeException("Ваш аккаунт заблокирован. Обратитесь к администратору.");
        }

        return user;
    }

    // ==========================================
    // === СОЗДАНИЕ И УПРАВЛЕНИЕ ===
    // ==========================================
    @Transactional
    public User createUser(Long actorId, User newUser) {
        // Если actorId нет — это БУТСТРАП (создание самого первого пользователя)
        if (actorId == null) {
            if (userRepository.count() > 0) {
                throw new RuntimeException("Передан actorId=null, но база не пуста. Укажите создателя.");
            }
            // Разрешаем создать только ADMIN
            if (newUser.getRole() != UserRole.ADMIN) {
                throw new RuntimeException("Первый пользователь в системе должен быть ADMIN");
            }
        } else {
            // Обычная проверка прав
            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new RuntimeException("Создатель не найден"));

            if (actor.getRole() != UserRole.ADMIN &&
                    actor.getRole() != UserRole.HR &&
                    actor.getRole() != UserRole.MANAGER) {
                throw new SecurityException("Только HR, MANAGER или ADMIN могут создавать пользователей");
            }
        }

        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Пароль обязателен при создании");
        }

        if (newUser.getBalance() == null) newUser.setBalance(BigDecimal.ZERO);
        if (newUser.getRating() == null) newUser.setRating(0.0);
        if (newUser.getLifetimeRating() == null) newUser.setLifetimeRating(0.0);
        if (newUser.getIsBlocked() == null) newUser.setIsBlocked(false);
        if (newUser.getMonthlyEarnedCoins() == null) newUser.setMonthlyEarnedCoins(BigDecimal.ZERO);

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional
    public void changeUserPassword(Long userId, String newPassword, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Админ не найден"));

        if (actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Только ADMIN может сбрасывать пароли");
        }

        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new RuntimeException("Новый пароль слишком короткий (минимум 4 символа)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (!actor.getRole().has(Permission.USER_MANAGE)) {
            throw new SecurityException("Нет прав на редактирование");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) user.setEmail(request.email());

        return userRepository.save(user);
    }

    @Transactional
    public User changeUserRole(Long userId, UserRole newRole, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Только ADMIN может менять роли");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public User toggleBlockUser(Long userId, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Только ADMIN может блокировать");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setIsBlocked(!user.getIsBlocked());
        return userRepository.save(user);
    }

    // ==========================================
    // === ПРОФИЛЬ (С двумя рейтингами) ===
    // ==========================================
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 1. Уровень считаем от LIFETIME (общей репутации за всё время)
        double lifetimeRating = user.getLifetimeRating() != null ? user.getLifetimeRating() : 0.0;
        String level;
        if (lifetimeRating < 10) level = "Junior";
        else if (lifetimeRating < 50) level = "Middle";
        else if (lifetimeRating < 100) level = "Senior";
        else level = "Legend";

        // 2. Место считаем от ТЕКУЩЕГО рейтинга сезона
        double currentRating = user.getRating() != null ? user.getRating() : 0.0;
        long higherRatedCount = userRepository.countByRatingGreaterThan(currentRating);
        int currentPlace = (int) higherRatedCount + 1;
        long totalUsers = userRepository.count();

        // 3. Подразделение
        Long departmentId = null;
        String departmentName = "Без подразделения";
        Integer departmentPlace = null;

        if (user.getDepartment() != null) {
            departmentId = user.getDepartment().getId();
            departmentName = user.getDepartment().getName();

            try {
                departmentPlace = seasonService.getDepartmentPlace(departmentId);
            } catch (Exception e) {
                // Сезон еще не начался
            }
        }

        // 4. История баланса (3 шт)
        List<CoinTransactionDTO> recentBalance = coinTxRepository
                .findHistoryByUser(userId, PageRequest.of(0, 3))
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(),
                        t.getIsCancelled(), t.getCancellationReason(),
                        t.getCreatedAt()
                )).getContent();

        // 5. История рейтинга (3 шт)
        List<RatingTransactionDTO> recentRating = ratingTxRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 3))
                .map(rt -> new RatingTransactionDTO(
                        rt.getId(), rt.getAmount(), rt.getReason(), rt.getCreatedAt()
                )).getContent();

        // 6. Последние 5 заказов
        List<UserProfileResponseDTO.OrderShortDTO> recentOrders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .map(o -> new UserProfileResponseDTO.OrderShortDTO(
                        o.getId(), o.getProduct().getName(), o.getQuantity(),
                        o.getTotalPrice(), o.getStatus(), o.getAdminComment()
                )).getContent();

        // 7. Статистика событий
        long organizedEvents = eventRepository.countByOrganizerId(userId);
        long participatedEvents = participantRepository.countByUserId(userId);

        // 8. Сгруппированные ачивки
        List<AchievementProfileDTO> achievementsProfile = buildGroupedAchievements(userId);

        return new UserProfileResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getBalance(),
                currentRating,      // Текущий рейтинг сезона
                lifetimeRating,     // Общая репутация
                level,              // От lifetimeRating
                currentPlace,
                totalUsers,
                departmentId,
                departmentName,
                departmentPlace,
                organizedEvents,
                participatedEvents,
                recentRating,
                recentBalance,
                recentOrders,
                achievementsProfile
        );
    }

    // ==========================================
    // === ВСПОМОГАТЕЛЬНЫЙ МЕТОД (ачивки) ===
    // ==========================================
    private List<AchievementProfileDTO> buildGroupedAchievements(Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAll();

        Map<Long, UserAchievement> earnedMap = userAchievementRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        Map<String, List<Achievement>> grouped = allAchievements.stream()
                .filter(a -> a.getGroupCode() != null)
                .collect(Collectors.groupingBy(Achievement::getGroupCode));

        List<AchievementProfileDTO> result = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            String groupCode = entry.getKey();
            List<Achievement> levels = entry.getValue();
            levels.sort(Comparator.comparing(Achievement::getLevel));

            List<AchievementProfileDTO.LevelDetail> levelDetails = new ArrayList<>();
            boolean anyEarned = false;

            for (Achievement lvl : levels) {
                UserAchievement earned = earnedMap.get(lvl.getId());
                boolean isEarned = earned != null;
                if (isEarned) anyEarned = true;

                levelDetails.add(new AchievementProfileDTO.LevelDetail(
                        lvl.getLevel(),
                        lvl.getName(),
                        lvl.getDescription(),
                        lvl.getRequiredProgress(),
                        lvl.getRewardCoins() != null ? lvl.getRewardCoins() : BigDecimal.ZERO,
                        isEarned,
                        isEarned ? earned.getEarnedAt() : null
                ));
            }

            Achievement first = levels.get(0);
            // "Участник I" -> "Участник"
            String groupName = first.getName().split(" I")[0];

            result.add(new AchievementProfileDTO(
                    groupCode,
                    groupName,
                    first.getDescription(),
                    first.getIconUrl(),
                    anyEarned,
                    levelDetails
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return userRepository.searchUsers(query.trim());
    }

    @Transactional
    public User updateAvatar(Long userId, String avatarUrl, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Менять можно только себе, если ты не ADMIN
        if (!actor.getId().equals(userId) && actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Можно менять только свой аватар");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }
}