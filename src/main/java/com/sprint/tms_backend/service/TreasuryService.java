package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.TreasuryLogDTO;
import com.sprint.tms_backend.model.Treasury;
import com.sprint.tms_backend.model.TreasuryTransaction;
import com.sprint.tms_backend.model.TreasuryTxType;
import com.sprint.tms_backend.repository.TreasuryRepository;
import com.sprint.tms_backend.repository.TreasuryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreasuryService {

    private final TreasuryRepository treasuryRepository;
    private final TreasuryTransactionRepository txRepository;

    @Transactional(readOnly = true)
    public Treasury getTreasury() {
        return treasuryRepository.findById(1L).orElseGet(() -> {
            Treasury t = new Treasury();
            t.setBalance(BigDecimal.ZERO);
            return t;
        });
    }

    // === Пополнение (HR/ADMIN) ===
    @Transactional
    public Treasury fundTreasury(BigDecimal amount) {
        return doFund(amount, TreasuryTxType.FUND, "Пополнение казны", null, null);
    }

    @Transactional
    public Treasury fundTreasury(BigDecimal amount, TreasuryTxType type, String description) {
        return doFund(amount, type, description, null, null);
    }

    private Treasury doFund(BigDecimal amount, TreasuryTxType type, String description,
                            Long actorId, String actorName) {
        Treasury treasury = treasuryRepository.findByIdForUpdate().orElseGet(() -> {
            Treasury t = new Treasury();
            t.setId(1L);
            t.setBalance(BigDecimal.ZERO);
            return t;
        });

        treasury.setBalance(treasury.getBalance().add(amount));
        treasury.setLastFundedAt(LocalDateTime.now());
        Treasury saved = treasuryRepository.save(treasury);

        writeLog(type, amount, description, actorId, actorName, saved.getBalance());
        log.info("Казна пополнена на {} ({}). Итого: {}", amount, type, saved.getBalance());
        return saved;
    }

    // === Автоматические выплаты (from quests, events, bonuses, etc.) ===
    @Transactional
    public void withdraw(BigDecimal amount) {
        doWithdraw(amount, TreasuryTxType.REWARD_PAYOUT, "Выплата награды", null, null);
    }

    @Transactional
    public void withdraw(BigDecimal amount, TreasuryTxType type, String description) {
        doWithdraw(amount, type, description, null, null);
    }

    // === Ручное списание (только ADMIN) ===
    @Transactional
    public Treasury manualWithdraw(BigDecimal amount, Long actorId, String actorName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Сумма должна быть положительной");
        }
        Treasury treasury = treasuryRepository.findByIdForUpdate()
                .orElseThrow(() -> new RuntimeException("Казна не инициализирована"));
        if (treasury.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                    "В казне недостаточно средств. Доступно: " + treasury.getBalance()
                            + ", требуется: " + amount);
        }
        treasury.setBalance(treasury.getBalance().subtract(amount));
        treasury.setLastFundedAt(LocalDateTime.now());
        Treasury saved = treasuryRepository.save(treasury);

        writeLog(TreasuryTxType.WITHDRAW, amount, "Ручное списание",
                actorId, actorName, saved.getBalance());
        log.info("Из казны списано {} (ручное). Остаток: {}", amount, saved.getBalance());
        return saved;
    }

    private void doWithdraw(BigDecimal amount, TreasuryTxType type, String description,
                            Long actorId, String actorName) {
        Treasury treasury = treasuryRepository.findByIdForUpdate()
                .orElseThrow(() -> new RuntimeException("Казна не инициализирована"));
        if (treasury.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("В казне компании недостаточно средств! " +
                    "Доступно: " + treasury.getBalance() + ", требуется: " + amount);
        }
        treasury.setBalance(treasury.getBalance().subtract(amount));
        treasuryRepository.save(treasury);

        writeLog(type, amount, description, actorId, actorName, treasury.getBalance());
        log.info("Из казны списано {} ({}). Остаток: {}", amount, type, treasury.getBalance());
    }

    private void writeLog(TreasuryTxType type, BigDecimal amount, String description,
                          Long actorId, String actorName, BigDecimal balanceAfter) {
        TreasuryTransaction tx = new TreasuryTransaction();
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setActorId(actorId);
        tx.setActorName(actorName);
        tx.setBalanceAfter(balanceAfter);
        txRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public Page<TreasuryLogDTO> getLogs(int page, int size) {
        return txRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(t -> new TreasuryLogDTO(
                        t.getId(),
                        t.getType(),
                        t.getType().getLabel(),
                        t.getType().isIncoming(),
                        t.getAmount(),
                        t.getDescription(),
                        t.getActorId(),
                        t.getActorName(),
                        t.getBalanceAfter(),
                        t.getCreatedAt()
                ));
    }
}