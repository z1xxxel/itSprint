package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.ParticipantResponseDTO;
import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventParticipant;
import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.EventParticipantRepository;
import com.sprint.tms_backend.repository.EventRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventParticipantService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository participantRepository;

    @Transactional
    public ParticipantResponseDTO register(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        if (event.getEventStatus() != EventStatus.ASSIGNED) {
            throw new RuntimeException(
                    "Записаться можно только на событие в статусе ASSIGNED. Текущий: "
                            + event.getEventStatus());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (event.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Организатор не может быть участником своего события");
        }

        if (participantRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("Пользователь уже зарегистрирован на это событие");
        }

        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setUser(user);
        EventParticipant saved = participantRepository.save(participant);

        return new ParticipantResponseDTO(
                saved.getUser().getId(),
                saved.getUser().getName(),
                saved.getUser().getEmail()
        );
    }

    @Transactional
    public void unregister(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));

        if (event.getEventStatus() == EventStatus.COMPLETED
                || event.getEventStatus() == EventStatus.CANCELLED) {
            throw new RuntimeException(
                    "Нельзя отписаться: событие в статусе " + event.getEventStatus());
        }

        EventParticipant participant = participantRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Пользователь не зарегистрирован на это событие"));

        participantRepository.delete(participant);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponseDTO> getParticipants(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Ивент не найден");
        }
        return participantRepository.findByEventId(eventId).stream()
                .map(p -> new ParticipantResponseDTO(
                        p.getUser().getId(),
                        p.getUser().getName(),
                        p.getUser().getEmail()))
                .toList();
    }
}