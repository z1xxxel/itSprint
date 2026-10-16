package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CreateEventRequestDTO;
import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventRequestDTO request) {
        Event createdEvent = eventService.createEvent(
                request.organizerId(),
                request.fromTime(),
                request.toTime(),
                request.category(),
                request.nameEvent(),
                request.description(),
                request.eventPlace(),
                request.commentToAdmin()
        );
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Event> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<Event>> getEventsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.getEventsByUser(organizerId));
    }
}