package com.sprint.tms_backend.service;

import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.model.UserRole;
import com.sprint.tms_backend.repository.EventRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    public Event createEvent(Long organizerId,
                             LocalDateTime fromTime,
                             LocalDateTime toTime,
                             String category,
                             String nameEvent,
                             String description,
                             String eventPlace,
                             String commentToAdmin) {

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

        // Статус — серверная логика
        if (organizer.getRole() == UserRole.Admin) {
            event.setEventStatus(EventStatus.ASSIGNED);
        } else {
            event.setEventStatus(EventStatus.NEW);
        }

        return eventRepository.save(event);
    }

    @Transactional
    public Event cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ивент не найден"));
        if (event.getEventStatus() == EventStatus.COMPLETED) {
            throw new RuntimeException("Это завершенный ивент");
        }
        event.setEventStatus(EventStatus.CANCELLED);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsByUser(Long userId) {
        return eventRepository.findByOrganizerId(userId);
    }
}