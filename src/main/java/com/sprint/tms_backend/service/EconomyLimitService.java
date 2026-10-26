package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class EconomyLimitService {

    private final UserRepository userRepository;
    private static final BigDecimal MONTHLY_LIMIT = new BigDecimal("10000.00");

    @Transactional
    public void checkMonthlyLimit(User user, BigDecimal amountToEarn) {
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);

        if (user.getLastMonthlyReset() == null ||
                !YearMonth.from(user.getLastMonthlyReset()).equals(currentMonth)) {

            user.setMonthlyEarnedCoins(BigDecimal.ZERO);
            user.setLastMonthlyReset(now);
            userRepository.save(user);
        }

        BigDecimal currentEarned = user.getMonthlyEarnedCoins() != null
                ? user.getMonthlyEarnedCoins() : BigDecimal.ZERO;

        if (currentEarned.add(amountToEarn).compareTo(MONTHLY_LIMIT) > 0) {
            throw new RuntimeException("Превышен месячный лимит заработка (" + MONTHLY_LIMIT +
                    " монет). Вы уже заработали " + currentEarned + " монет в этом месяце.");
        }

        user.setMonthlyEarnedCoins(currentEarned.add(amountToEarn));
        userRepository.save(user);
    }
}