package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.AchievementResponseDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository participantRepository;
    private final CoinTransactionRepository coinTxRepository; // <--- ДОБАВЛЕНО

    // === АВТОМАТИЧЕСКИЕ АЧИВКИ ===

    @Transactional
    public void checkEventAchievements(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        long count = participantRepository.countByUserId(userId);

        checkAndAward(user, "EVENT_JOINER_5", count >= 5);
        checkAndAward(user, "EVENT_JOINER_10", count >= 10);
        checkAndAward(user, "EVENT_JOINER_20", count >= 20);
    }

    private void checkAndAward(User user, String achievementCode, boolean condition) {
        if (!condition) return;

        Achievement achievement = achievementRepository.findByCode(achievementCode).orElse(null);
        if (achievement == null) return;

        if (!userAchievementRepository.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
            // 1. Записываем ачивку
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepository.save(ua);

            // 2. Выдаем монеты, если предусмотрено
            awardCoinsForAchievement(user, achievement);
        }
    }

    // === РУЧНЫЕ АЧИВКИ (от менеджера/админа) ===

    @Transactional
    public void grantManualAchievement(Long actorId, Long userId, String code) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (!actor.getRole().has(Permission.USER_MANAGE)) {
            throw new SecurityException("Нет прав на выдачу ачивок");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Achievement achievement = achievementRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Ачивка с таким кодом не найдена"));

        if (achievement.getType() != AchievementType.MANUAL) {
            throw new RuntimeException("Эту ачивку нельзя выдать вручную (она автоматическая)");
        }

        if (!userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepository.save(ua);

            // Выдаем монеты, если предусмотрено
            awardCoinsForAchievement(user, achievement);
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ НАЧИСЛЕНИЯ ===
    private void awardCoinsForAchievement(User user, Achievement achievement) {
        BigDecimal reward = achievement.getRewardCoins();
        if (reward != null && reward.compareTo(BigDecimal.ZERO) > 0) {
            // 1. Начисляем баланс
            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            user.setBalance(currentBalance.add(reward));
            userRepository.save(user);

            // 2. Пишем в журнал транзакций (эмиссия)
            CoinTransaction tx = new CoinTransaction();
            tx.setFromUserId(null);
            tx.setToUserId(user.getId());
            tx.setAmount(reward);
            tx.setType(CoinTransactionType.ACHIEVEMENT);
            tx.setComment("Награда за достижение: " + achievement.getName());
            coinTxRepository.save(tx);
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementResponseDTO> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> new AchievementResponseDTO(
                        ua.getAchievement().getId(),
                        ua.getAchievement().getCode(),
                        ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(),
                        ua.getAchievement().getIconUrl(),
                        ua.getAchievement().getRewardCoins(), // <--- ПЕРЕДАЕМ В DTO
                        ua.getEarnedAt()
                )).toList();
    }
}