package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.CreateQuestRequest;
import com.sprint.tms_backend.DTO.QuestResponseDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final TreasuryService treasuryService;
    private final EconomyLimitService economyLimitService; // <--- ДОБАВЛЕНО

    // === СОЗДАНИЕ И УДАЛЕНИЕ ===
    @Transactional
    public Quest createQuest(CreateQuestRequest request) {
        User creator = userRepository.findById(request.creatorId())
                .orElseThrow(() -> new RuntimeException("Создатель не найден"));

        if (!creator.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }

        if (request.rewardCoins() == null || request.rewardCoins().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Награда должна быть больше нуля");
        }

        Quest quest = new Quest();
        quest.setTitle(request.title());
        quest.setDescription(request.description());
        quest.setRewardCoins(request.rewardCoins());
        quest.setCreator(creator);
        quest.setIsActive(true);

        return questRepository.save(quest);
    }

    @Transactional
    public void deleteQuest(Long questId, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на удаление квестов");
        }

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Квест не найден"));

        quest.setIsActive(false);
        questRepository.save(quest);
    }

    // === ПРОСМОТР И ПРОХОЖДЕНИЕ ===
    @Transactional(readOnly = true)
    public List<QuestResponseDTO> getAvailableQuests(Long userId) {
        List<Quest> activeQuests = questRepository.findByIsActiveTrue();
        List<QuestResponseDTO> result = new ArrayList<>();

        for (Quest q : activeQuests) {
            UserQuest progress = userQuestRepository.findByUserIdAndQuestId(userId, q.getId()).orElse(null);
            QuestProgressStatus status = progress != null ? progress.getStatus() : null;
            String comment = progress != null ? progress.getAdminComment() : null;

            result.add(new QuestResponseDTO(
                    q.getId(), q.getTitle(), q.getDescription(), q.getRewardCoins(),
                    status, comment
            ));
        }
        return result;
    }

    @Transactional
    public void startQuest(Long questId, Long userId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Квест не найден"));

        if (!quest.getIsActive()) {
            throw new RuntimeException("Этот квест больше не доступен");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (userQuestRepository.findByUserIdAndQuestId(userId, questId).isPresent()) {
            throw new RuntimeException("Вы уже взяли этот квест");
        }

        UserQuest userQuest = new UserQuest();
        userQuest.setUser(user);
        userQuest.setQuest(quest);
        userQuest.setStatus(QuestProgressStatus.IN_PROGRESS);
        userQuestRepository.save(userQuest);
    }

    @Transactional
    public void submitQuest(Long questId, Long userId) {
        UserQuest userQuest = userQuestRepository.findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new RuntimeException("Вы не брали этот квест"));

        if (userQuest.getStatus() != QuestProgressStatus.IN_PROGRESS && userQuest.getStatus() != QuestProgressStatus.REJECTED) {
            throw new RuntimeException("Квест уже на проверке или выполнен");
        }

        userQuest.setStatus(QuestProgressStatus.PENDING_REVIEW);
        userQuest.setSubmittedAt(LocalDateTime.now());
        userQuest.setAdminComment(null);
        userQuestRepository.save(userQuest);
    }

    // === ПРОВЕРКА КВЕСТОВ (Менеджер) ===
    @Transactional(readOnly = true)
    public List<UserQuest> getQuestsForReview(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на проверку квестов");
        }
        return userQuestRepository.findByStatus(QuestProgressStatus.PENDING_REVIEW);
    }

    @Transactional
    public void approveQuest(Long userQuestId, Long actorId) {
        checkManagerRights(actorId);

        UserQuest userQuest = userQuestRepository.findById(userQuestId)
                .orElseThrow(() -> new RuntimeException("Запись о квесте не найдена"));

        if (userQuest.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Квест не находится на проверке");
        }

        User user = userQuest.getUser();
        BigDecimal reward = userQuest.getQuest().getRewardCoins();

        // ПРОВЕРКА МЕСЯЧНОГО ЛИМИТА
        economyLimitService.checkMonthlyLimit(user, reward);

        // СПИСЫВАЕМ ИЗ КАЗНЫ (только после успешной проверки лимита)
        treasuryService.withdraw(reward);

        userQuest.setStatus(QuestProgressStatus.COMPLETED);
        userQuest.setCompletedAt(LocalDateTime.now());
        userQuestRepository.save(userQuest);

        // Начисляем монеты пользователю
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(reward));
        userRepository.save(user);

        // Пишем в журнал транзакций
        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(null);
        tx.setToUserId(user.getId());
        tx.setAmount(reward);
        tx.setType(CoinTransactionType.QUEST_REWARD);
        tx.setComment("Награда за квест: " + userQuest.getQuest().getTitle());
        coinTxRepository.save(tx);
    }

    @Transactional
    public void rejectQuest(Long userQuestId, Long actorId, String reason) {
        checkManagerRights(actorId);

        UserQuest userQuest = userQuestRepository.findById(userQuestId)
                .orElseThrow(() -> new RuntimeException("Запись о квесте не найдена"));

        if (userQuest.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Квест не находится на проверке");
        }

        userQuest.setStatus(QuestProgressStatus.REJECTED);
        userQuest.setAdminComment(reason);
        userQuestRepository.save(userQuest);
    }

    private void checkManagerRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }
    }
}