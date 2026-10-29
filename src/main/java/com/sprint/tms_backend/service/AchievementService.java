package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.AchievementResponseDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository participantRepository;
    private final CoinTransactionRepository coinTxRepository;

    // === АВТОМАТИЧЕСКИЕ АЧИВКИ (Проверка при действиях пользователя) ===

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
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepository.save(ua);

            awardCoinsForAchievement(user, achievement);
        }
    }

    // === СИСТЕМНАЯ ВЫДАЧА АЧИВКИ (Вызывается планировщиком) ===
    @Transactional
    public void grantAchievementByCode(Long userId, String code) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден для выдачи ачивки {}", userId, code);
            return;
        }

        Achievement achievement = achievementRepository.findByCode(code).orElse(null);
        if (achievement == null) {
            log.warn("Ачивка с кодом {} не найдена в базе данных", code);
            return;
        }

        if (!userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
            UserAchievement ua = new UserAchievement();
            ua.setUser(user);
            ua.setAchievement(achievement);
            userAchievementRepository.save(ua);

            awardCoinsForAchievement(user, achievement);
            log.info("Пользователю {} выдана ачивка {}", user.getName(), achievement.getName());
        }
    }

    // === РУЧНЫЕ АЧИВКИ (от менеджера/админа) ===

    @Transactional
    public void grantManualAchievement(Long actorId, Long userId, String code) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Актор не найден"));

        if (!actor.getRole().has(Permission.USER_MANAGE)) {
            throw new SecurityException("Нет прав на выдачу ачивок");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

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

            awardCoinsForAchievement(user, achievement);
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ НАЧИСЛЕНИЯ МОНЕТ ===
    private void awardCoinsForAchievement(User user, Achievement achievement) {
        BigDecimal reward = achievement.getRewardCoins();
        if (reward != null && reward.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            user.setBalance(currentBalance.add(reward));
            userRepository.save(user);

            CoinTransaction tx = new CoinTransaction();
            tx.setFromUserId(null);
            tx.setToUserId(user.getId());
            tx.setAmount(reward);
            tx.setType(CoinTransactionType.ACHIEVEMENT);
            tx.setComment("Награда за достижение: " + achievement.getName());
            coinTxRepository.save(tx);
        }
    }

    // === ПОЛУЧЕНИЕ СПИСКА АЧИВОК ПОЛЬЗОВАТЕЛЯ ===
    @Transactional(readOnly = true)
    public List<AchievementResponseDTO> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> new AchievementResponseDTO(
                        ua.getAchievement().getId(),
                        ua.getAchievement().getCode(),
                        ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(),
                        ua.getAchievement().getIconUrl(),
                        ua.getAchievement().getRewardCoins(),
                        ua.getEarnedAt()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }
}