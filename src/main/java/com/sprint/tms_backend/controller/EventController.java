package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.AssignEventRequest;
import com.sprint.tms_backend.DTO.CreateEventRequestDTO;
import com.sprint.tms_backend.DTO.EventResponseDTO;
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
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody CreateEventRequestDTO request) {
        EventResponseDTO createdEvent = eventService.createEvent(
                request.organizerId(),
                request.fromTime(),
                request.toTime(),
                request.category(),
                request.nameEvent(),
                request.description(),
                request.eventPlace(),
                request.commentToAdmin(),
                request.paidInCoins(),
                request.rewardCoins(),
                request.rewardRating()
        );
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<EventResponseDTO> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<EventResponseDTO> assignEvent(@PathVariable Long id,
                                                        @RequestBody AssignEventRequest request) {
        return ResponseEntity.ok(eventService.assignEvent(id, request.actorId()));
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents(
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(eventService.getAllEvents(actorId));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<EventResponseDTO>> getEventsByOrganizer(
            @PathVariable Long organizerId,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(eventService.getEventsByUser(organizerId, actorId));
    }
}