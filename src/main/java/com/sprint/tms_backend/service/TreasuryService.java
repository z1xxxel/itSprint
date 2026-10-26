package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.Treasury;
import com.sprint.tms_backend.repository.TreasuryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreasuryService {

    private final TreasuryRepository treasuryRepository;

    @Transactional(readOnly = true)
    public Treasury getTreasury() {
        return treasuryRepository.findById(1L).orElseGet(() -> {
            Treasury t = new Treasury();
            t.setBalance(BigDecimal.ZERO);
            return t; // Возвращаем пустую, если еще не создана
        });
    }

    // HR/Админ пополняет бюджет компании
    @Transactional
    public Treasury fundTreasury(BigDecimal amount) {
        Treasury treasury = treasuryRepository.findByIdForUpdate().orElseGet(() -> {
            Treasury t = new Treasury();
            t.setId(1L);
            return t;
        });

        treasury.setBalance(treasury.getBalance().add(amount));
        treasury.setLastFundedAt(LocalDateTime.now());

        log.info("Казна пополнена на {}. Текущий баланс: {}", amount, treasury.getBalance());
        return treasuryRepository.save(treasury);
    }

    // Списание средств из Казны
    @Transactional
    public void withdraw(BigDecimal amount) {
        Treasury treasury = treasuryRepository.findByIdForUpdate()
                .orElseThrow(() -> new RuntimeException("Казна не инициализирована"));

        if (treasury.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("В казне компании недостаточно средств! Обратитесь к HR. " +
                    "Доступно: " + treasury.getBalance() + ", требуется: " + amount);
        }

        treasury.setBalance(treasury.getBalance().subtract(amount));
        treasuryRepository.save(treasury);
        log.info("Из казны списано {}. Остаток: {}", amount, treasury.getBalance());
    }
}