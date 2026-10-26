package com.sprint.tms_backend.service;

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
public class EventRewardService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final RatingTransactionRepository ratingTxRepository;
    private final TreasuryService treasuryService;
    private final EconomyLimitService economyLimitService; // <--- ДОБАВЛЕНО

    @Transactional
    public void completeAndAward(Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        if (event.getEventStatus() != EventStatus.ASSIGNED) {
            return;
        }

        if (Boolean.TRUE.equals(event.getRewardsGranted())) {
            event.setEventStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            return;
        }

        boolean paid = Boolean.TRUE.equals(event.getPaidInCoins());
        if (paid) {
            List<EventParticipant> participants = participantRepository.findByEventId(eventId);

            if (!participants.isEmpty() && event.getRewardCoins() != null) {
                // Считаем общую сумму и списываем из Казны ОДНИМ разом
                BigDecimal totalPayout = event.getRewardCoins().multiply(BigDecimal.valueOf(participants.size()));
                treasuryService.withdraw(totalPayout);

                for (EventParticipant p : participants) {
                    User user = p.getUser();
                    BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

                    // ПРОВЕРКА МЕСЯЧНОГО ЛИМИТА (в try-catch, чтобы не блокировать остальных)
                    if (event.getRewardCoins() != null) {
                        try {
                            economyLimitService.checkMonthlyLimit(user, event.getRewardCoins());
                        } catch (RuntimeException e) {
                            log.warn("Пользователь {} не получил награду за событие {}: {}",
                                    user.getId(), eventId, e.getMessage());
                            // Возвращаем его часть в Казну, так как награда не выдана
                            treasuryService.fundTreasury(event.getRewardCoins());
                            continue; // пропускаем этого участника
                        }
                    }

                    if (event.getRewardCoins() != null) {
                        user.setBalance(currentBalance.add(event.getRewardCoins()));
                    }

                    if (event.getRewardRating() != null) {
                        double currentRating = user.getRating() != null ? user.getRating() : 0.0;
                        user.setRating(currentRating + event.getRewardRating());

                        RatingTransaction rt = new RatingTransaction();
                        rt.setUserId(user.getId());
                        rt.setAmount(event.getRewardRating());
                        rt.setReason("Награда за событие #" + event.getId() + ": " + event.getNameEvent());
                        ratingTxRepository.save(rt);
                    }
                    userRepository.save(user);

                    if (event.getRewardCoins() != null) {
                        CoinTransaction tx = new CoinTransaction();
                        tx.setFromUserId(null);
                        tx.setToUserId(user.getId());
                        tx.setAmount(event.getRewardCoins());
                        tx.setType(CoinTransactionType.EVENT_REWARD);
                        tx.setComment("Награда за событие #" + eventId + ": " + event.getNameEvent());
                        coinTxRepository.save(tx);
                    }
                }
            }
            log.info("Событие {} завершено. Награда выдана участникам", eventId);
        }

        event.setEventStatus(EventStatus.COMPLETED);
        event.setRewardsGranted(true);
        eventRepository.save(event);
    }
}