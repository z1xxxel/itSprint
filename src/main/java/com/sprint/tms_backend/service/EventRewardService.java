package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventParticipant;
import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.EventParticipantRepository;
import com.sprint.tms_backend.repository.EventRepository;
import com.sprint.tms_backend.repository.UserRepository;
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

    @Transactional
    public void completeAndAward(Long eventId) {
        // Блокировка строки — защита от параллельного вызова
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        // Только ASSIGNED → COMPLETED
        if (event.getEventStatus() != EventStatus.ASSIGNED) {
            return;
        }

        // Идемпотентность: награда уже выдана
        if (Boolean.TRUE.equals(event.getRewardsGranted())) {
            event.setEventStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            return;
        }

        boolean paid = Boolean.TRUE.equals(event.getPaidInCoins());
        if (paid) {
            List<EventParticipant> participants = participantRepository.findByEventId(eventId);
            for (EventParticipant p : participants) {
                User user = p.getUser();

                if (event.getRewardCoins() != null) {
                    BigDecimal current = user.getBalance() != null
                            ? user.getBalance() : BigDecimal.ZERO;
                    user.setBalance(current.add(event.getRewardCoins()));
                }
                if (event.getRewardRating() != null) {
                    double currentRating = user.getRating() != null ? user.getRating() : 0.0;
                    user.setRating(currentRating + event.getRewardRating());
                }
                userRepository.save(user);
            }
            log.info("Событие {} завершено. Награда выдана {} участникам",
                    eventId, participants.size());
        }

        event.setEventStatus(EventStatus.COMPLETED);
        event.setRewardsGranted(true);
        eventRepository.save(event);
    }
}