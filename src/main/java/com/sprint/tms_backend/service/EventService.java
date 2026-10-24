package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.EventResponseDTO;
import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.Permission;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.EventRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    public EventResponseDTO createEvent(Long organizerId,
                                        LocalDateTime fromTime,
                                        LocalDateTime toTime,
                                        String category,
                                        String nameEvent,
                                        String description,
                                        String eventPlace,
                                        String commentToAdmin,
                                        Boolean paidInCoins,
                                        BigDecimal rewardCoins,
                                        Double rewardRating) {

        if (fromTime == null || toTime == null || !fromTime.isBefore(toTime)) {
            throw new RuntimeException("fromTime должен быть раньше toTime");
        }

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));

        Event event = new Event();
        event.setOrganizer(organizer);
        event.setFromTime(fromTime);
        event.setToTime(toTime);
        event.setCategory(category);
        event.setNameEvent(nameEvent);
        event.setDescription(description);
        event.setEventPlace(eventPlace);
        event.setCommentToAdmin(commentToAdmin);
        event.setEventStatus(EventStatus.NEW);
        event.setRewardsGranted(false);

        boolean paid = Boolean.TRUE.equals(paidInCoins);
        if (paid) {
            if (rewardCoins == null || rewardCoins.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("При оплате монетами укажите rewardCoins > 0");
            }
            if (rewardRating != null && rewardRating < 0) {
                throw new RuntimeException("Рейтинг не может быть отрицательным");
            }
            event.setPaidInCoins(true);
            event.setRewardCoins(rewardCoins);
            event.setRewardRating(rewardRating != null ? rewardRating : 0.0);
        } else {
            event.setPaidInCoins(false);
            event.setRewardCoins(null);
            event.setRewardRating(null);
        }

        return toDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDTO cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new RuntimeException("Это завершенный ивент");
        }
        if (event.getEventStatus() == EventStatus.CANCELLED) {
            throw new RuntimeException("Ивент уже отменён");
        }
        event.setEventStatus(EventStatus.CANCELLED);
        return toDto(eventRepository.save(event));
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
        return events.stream().map(this::toDto).toList();
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
            events = eventRepository.findByOrganizerIdAndEventStatusNot(organizerId, EventStatus.NEW);
        }
        return events.stream().map(this::toDto).toList();
    }

    @Transactional
    public EventResponseDTO assignEvent(Long eventId, Long actorId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!actor.getRole().has(Permission.EVENT_APPROVE)) {
            throw new SecurityException(
                    "Только пользователь с правом EVENT_APPROVE может присвоить ASSIGNED");
        }
        if (event.getEventStatus() != EventStatus.NEW) {
            throw new RuntimeException(
                    "Присвоить ASSIGNED можно только из NEW. Текущий статус: "
                            + event.getEventStatus());
        }

        event.setEventStatus(EventStatus.ASSIGNED);
        return toDto(eventRepository.save(event));
    }

    private User resolveActor(Long actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId).orElse(null);
    }

    // --- Маппинг Entity -> DTO ---
    private EventResponseDTO toDto(Event event) {
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