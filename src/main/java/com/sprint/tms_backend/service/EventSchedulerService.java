package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.Treasury;
import com.sprint.tms_backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSchedulerService {

    private final EventRepository eventRepository;
    private final EventRewardService rewardService;
    private final SurveyService surveyService;
    private final SeasonService seasonService;

    // Раз в 30 секунд проверяем события
    @Scheduled(fixedRate = 30_000)
    public void completeExpiredEvents() {
        List<Long> ids = eventRepository.findIdsToComplete(
                EventStatus.ASSIGNED, LocalDateTime.now());

        if (!ids.isEmpty()) {
            log.info("Найдено {} событий для завершения", ids.size());
        }

        for (Long id : ids) {
            try {
                rewardService.completeAndAward(id);
            } catch (Exception e) {
                log.error("Не удалось завершить событие {}", id, e);
            }
        }
    }

    // Раз в 5 минут (300_000 мс) проверяем просроченные опросы
    // (Если хотите раз в час, замените на 3_600_000)
    @Scheduled(fixedRate = 300_000)
    public void checkExpiredSurveys() {
        try {
            surveyService.applyPenaltiesForExpiredSurveys();
        } catch (Exception e) {
            log.error("Ошибка при начислении штрафов за опросы", e);
        }
    }

    @Scheduled(fixedRate = 3_600_000) // раз в час
    public void checkExpiredSeasons() {
        try {
            seasonService.closeExpiredSeasons();
        } catch (Exception e) {
            log.error("Ошибка при закрытии сезонов", e);
        }
    }

    private final TreasuryService treasuryService;

    // Запускается 1-го числа каждого месяца в 00:00
    @Scheduled(cron = "0 0 0 1 * *")
    public void refillTreasuryMonthly() {
        log.info("Запуск ежемесячного пополнения Казны...");

        Treasury current = treasuryService.getTreasury();

        // Если HR забыл пополнить казну и она пуста (или почти пуста)
        if (current.getBalance().compareTo(new BigDecimal("1000")) < 0) {
            BigDecimal defaultBudget = new BigDecimal("50000.00"); // Лимит по умолчанию
            treasuryService.fundTreasury(defaultBudget);
            log.info("Казна автоматически пополнена на сумму по умолчанию: {}", defaultBudget);
        }
    }

    private final NewsService newsService;
    // Раз в 15 минут проверяем новости с истёкшим сроком и уводим в архив
    @Scheduled(fixedRate = 900_000)
    public void autoArchiveExpiredNews() {
        try {
            int archived = newsService.autoArchiveExpired();
            if (archived > 0) {
                log.info("Автоархив: {} новостей отправлено в архив", archived);
            }
        } catch (Exception e) {
            log.error("Ошибка при автоархивации новостей", e);
        }
    }
}