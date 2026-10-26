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

    @Transactional
    public User createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email существует");
        }
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        if (user.getRating() == null) {
            user.setRating(0.0);
        }
        // ДОБАВИТЬ ВОТ ЭТУ СТРОКУ:
        if (user.getIsBlocked() == null) {
            user.setIsBlocked(false);
        }

        return userRepository.save(user);
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

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 1. Расчет уровня
        double rating = user.getRating() != null ? user.getRating() : 0.0;
        String level;
        if (rating < 10) level = "Junior";
        else if (rating < 50) level = "Middle";
        else if (rating < 100) level = "Senior";
        else level = "Legend";

        // 2. Расчет места в общем рейтинге
        long higherRatedCount = userRepository.countByRatingGreaterThan(rating);
        int currentPlace = (int) higherRatedCount + 1;
        long totalUsers = userRepository.count();

        // 3. Информация о подразделении (ИСПРАВЛЕНО)
        Long departmentId = null;
        String departmentName = "Без подразделения";
        Integer departmentPlace = null;

        if (user.getDepartment() != null) {
            departmentId = user.getDepartment().getId();
            departmentName = user.getDepartment().getName();

            try {
                departmentPlace = seasonService.getDepartmentPlace(departmentId);
            } catch (Exception e) {
                // Если сезон еще не начался, просто оставляем null
            }
        }

        // 4. История баланса (последние 3)
        List<CoinTransactionDTO> recentBalance = coinTxRepository
                .findHistoryByUser(userId, PageRequest.of(0, 3))
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(), t.getCreatedAt()
                )).getContent();

        // 5. История рейтинга (последние 3)
        List<RatingTransactionDTO> recentRating = ratingTxRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 3))
                .map(rt -> new RatingTransactionDTO(
                        rt.getId(), rt.getAmount(), rt.getReason(), rt.getCreatedAt()
                )).getContent();

        // 6. Последние заказы (5)
        List<UserProfileResponseDTO.OrderShortDTO> recentOrders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .map(o -> new UserProfileResponseDTO.OrderShortDTO(
                        o.getId(), o.getProduct().getName(), o.getQuantity(),
                        o.getTotalPrice(), o.getStatus(), o.getAdminComment()
                )).getContent();

        // 7. Статистика по событиям
        long organizedEvents = eventRepository.countByOrganizerId(userId);
        long participatedEvents = participantRepository.countByUserId(userId);

        // 8. Сгруппированные ачивки
        List<AchievementProfileDTO> achievementsProfile = buildGroupedAchievements(userId);

        // 9. Сборка итогового DTO
        return new UserProfileResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getRating(),
                level,
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

    // Вспомогательный метод для группировки ачивок
    // Вспомогательный метод для группировки ачивок
    private List<AchievementProfileDTO> buildGroupedAchievements(Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAll();

        Map<Long, UserAchievement> earnedMap = userAchievementRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        // Группируем по groupCode, а если его нет — по code (чтобы одиночные ачивки тоже показывались)
        Map<String, List<Achievement>> grouped = allAchievements.stream()
                .collect(Collectors.groupingBy(a -> a.getGroupCode() != null ? a.getGroupCode() : a.getCode()));

        List<AchievementProfileDTO> result = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            String groupCode = entry.getKey();
            List<Achievement> levels = entry.getValue();

            // Сортируем уровни (если level null, считаем 1)
            levels.sort(Comparator.comparing(a -> a.getLevel() != null ? a.getLevel() : 1));

            List<AchievementProfileDTO.LevelDetail> levelDetails = new ArrayList<>();
            boolean anyEarned = false;

            for (Achievement lvl : levels) {
                UserAchievement earned = earnedMap.get(lvl.getId());
                boolean isEarned = earned != null;
                if (isEarned) anyEarned = true;

                levelDetails.add(new AchievementProfileDTO.LevelDetail(
                        lvl.getLevel() != null ? lvl.getLevel() : 1,
                        lvl.getName(),
                        isEarned,
                        isEarned ? earned.getEarnedAt() : null
                ));
            }

            Achievement first = levels.get(0);
            result.add(new AchievementProfileDTO(
                    groupCode,
                    first.getName().split(" I")[0],
                    first.getDescription(),
                    first.getIconUrl(),
                    anyEarned,
                    levelDetails
            ));
        }
        return result;
    }

    // Обновление данных (Админ или HR)
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (!actor.getRole().has(Permission.USER_MANAGE)) {
            throw new SecurityException("Нет прав на редактирование пользователей");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) user.setEmail(request.email());
        if (request.password() != null) user.setPassword(request.password());

        return userRepository.save(user);
    }

    // Смена роли (Только ADMIN)
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

    // Блокировка/Разблокировка (Только ADMIN)
    @Transactional
    public User toggleBlockUser(Long userId, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Только ADMIN может блокировать пользователей");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setIsBlocked(!user.getIsBlocked());
        return userRepository.save(user);
    }
}