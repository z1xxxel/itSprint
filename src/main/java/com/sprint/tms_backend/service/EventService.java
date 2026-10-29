package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.ApproveEventRequest;
import com.sprint.tms_backend.DTO.CreateEventRequestDTO;
import com.sprint.tms_backend.DTO.EventResponseDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final RatingTransactionRepository ratingTransactionRepository;

    // ==========================================
    // === СОЗДАНИЕ ===
    // ==========================================

    @Transactional
    public EventResponseDTO createEvent(CreateEventRequestDTO req) {
        if (req.fromTime() == null || req.toTime() == null
                || !req.fromTime().isBefore(req.toTime())) {
            throw new RuntimeException("fromTime должен быть раньше toTime");
        }

        User organizer = userRepository.findById(req.organizerId())
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));

        Event event = new Event();
        event.setOrganizer(organizer);
        event.setFromTime(req.fromTime());
        event.setToTime(req.toTime());
        event.setCategory(req.category());
        event.setNameEvent(req.nameEvent());
        event.setDescription(req.description());
        event.setEventPlace(req.eventPlace());
        event.setCommentToAdmin(req.commentToAdmin());
        event.setEventStatus(EventStatus.NEW);
        event.setRewardsGranted(false);

        // Только HR/ADMIN могут сразу устанавливать награды
        boolean canSetRewards = organizer.getRole().has(Permission.EVENT_APPROVE);
        if (canSetRewards && Boolean.TRUE.equals(req.paidInCoins())) {
            if (req.rewardCoins() == null || req.rewardCoins().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("При начислении монет укажите сумму > 0");
            }
            if (req.rewardRating() != null && req.rewardRating() < 0) {
                throw new RuntimeException("Рейтинг не может быть отрицательным");
            }
            event.setPaidInCoins(true);
            event.setRewardCoins(req.rewardCoins());
            event.setRewardRating(req.rewardRating() != null ? req.rewardRating() : 0.0);
        } else {
            // Обычный пользователь — без наград. Админ решит позже.
            event.setPaidInCoins(false);
            event.setRewardCoins(null);
            event.setRewardRating(null);
        }

        return mapToDto(eventRepository.save(event));
    }

    // ==========================================
    // === ОДОБРЕНИЕ (с установкой наград) ===
    // ==========================================

    @Transactional
    public EventResponseDTO assignEvent(Long eventId, ApproveEventRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        User actor = userRepository.findById(req.actorId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Только администратор или HR может одобрить событие");
        }
        if (event.getEventStatus() != EventStatus.NEW) {
            throw new RuntimeException(
                    "Одобрить можно только из статуса NEW. Текущий: " + event.getEventStatus());
        }

        // Применяем награды, если переданы
        if (req.paidInCoins() != null) {
            if (req.paidInCoins()) {
                if (req.rewardCoins() == null || req.rewardCoins().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("При начислении монет укажите сумму > 0");
                }
                if (req.rewardRating() != null && req.rewardRating() < 0) {
                    throw new RuntimeException("Рейтинг не может быть отрицательным");
                }
                event.setPaidInCoins(true);
                event.setRewardCoins(req.rewardCoins());
                event.setRewardRating(req.rewardRating() != null ? req.rewardRating() : 0.0);
            } else {
                event.setPaidInCoins(false);
                event.setRewardCoins(null);
                event.setRewardRating(null);
            }
        }

        event.setEventStatus(EventStatus.ASSIGNED);
        return mapToDto(eventRepository.save(event));
    }

    // ==========================================
    // === ОТКАТ НАГРАД ===
    // ==========================================

    @Transactional
    public EventResponseDTO rollbackRewards(Long eventId, Long actorId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException("Только администратор или HR может откатить награды");
        }
        if (event.getEventStatus() != EventStatus.COMPLETED) {
            throw new RuntimeException(
                    "Откатить можно только завершённое событие. Текущий: " + event.getEventStatus());
        }
        if (!Boolean.TRUE.equals(event.getRewardsGranted())) {
            throw new RuntimeException("Награды за это событие не выдавались");
        }

        List<EventParticipant> participants = participantRepository.findByEventId(eventId);
        int coinsRolledBack = 0;
        int ratingRolledBack = 0;

        for (EventParticipant p : participants) {
            User user = userRepository.findByIdForUpdate(p.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Забираем монеты
            if (event.getRewardCoins() != null
                    && event.getRewardCoins().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal current = user.getBalance() != null
                        ? user.getBalance() : BigDecimal.ZERO;
                user.setBalance(current.subtract(event.getRewardCoins()));

                CoinTransaction tx = new CoinTransaction();
                tx.setFromUserId(user.getId());
                tx.setToUserId(null);
                tx.setAmount(event.getRewardCoins());
                tx.setType(CoinTransactionType.REFUND);
                tx.setComment("Откат награды за событие #" + eventId
                        + ": " + event.getNameEvent() + ". Причина: " + reason);
                coinTransactionRepository.save(tx);
                coinsRolledBack++;
            }

            // Забираем рейтинг (не ниже 0)
            if (event.getRewardRating() != null && event.getRewardRating() > 0) {
                double current = user.getRating() != null ? user.getRating() : 0.0;
                double newRating = Math.max(0, current - event.getRewardRating());
                user.setRating(newRating);

                RatingTransaction rt = new RatingTransaction();
                rt.setUserId(user.getId());
                rt.setAmount(-event.getRewardRating());
                rt.setReason("Откат рейтинга за событие #" + eventId
                        + ": " + event.getNameEvent() + ". Причина: " + reason);
                ratingTransactionRepository.save(rt);
                ratingRolledBack++;
            }

            userRepository.save(user);
        }

        // Меняем статус и убираем флаг
        event.setEventStatus(EventStatus.CANCELLED);
        event.setRewardsGranted(false);
        String note = "[ОТКАТ НАГРАД] " + reason;
        event.setCommentToAdmin(
                event.getCommentToAdmin() != null && !event.getCommentToAdmin().isEmpty()
                        ? event.getCommentToAdmin() + "\n" + note
                        : note
        );

        log.info("{} откатил награды за событие {} ({} монет, {} рейтинга у {} участников). Причина: {}",
                actor.getName(), eventId, coinsRolledBack, ratingRolledBack,
                participants.size(), reason);

        return mapToDto(eventRepository.save(event));
    }

    // ==========================================
    // === ОТМЕНА / ПРОСМОТР ===
    // ==========================================

    @Transactional
    public EventResponseDTO cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new RuntimeException("Завершённый ивент нельзя отменить");
        }
        if (event.getEventStatus() == EventStatus.CANCELLED) {
            throw new RuntimeException("Ивент уже отменён");
        }
        event.setEventStatus(EventStatus.CANCELLED);
        return mapToDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getAllEvents(Long actorId) {
        User actor = resolveActor(actorId);
        List<Event> events;
        if (actor == null || !actor.getRole().has(Permission.EVENT_VIEW_ANY)) {
            events = eventRepository.findByEventStatusNot(EventStatus.NEW);
        } else {
            events = eventRepository.findAll();
        }
        return events.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEventsByUser(Long organizerId, Long actorId) {
        User actor = resolveActor(actorId);
        boolean canViewAny = actor != null && actor.getRole().has(Permission.EVENT_VIEW_ANY);
        boolean isOwner = actor != null && actor.getId().equals(organizerId);

        List<Event> events;
        if (canViewAny || isOwner) {
            events = eventRepository.findByOrganizerId(organizerId);
        } else {
            events = eventRepository.findByOrganizerIdAndEventStatusNot(
                    organizerId, EventStatus.NEW);
        }
        return events.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getParticipatingEventIds(Long userId) {
        return participantRepository.findByUserId(userId).stream()
                .map(p -> p.getEvent().getId())
                .toList();
    }

    private User resolveActor(Long actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId).orElse(null);
    }

    private EventResponseDTO mapToDto(Event event) {
        return new EventResponseDTO(
                event.getId(),
                event.getOrganizer().getId(),
                event.getOrganizer().getName(),
                event.getFromTime(),
                event.getToTime(),
                event.getCategory(),
                event.getNameEvent(),
                event.getDescription(),
                event.getEventPlace(),
                event.getCommentToAdmin(),
                event.getEventStatus(),
                event.getPaidInCoins(),
                event.getRewardCoins(),
                event.getRewardRating(),
                event.getRewardsGranted()
        );
    }
}