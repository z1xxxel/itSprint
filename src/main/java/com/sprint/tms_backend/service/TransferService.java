package com.sprint.tms_backend.service;

import com.sprint.tms_backend.Exception.InsufficientFundsException;
import com.sprint.tms_backend.Exception.InvalidTransferException;
import com.sprint.tms_backend.Exception.UserNotFoundException;
import com.sprint.tms_backend.config.TransferLimits;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final TransferLimits limits;
    private final TreasuryService treasuryService;
    private final EconomyLimitService economyLimitService; // <--- ДОБАВЛЕНО

    // === 1. Peer-to-peer перевод (сотрудник → сотруднику). НЕ считается заработком! ===
    @Transactional
    public CoinTransaction transferCoins(Long fromUserId, Long toUserId,
                                         BigDecimal amount, String comment, ThanksTag tag) {

        validateBasic(fromUserId, toUserId, amount);

        User fromUser;
        User toUser;
        if (fromUserId.compareTo(toUserId) < 0) {
            fromUser = lockUser(fromUserId, "Отправитель не найден");
            toUser   = lockUser(toUserId,   "Получатель не найден");
        } else {
            toUser   = lockUser(toUserId,   "Получатель не найден");
            fromUser = lockUser(fromUserId, "Отправитель не найден");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        BigDecimal sentToUser = coinTxRepository.sumSentBetween(
                fromUserId, toUserId, CoinTransactionType.PEER_THANKS, startOfDay);
        if (sentToUser.add(amount).compareTo(limits.getPeer().getDailyPerRecipient()) > 0) {
            throw new InvalidTransferException(
                    "Дневной лимит одному коллеге: " + limits.getPeer().getDailyPerRecipient()
                            + ". Уже отправлено сегодня: " + sentToUser);
        }

        BigDecimal sentTotal = coinTxRepository.sumSentTotal(
                fromUserId, CoinTransactionType.PEER_THANKS, startOfDay);
        if (sentTotal.add(amount).compareTo(limits.getPeer().getDailyTotal()) > 0) {
            throw new InvalidTransferException(
                    "Дневной лимит переводов: " + limits.getPeer().getDailyTotal()
                            + ". Уже отправлено сегодня: " + sentTotal);
        }

        BigDecimal fromBalance = safeBalance(fromUser);
        if (fromBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на балансе");
        }

        fromUser.setBalance(fromBalance.subtract(amount));
        toUser.setBalance(safeBalance(toUser).add(amount));

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(fromUserId);
        tx.setToUserId(toUserId);
        tx.setAmount(amount);
        tx.setType(CoinTransactionType.PEER_THANKS);
        tx.setComment(comment);
        tx.setTag(tag);
        return coinTxRepository.save(tx);
    }

    // === 2. Списание (для магазина). НЕ считается заработком! ===
    @Transactional
    public CoinTransaction spendCoins(Long userId, BigDecimal amount, String comment) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Сумма списания должна быть положительной");
        }
        User user = lockUser(userId, "Пользователь не найден");

        BigDecimal balance = safeBalance(user);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на балансе");
        }
        user.setBalance(balance.subtract(amount));

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(userId);
        tx.setToUserId(null);
        tx.setAmount(amount);
        tx.setType(CoinTransactionType.PURCHASE);
        tx.setComment(comment);
        return coinTxRepository.save(tx);
    }

    // === 3. Grant: MANAGER → подчинённому, ADMIN → любому. Считается заработком! ===
    @Transactional
    public CoinTransaction grantCoins(Long actorId, Long toUserId,
                                      BigDecimal amount, String comment) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Сумма должна быть положительной");
        }
        if (actorId == null || toUserId == null) {
            throw new InvalidTransferException("ID обязательны");
        }
        if (actorId.equals(toUserId)) {
            throw new InvalidTransferException("Нельзя начислить самому себе");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new UserNotFoundException("Актор не найден"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new UserNotFoundException("Получатель не найден"));

        CoinTransactionType type;
        if (actor.getRole().has(Permission.COIN_GRANT_ANY)) {
            type = CoinTransactionType.ADMIN_ADJUSTMENT;
        } else if (actor.getRole().has(Permission.COIN_GRANT_SUBORDINATE)) {
            type = CoinTransactionType.MANAGER_BONUS;

            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

            BigDecimal sentToUser = coinTxRepository.sumSentBetween(
                    actorId, toUserId, type, startOfDay);
            if (sentToUser.add(amount).compareTo(limits.getManager().getDailyPerSubordinate()) > 0) {
                throw new InvalidTransferException(
                        "Дневной лимит бонусов одному сотруднику: "
                                + limits.getManager().getDailyPerSubordinate()
                                + ". Уже выдано сегодня: " + sentToUser);
            }

            BigDecimal sentTotal = coinTxRepository.sumSentTotal(actorId, type, startOfDay);
            if (sentTotal.add(amount).compareTo(limits.getManager().getDailyTotal()) > 0) {
                throw new InvalidTransferException(
                        "Дневной лимит бонусов: " + limits.getManager().getDailyTotal()
                                + ". Уже выдано сегодня: " + sentTotal);
            }
        } else {
            throw new SecurityException("Нет прав на начисление бонусов");
        }

        // ПРОВЕРКА МЕСЯЧНОГО ЛИМИТА (перед начислением)
        economyLimitService.checkMonthlyLimit(toUser, amount);

        // СПИСЫВАЕМ ИЗ КАЗНЫ
        treasuryService.withdraw(amount);

        toUser.setBalance(safeBalance(toUser).add(amount));
        userRepository.save(toUser);

        CoinTransaction tx = new CoinTransaction();
        tx.setFromUserId(null);
        tx.setToUserId(toUserId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setComment(comment);
        log.info("{} начислил {} монет пользователю {} ({})",
                actor.getEmail(), amount, toUserId, type);
        return coinTxRepository.save(tx);
    }

    // === helpers ===
    private void validateBasic(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Сумма перевода должна быть положительной");
        }
        if (fromUserId == null || toUserId == null) {
            throw new InvalidTransferException("ID пользователей обязательны");
        }
        if (fromUserId.equals(toUserId)) {
            throw new InvalidTransferException("Нельзя перевести монеты самому себе");
        }
    }

    private BigDecimal safeBalance(User user) {
        return user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
    }

    private User lockUser(Long id, String notFoundMessage) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new UserNotFoundException(notFoundMessage));
    }
}