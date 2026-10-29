package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.StreakInfoDTO;
import com.sprint.tms_backend.model.RatingTransaction;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.RatingTransactionRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;
    private final RatingTransactionRepository ratingTxRepository;

    private static final int REWARD_EVERY_DAYS = 10;   // каждые 10 дней
    private static final int REWARD_RATING_10 = 1;     // +1 рейтинг
    private static final int MONTH_DAYS = 30;          // месяц = 30 дней
    private static final int REWARD_RATING_MONTH = 2;  // доп. +2 рейтинга

    @Transactional
    public StreakInfoDTO touchStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastActiveDate();

        boolean isNewDay = last == null || !last.equals(today);
        int earnedToday = 0;

        if (isNewDay) {
            if (last != null && last.equals(today.minusDays(1))) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else {
                user.setCurrentStreak(1);
            }
            user.setLastActiveDate(today);

            if (user.getCurrentStreak() > user.getLongestStreak()) {
                user.setLongestStreak(user.getCurrentStreak());
            }

            // Награды
            int streak = user.getCurrentStreak();
            int lastReward = user.getLastStreakRewardAt();

            if (streak >= lastReward + REWARD_EVERY_DAYS) {
                int totalReward = 0;
                StringBuilder reasons = new StringBuilder();

                for (int day = lastReward + REWARD_EVERY_DAYS; day <= streak; day += REWARD_EVERY_DAYS) {
                    totalReward += REWARD_RATING_10;
                    reasons.append("+").append(REWARD_RATING_10).append(" за ").append(day).append(" дней; ");

                    if (day % MONTH_DAYS == 0) {
                        totalReward += REWARD_RATING_MONTH;
                        reasons.append("+").append(REWARD_RATING_MONTH)
                                .append(" за ").append(day / 30).append(" мес.; ");
                    }
                }

                if (totalReward > 0) {
                    double curRating = user.getRating() != null ? user.getRating() : 0.0;
                    user.setRating(curRating + totalReward);

                    RatingTransaction rt = new RatingTransaction();
                    rt.setUserId(userId);
                    rt.setAmount((double) totalReward);
                    rt.setReason("Streak " + streak + " дней: " + reasons.toString().trim());
                    ratingTxRepository.save(rt);

                    earnedToday = totalReward;
                    log.info("Streak reward: user={} streak={} reward=+{}", userId, streak, totalReward);
                }

                user.setLastStreakRewardAt(streak);
            }

            userRepository.save(user);
        }

        return buildInfo(user, earnedToday);
    }

    @Transactional(readOnly = true)
    public StreakInfoDTO getStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return buildInfo(user, 0);
    }

    private StreakInfoDTO buildInfo(User user, int earnedToday) {
        int streak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        int nextReward = ((streak / REWARD_EVERY_DAYS) + 1) * REWARD_EVERY_DAYS;
        int daysToNext = nextReward - streak;

        int nextMonth = ((streak / MONTH_DAYS) + 1) * MONTH_DAYS;
        int daysToMonth = nextMonth - streak;

        return new StreakInfoDTO(
                streak,
                user.getLongestStreak() != null ? user.getLongestStreak() : 0,
                user.getLastActiveDate(),
                daysToNext,
                nextReward,
                daysToMonth,
                nextMonth,
                earnedToday
        );
    }
}