package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final RatingTransactionRepository ratingTxRepository;
    private final TreasuryService treasuryService;
    private final EconomyLimitService economyLimitService;

    // ==========================================
    // === СОЗДАНИЕ (старое, публичное) ===
    // ==========================================
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
        quest.setRewardRating(0.0);
        quest.setCreator(creator);
        quest.setIsActive(true);
        quest.setIsPublic(true);
        quest.setIsMandatory(false);
        quest.setAudienceDescription("Все сотрудники");

        return questRepository.save(quest);
    }

    // ==========================================
    // === СОЗДАНИЕ С АУДИТОРИЕЙ ===
    // ==========================================
    @Transactional
    public Quest createQuestWithAudience(CreateQuestWithAudienceRequest req) {
        User creator = userRepository.findById(req.creatorId())
                .orElseThrow(() -> new RuntimeException("Создатель не найден"));

        if (!creator.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }

        if (req.rewardCoins() == null || req.rewardCoins().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Награда должна быть больше нуля");
        }

        // Валидация рейтинговой награды
        Double rewardRating = req.rewardRating() != null ? req.rewardRating() : 0.0;
        if (rewardRating < 0) {
            throw new RuntimeException("Рейтинговая награда не может быть отрицательной");
        }
        if (rewardRating > 5) {
            throw new RuntimeException("Рейтинговая награда не может быть больше 5");
        }

        boolean allUsers = Boolean.TRUE.equals(req.allUsers());
        boolean isMandatory = Boolean.TRUE.equals(req.isMandatory());
        boolean isPublic = allUsers && !isMandatory;

        String audienceDescription = buildAudienceDescription(
                allUsers, req.departmentIds(), req.userIds(), req.excludeUserIds());

        Quest quest = new Quest();
        quest.setTitle(req.title());
        quest.setDescription(req.description());
        quest.setRewardCoins(req.rewardCoins());
        quest.setRewardRating(rewardRating);
        quest.setDeadline(req.deadline());
        quest.setIsMandatory(isMandatory);
        quest.setIsPublic(isPublic);
        quest.setPenaltyCoins(req.penaltyCoins());
        quest.setPenaltyRating(req.penaltyRating());
        quest.setCreator(creator);
        quest.setIsActive(true);
        quest.setAudienceDescription(audienceDescription);

        Quest saved = questRepository.save(quest);

        // Если квест публичный — никого не назначаем
        if (!isPublic) {
            Set<Long> targetIds = resolveAudience(req, creator.getId());
            for (Long uid : targetIds) {
                User u = userRepository.findById(uid).orElse(null);
                if (u == null || Boolean.TRUE.equals(u.getIsBlocked())) continue;
                if (userQuestRepository.findByUserIdAndQuestId(uid, saved.getId()).isPresent()) continue;

                UserQuest uq = new UserQuest();
                uq.setUser(u);
                uq.setQuest(saved);
                uq.setStatus(QuestProgressStatus.IN_PROGRESS);
                userQuestRepository.save(uq);
            }
        }

        return saved;
    }

    private String buildAudienceDescription(
            boolean allUsers, List<Long> departmentIds, List<Long> userIds, List<Long> excludeUserIds) {
        if (allUsers) return "Все сотрудники";
        List<String> parts = new ArrayList<>();
        if (departmentIds != null && !departmentIds.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Long dId : departmentIds) {
                departmentRepository.findById(dId).ifPresent(d -> names.add(d.getName()));
            }
            parts.add("Отделы: " + String.join(", ", names));
        }
        if (userIds != null && !userIds.isEmpty()) {
            parts.add("Сотрудники: " + userIds.size() + " чел.");
        }
        if (excludeUserIds != null && !excludeUserIds.isEmpty()) {
            parts.add("Исключено: " + excludeUserIds.size());
        }
        return parts.isEmpty() ? "Не задано" : String.join(" · ", parts);
    }

    private Set<Long> resolveAudience(CreateQuestWithAudienceRequest req, Long creatorId) {
        Set<Long> ids = new HashSet<>();

        if (Boolean.TRUE.equals(req.allUsers())) {
            userRepository.findAll().forEach(u -> ids.add(u.getId()));
        } else {
            if (req.departmentIds() != null) {
                for (Long dId : req.departmentIds()) {
                    userRepository.findByDepartmentId(dId).forEach(u -> ids.add(u.getId()));
                }
            }
            if (req.userIds() != null) {
                ids.addAll(req.userIds());
            }
        }

        if (req.excludeUserIds() != null) {
            ids.removeAll(req.excludeUserIds());
        }
        ids.remove(creatorId);
        return ids;
    }

    // ==========================================
    // === РЕДАКТИРОВАНИЕ ===
    // ==========================================
    @Transactional
    public Quest updateQuest(Long questId, UpdateQuestRequest req) {
        User actor = userRepository.findById(req.actorId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }
        Quest q = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Квест не найден"));

        if (req.title() != null) q.setTitle(req.title());
        if (req.description() != null) q.setDescription(req.description());
        if (req.rewardCoins() != null && req.rewardCoins().compareTo(BigDecimal.ZERO) > 0) {
            q.setRewardCoins(req.rewardCoins());
        }
        if (req.rewardRating() != null) {
            if (req.rewardRating() < 0 || req.rewardRating() > 5) {
                throw new RuntimeException("Рейтинговая награда должна быть от 0 до 5");
            }
            q.setRewardRating(req.rewardRating());
        }
        if (req.deadline() != null) q.setDeadline(req.deadline());
        if (req.penaltyCoins() != null) q.setPenaltyCoins(req.penaltyCoins());
        if (req.penaltyRating() != null) q.setPenaltyRating(req.penaltyRating());

        return questRepository.save(q);
    }

    // ==========================================
    // === ПРОДЛЕНИЕ ДЕДЛАЙНА ===
    // ==========================================
    @Transactional
    public Quest extendDeadline(Long questId, Long actorId, LocalDateTime newDeadline) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }
        Quest q = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Квест не найден"));
        q.setDeadline(newDeadline);
        return questRepository.save(q);
    }

    // ==========================================
    // === УЧАСТНИКИ ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminQuestParticipantDTO> getQuestParticipants(Long questId, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав");
        }
        return userQuestRepository.findByQuestId(questId).stream()
                .map(uq -> new AdminQuestParticipantDTO(
                        uq.getId(),
                        uq.getUser().getId(),
                        uq.getUser().getName(),
                        uq.getStatus(),
                        uq.getStartedAt(),
                        uq.getSubmittedAt(),
                        uq.getCompletedAt()
                ))
                .toList();
    }

    // ==========================================
    // === ЛОГИ (для админки) ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminQuestLogDTO> getAllQuestLogs(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав");
        }

        return userQuestRepository.findAll().stream()
                .sorted((a, b) -> {
                    LocalDateTime da = a.getCompletedAt() != null ? a.getCompletedAt()
                            : (a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getStartedAt());
                    LocalDateTime db = b.getCompletedAt() != null ? b.getCompletedAt()
                            : (b.getSubmittedAt() != null ? b.getSubmittedAt() : b.getStartedAt());
                    return db.compareTo(da);
                })
                .map(uq -> new AdminQuestLogDTO(
                        uq.getId(),
                        uq.getQuest().getId(),
                        uq.getQuest().getTitle(),
                        uq.getUser().getId(),
                        uq.getUser().getName(),
                        uq.getStatus(),
                        uq.getQuest().getRewardCoins(),
                        uq.getQuest().getRewardRating(),
                        uq.getQuest().getPenaltyCoins(),
                        uq.getQuest().getPenaltyRating(),
                        uq.getStartedAt(),
                        uq.getSubmittedAt(),
                        uq.getCompletedAt(),
                        uq.getAdminComment(),
                        uq.getQuest().getCreator() != null
                                ? uq.getQuest().getCreator().getName() : null,
                        uq.getQuest().getCreatedAt()
                ))
                .toList();
    }

    // ==========================================
    // === ШТРАФ ===
    // ==========================================
    @Transactional
    public Map<String, Object> applyQuestPenalty(Long questId, ApplyQuestPenaltyRequest req) {
        User actor = userRepository.findById(req.actorId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Квест не найден"));

        BigDecimal penaltyCoins = Boolean.TRUE.equals(req.applyCoins()) && quest.getPenaltyCoins() != null
                ? quest.getPenaltyCoins() : BigDecimal.ZERO;
        Double penaltyRating = Boolean.TRUE.equals(req.applyRating()) && quest.getPenaltyRating() != null
                ? quest.getPenaltyRating() : 0.0;

        if (penaltyCoins.compareTo(BigDecimal.ZERO) == 0 && penaltyRating == 0.0) {
            throw new RuntimeException("Штраф не задан — нечего применять");
        }

        List<UserQuest> participants;
        if (req.userIds() != null && !req.userIds().isEmpty()) {
            participants = userQuestRepository.findByQuestId(questId).stream()
                    .filter(uq -> req.userIds().contains(uq.getUser().getId()))
                    .toList();
        } else {
            participants = userQuestRepository.findByQuestId(questId).stream()
                    .filter(uq -> uq.getStatus() != QuestProgressStatus.COMPLETED)
                    .filter(uq -> uq.getStatus() != QuestProgressStatus.CANCELLED)
                    .toList();
        }

        int coinsApplied = 0;
        int ratingApplied = 0;
        List<String> skipped = new ArrayList<>();

        for (UserQuest uq : participants) {
            User u = uq.getUser();

            // Монеты
            if (penaltyCoins.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bal = u.getBalance() != null ? u.getBalance() : BigDecimal.ZERO;
                if (bal.compareTo(penaltyCoins) >= 0) {
                    u.setBalance(bal.subtract(penaltyCoins));
                    treasuryService.fundTreasury(penaltyCoins);

                    CoinTransaction tx = new CoinTransaction();
                    tx.setFromUserId(u.getId());
                    tx.setToUserId(null);
                    tx.setAmount(penaltyCoins);
                    tx.setType(CoinTransactionType.PENALTY);
                    tx.setComment("Штраф за невыполнение квеста: " + quest.getTitle()
                            + ". Причина: " + req.reason());
                    coinTxRepository.save(tx);
                    coinsApplied++;
                } else {
                    skipped.add(u.getName() + " (недостаточно монет)");
                }
            }

            // Рейтинг
            if (penaltyRating > 0) {
                double cur = u.getRating() != null ? u.getRating() : 0.0;
                double newR = Math.max(0, cur - penaltyRating);
                u.setRating(newR);

                RatingTransaction rt = new RatingTransaction();
                rt.setUserId(u.getId());
                rt.setAmount(-penaltyRating);
                rt.setReason("Штраф за невыполнение квеста: " + quest.getTitle()
                        + ". Причина: " + req.reason());
                ratingTxRepository.save(rt);
                ratingApplied++;
            }

            if (uq.getStatus() == QuestProgressStatus.IN_PROGRESS
                    || uq.getStatus() == QuestProgressStatus.PENDING_REVIEW) {
                uq.setStatus(QuestProgressStatus.REJECTED);
                uq.setAdminComment("Оштрафован: " + req.reason());
                userQuestRepository.save(uq);
            }

            userRepository.save(u);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("coinsApplied", coinsApplied);
        result.put("ratingApplied", ratingApplied);
        result.put("skipped", skipped);
        result.put("total", participants.size());
        return result;
    }

    // ==========================================
    // === УДАЛЕНИЕ ===
    // ==========================================
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

    // ==========================================
    // === ПРОСМОТР (сотрудник) ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<QuestResponseDTO> getAvailableQuests(Long userId) {
        List<Quest> activeQuests = questRepository.findByIsActiveTrue();
        List<QuestResponseDTO> result = new ArrayList<>();

        for (Quest q : activeQuests) {
            UserQuest progress = userQuestRepository
                    .findByUserIdAndQuestId(userId, q.getId()).orElse(null);

            if (!Boolean.TRUE.equals(q.getIsPublic()) && progress == null) {
                continue;
            }

            QuestProgressStatus status = progress != null ? progress.getStatus() : null;
            String comment = progress != null ? progress.getAdminComment() : null;

            result.add(new QuestResponseDTO(
                    q.getId(), q.getTitle(), q.getDescription(), q.getRewardCoins(),
                    q.getRewardRating(),
                    q.getDeadline(), q.getIsMandatory(), q.getPenaltyCoins(), q.getPenaltyRating(),
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

        UserQuest uq = new UserQuest();
        uq.setUser(user);
        uq.setQuest(quest);
        uq.setStatus(QuestProgressStatus.IN_PROGRESS);
        userQuestRepository.save(uq);
    }

    @Transactional
    public void submitQuest(Long questId, Long userId) {
        UserQuest uq = userQuestRepository.findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new RuntimeException("Вы не брали этот квест"));

        if (uq.getStatus() != QuestProgressStatus.IN_PROGRESS
                && uq.getStatus() != QuestProgressStatus.REJECTED) {
            throw new RuntimeException("Квест уже на проверке или выполнен");
        }
        uq.setStatus(QuestProgressStatus.PENDING_REVIEW);
        uq.setSubmittedAt(LocalDateTime.now());
        uq.setAdminComment(null);
        userQuestRepository.save(uq);
    }

    // ==========================================
    // === ПРОВЕРКА (manager review) ===
    // ==========================================
    @Transactional(readOnly = true)
    public List<UserQuestForReview> getQuestsForReview(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на проверку квестов");
        }
        return userQuestRepository.findByStatus(QuestProgressStatus.PENDING_REVIEW).stream()
                .map(this::mapToReviewDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserQuestForReview> getQuestsForReviewByUser(Long userId, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на проверку квестов");
        }
        return userQuestRepository.findByStatus(QuestProgressStatus.PENDING_REVIEW).stream()
                .filter(uq -> uq.getUser().getId().equals(userId))
                .map(this::mapToReviewDto)
                .toList();
    }

    private UserQuestForReview mapToReviewDto(UserQuest uq) {
        return new UserQuestForReview(
                uq.getId(),
                uq.getUser().getId(),
                uq.getQuest().getTitle(),
                uq.getUser().getName(),
                uq.getStatus().name(),
                uq.getStartedAt(),
                uq.getSubmittedAt(),
                uq.getCompletedAt(),
                uq.getAdminComment()
        );
    }

    @Transactional
    public void approveQuest(Long userQuestId, Long actorId) {
        checkManagerRights(actorId);

        UserQuest uq = userQuestRepository.findById(userQuestId)
                .orElseThrow(() -> new RuntimeException("Запись о квесте не найдена"));
        if (uq.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Квест не находится на проверке");
        }

        User user = uq.getUser();
        Quest quest = uq.getQuest();
        BigDecimal reward = quest.getRewardCoins();
        Double ratingReward = quest.getRewardRating();

        economyLimitService.checkMonthlyLimit(user, reward);
        treasuryService.withdraw(reward);

        uq.setStatus(QuestProgressStatus.COMPLETED);
        uq.setCompletedAt(LocalDateTime.now());
        userQuestRepository.save(uq);

        BigDecimal cur = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(cur.add(reward));

        // Начисление рейтинга
        if (ratingReward != null && ratingReward > 0) {
            double curRating = user.getRating() != null ? user.getRating() : 0.0;
            user.setRating(curRating + ratingReward);

            RatingTransaction rt = new RatingTransaction();
            rt.setUserId(user.getId());
            rt.setAmount(ratingReward);
            rt.setReason("Награда за квест: " + quest.getTitle());
            ratingTxRepository.save(rt);
        }

        userRepository.save(user);

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(null);
        tx.setToUserId(user.getId());
        tx.setAmount(reward);
        tx.setType(CoinTransactionType.QUEST_REWARD);
        tx.setComment("Награда за квест: " + quest.getTitle());
        coinTxRepository.save(tx);
    }

    @Transactional
    public void rejectQuest(Long userQuestId, Long actorId, String reason) {
        checkManagerRights(actorId);

        UserQuest uq = userQuestRepository.findById(userQuestId)
                .orElseThrow(() -> new RuntimeException("Запись о квесте не найдена"));
        if (uq.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Квест не находится на проверке");
        }
        uq.setStatus(QuestProgressStatus.REJECTED);
        uq.setAdminComment(reason);
        userQuestRepository.save(uq);
    }

    private void checkManagerRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.QUEST_MANAGE)) {
            throw new SecurityException("Нет прав на управление квестами");
        }
    }
}