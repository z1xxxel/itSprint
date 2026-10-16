package com.sprint.tms_backend.service;


import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.EventRepository;
import com.sprint.tms_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EventService {

    private UserRepository userRepository;
    private EventRepository eventRepository;

    public Event createEvent(Long organizerId,
                             LocalDateTime fromTime,
                             LocalDateTime toTime,
                             String category,
                             String nameEvent,
                             String eventPlace,
                             String commentToAdmin,
                             EventStatus eventStatus) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));      Event event = new Event();
        event.setOrganizer(organizer);
        event.setFromTime(fromTime);
        event.setToTime(toTime);
        event.setCategory(category);
        event.setNameEvent(nameEvent);
        event.setEventPlace(eventPlace);
        event.setCommentToAdmin(commentToAdmin);
        event.setEventStatus(eventStatus);
        return eventRepository.save(event);

    }
}
