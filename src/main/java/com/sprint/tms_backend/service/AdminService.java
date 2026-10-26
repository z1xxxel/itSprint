package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final SupportTicketRepository ticketRepository;
    private final UserQuestRepository userQuestRepository;
    private final SystemSettingRepository settingRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final RatingTransactionRepository ratingTxRepository;
    private final TreasuryService treasuryService;

    // ==========================================
    // 1. АНАЛИТИКА (ДАШБОРД)
    // ==========================================
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboard(Long actorId) {
        checkAdminRights(actorId);
        BigDecimal totalCoins = userRepository.sumAllBalances();
        if (totalCoins == null) totalCoins = BigDecimal.ZERO;

        return new AdminDashboardDTO(
                userRepository.count(), productRepository.count(), totalCoins,
                orderRepository.countByStatus(OrderStatus.PENDING),
                eventRepository.countByEventStatus(EventStatus.NEW),
                ticketRepository.countByStatus(TicketStatus.PENDING),
                userQuestRepository.countByStatus(QuestProgressStatus.PENDING_REVIEW)
        );
    }

    // ==========================================
    // 2. НАСТРОЙКИ
    // ==========================================
    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings(Long actorId) {
        checkAdminRights(actorId);
        return settingRepository.findAll();
    }

    @Transactional
    public SystemSetting updateSetting(String key, String value, Long actorId) {
        checkAdminRights(actorId);
        SystemSetting setting = settingRepository.findById(key).orElse(new SystemSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        return settingRepository.save(setting);
    }

    // ==========================================
    // 3. ГЛОБАЛЬНЫЙ ПРОСМОТР ТИКЕТОВ
    // ==========================================
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets(Long actorId) {
        checkAdminRights(actorId);
        return ticketRepository.findAll().stream().map(t -> new SupportTicketDTO(
                t.getId(), t.getTopic(), t.getDescription(),
                t.getCreator().getId(), t.getCreator().getName(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getName() : null,
                t.getStatus(), t.getCreatedAt(), t.getClosedAt(), t.getRating(), t.getReviewComment()
        )).toList();
    }

    // ==========================================
    // 4. УПРАВЛЕНИЕ КВЕСТАМИ
    // ==========================================
    @Transactional(readOnly = true)
    public List<AdminQuestDTO> getAllQuests(Long actorId) {
        checkAdminRights(actorId);
        return userQuestRepository.findAll().stream().map(this::mapToQuestDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminQuestDTO> getPendingQuests(Long actorId) {
        checkAdminRights(actorId);
        return userQuestRepository.findByStatus(QuestProgressStatus.PENDING_REVIEW).stream().map(this::mapToQuestDto).toList();
    }

    @Transactional
    public void approveQuest(Long userQuestId, Long actorId) {
        checkAdminRights(actorId);
        UserQuest userQuest = userQuestRepository.findById(userQuestId).orElseThrow();

        if (userQuest.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Можно одобрить только квесты на проверке");
        }

        treasuryService.withdraw(userQuest.getQuest().getRewardCoins());

        userQuest.setStatus(QuestProgressStatus.COMPLETED);
        userQuest.setCompletedAt(LocalDateTime.now());
        userQuestRepository.save(userQuest);

        User user = userQuest.getUser();
        BigDecimal reward = userQuest.getQuest().getRewardCoins();
        user.setBalance((user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO).add(reward));
        userRepository.save(user);

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(null); tx.setToUserId(user.getId()); tx.setAmount(reward);
        tx.setType(CoinTransactionType.QUEST_REWARD);
        tx.setComment("Награда за квест (одобрено админом): " + userQuest.getQuest().getTitle());
        coinTxRepository.save(tx);
    }

    @Transactional
    public void rejectQuest(Long userQuestId, Long actorId, String reason) {
        checkAdminRights(actorId);
        UserQuest userQuest = userQuestRepository.findById(userQuestId).orElseThrow();

        if (userQuest.getStatus() != QuestProgressStatus.PENDING_REVIEW) {
            throw new RuntimeException("Можно отклонить только квесты на проверке");
        }
        userQuest.setStatus(QuestProgressStatus.REJECTED);
        userQuest.setAdminComment(reason);
        userQuestRepository.save(userQuest);
    }

    // ОТМЕНА УЖЕ ЗАВЕРШЕННОГО КВЕСТА
    @Transactional
    public void cancelCompletedQuest(Long userQuestId, Long actorId, String reason) {
        checkAdminRights(actorId);
        UserQuest userQuest = userQuestRepository.findById(userQuestId).orElseThrow();

        if (userQuest.getStatus() != QuestProgressStatus.COMPLETED) {
            throw new RuntimeException("Отменить можно только уже выполненный (COMPLETED) квест");
        }

        User user = userQuest.getUser();
        BigDecimal reward = userQuest.getQuest().getRewardCoins();

        // Проверяем, есть ли у юзера деньги, чтобы забрать их обратно
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(reward) < 0) {
            throw new RuntimeException("Невозможно отменить квест: сотрудник уже потратил монеты");
        }

        // Забираем монеты
        user.setBalance(currentBalance.subtract(reward));
        userRepository.save(user);

        // Возвращаем в Казну
        treasuryService.fundTreasury(reward);

        // Меняем статус на CANCELLED
        userQuest.setStatus(QuestProgressStatus.CANCELLED);
        userQuest.setAdminComment("Отменено админом: " + reason);
        userQuestRepository.save(userQuest);

        // Лог транзакции
        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(user.getId()); // Списываем у юзера
        tx.setToUserId(null);
        tx.setAmount(reward);
        tx.setType(CoinTransactionType.REFUND);
        tx.setComment("Отмена выполненного квеста #" + userQuestId + ". Возврат в казну. Причина: " + reason);
        coinTxRepository.save(tx);
    }

    // ==========================================
    // 5. ТРАНЗАКЦИИ И ШТРАФЫ
    // ==========================================
    @Transactional(readOnly = true)
    public Page<CoinTransactionDTO> getAllTransactions(Long actorId, int page, int size) {
        checkAdminRights(actorId);
        return coinTxRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(t -> new CoinTransactionDTO(
                        t.getId(), t.getFromUserId(), t.getToUserId(), t.getAmount(),
                        t.getType(), t.getComment(), t.getTag(),
                        t.getIsCancelled(), t.getCancellationReason(), t.getCreatedAt()
                ));
    }

    @Transactional
    public void cancelTransaction(Long transactionId, Long actorId, String reason) {
        checkAdminRights(actorId);
        CoinTransaction original = coinTxRepository.findById(transactionId).orElseThrow();

        if (Boolean.TRUE.equals(original.getIsCancelled())) throw new RuntimeException("Уже отменена");
        if (original.getType() == CoinTransactionType.PURCHASE) throw new RuntimeException("Отменяйте через магазин");

        User fromUser = original.getFromUserId() != null ? userRepository.findByIdForUpdate(original.getFromUserId()).orElseThrow() : null;
        User toUser = original.getToUserId() != null ? userRepository.findByIdForUpdate(original.getToUserId()).orElseThrow() : null;

        if (toUser != null && (toUser.getBalance() == null || toUser.getBalance().compareTo(original.getAmount()) < 0)) {
            throw new RuntimeException("У получателя недостаточно средств для отмены");
        }

        if (fromUser != null && toUser != null) {
            toUser.setBalance(toUser.getBalance().subtract(original.getAmount()));
            fromUser.setBalance(fromUser.getBalance().add(original.getAmount()));
            userRepository.save(toUser); userRepository.save(fromUser);
        } else if (fromUser == null && toUser != null) {
            toUser.setBalance(toUser.getBalance().subtract(original.getAmount()));
            userRepository.save(toUser);
            treasuryService.fundTreasury(original.getAmount());
        }

        original.setIsCancelled(true);
        original.setCancellationReason(reason);
        coinTxRepository.save(original);

        CoinTransaction reversal = new CoinTransaction();
        reversal.setFromUserId(original.getToUserId()); reversal.setToUserId(original.getFromUserId());
        reversal.setAmount(original.getAmount()); reversal.setType(CoinTransactionType.REFUND);
        reversal.setComment("Отмена транзакции #" + original.getId() + ". Причина: " + reason);
        coinTxRepository.save(reversal);
    }

    // НАЛОЖЕНИЕ ШТРАФА (Доступно Админу и Менеджеру)
    @Transactional
    public void applyPenalty(PenaltyRequest request) {
        User actor = userRepository.findById(request.actorId()).orElseThrow();

        // Проверяем права (Менеджер или Админ)
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.HR && actor.getRole() != UserRole.MANAGER) {
            throw new SecurityException("Только менеджер или админ может накладывать штрафы");
        }

        User user = userRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getId().equals(actor.getId())) {
            throw new RuntimeException("Нельзя оштрафовать самого себя");
        }

        // 1. Штраф монетами
        if (request.coins() != null && request.coins().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            if (currentBalance.compareTo(request.coins()) < 0) {
                throw new RuntimeException("У сотрудника недостаточно монет для списания штрафа. Баланс: " + currentBalance);
            }
            user.setBalance(currentBalance.subtract(request.coins()));

            // Возвращаем штраф в Казну
            treasuryService.fundTreasury(request.coins());

            CoinTransaction tx = new CoinTransaction();
            tx.setFromUserId(user.getId());
            tx.setToUserId(null); // Сжигание / Возврат в казну
            tx.setAmount(request.coins());
            tx.setType(CoinTransactionType.PENALTY);
            tx.setComment("Штраф от " + actor.getName() + ". Причина: " + request.reason());
            coinTxRepository.save(tx);
        }

        // 2. Штраф рейтингом
        if (request.rating() != null && request.rating() > 0) {
            double currentRating = user.getRating() != null ? user.getRating() : 0.0;
            double newRating = currentRating - request.rating();
            if (newRating < 0) newRating = 0.0; // Защита от ухода в минус

            user.setRating(newRating);

            RatingTransaction rt = new RatingTransaction();
            rt.setUserId(user.getId());
            rt.setAmount(-request.rating());
            rt.setReason("Штраф от " + actor.getName() + ". Причина: " + request.reason());
            ratingTxRepository.save(rt);
        }

        userRepository.save(user);
        log.info("{} наложил штраф на {}: {} монет, {} рейтинга", actor.getEmail(), user.getEmail(), request.coins(), request.rating());
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================
    private void checkAdminRights(Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow();
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.HR) {
            throw new SecurityException("Нет доступа к административной панели");
        }
    }

    private AdminQuestDTO mapToQuestDto(UserQuest uq) {
        return new AdminQuestDTO(
                uq.getId(), uq.getQuest().getTitle(), uq.getUser().getName(),
                uq.getStatus(), uq.getStartedAt(), uq.getSubmittedAt(), uq.getCompletedAt(), uq.getAdminComment()
        );
    }
}