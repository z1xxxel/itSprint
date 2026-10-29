package com.sprint.tms_backend.DTO;

import java.time.LocalDate;

public record StreakInfoDTO(
        Integer currentStreak,
        Integer longestStreak,
        LocalDate lastActiveDate,
        Integer daysToNextReward,
        Integer nextRewardAtStreak,
        Integer daysToMonthBonus,
        Integer nextMonthBonusAtStreak,
        Integer earnedRatingToday
) {}