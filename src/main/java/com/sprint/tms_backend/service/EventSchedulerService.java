package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSchedulerService {

    private final EventRepository eventRepository;
    private final EventRewardService rewardService;

    @Scheduled(fixedRate = 30_000)
    public void completeExpiredEvents() {
        List<Long> ids = eventRepository.findIdsToComplete(
                EventStatus.ASSIGNED, LocalDateTime.now());

        for (Long id : ids) {
            try {
                rewardService.completeAndAward(id);
            } catch (Exception e) {
                log.error("Не удалось завершить событие {}", id, e);
            }
        }
    }
}