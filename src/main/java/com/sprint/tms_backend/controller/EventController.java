package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CreateEventRequestDTO;
import com.sprint.tms_backend.model.Event;
import com.sprint.tms_backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private EventService eventService;

    @PostMapping()
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventRequestDTO request) {
        Event createdEvent = eventService.createEvent(
                request.get
        )
    }
}
