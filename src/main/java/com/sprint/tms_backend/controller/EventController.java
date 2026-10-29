package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.ApproveEventRequest;
import com.sprint.tms_backend.DTO.CreateEventRequestDTO;
import com.sprint.tms_backend.DTO.EventResponseDTO;
import com.sprint.tms_backend.DTO.RollbackRequest;
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
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody CreateEventRequestDTO req) {
        return new ResponseEntity<>(eventService.createEvent(req), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<EventResponseDTO> assignEvent(
            @PathVariable Long id,
            @RequestBody ApproveEventRequest req) {
        return ResponseEntity.ok(eventService.assignEvent(id, req));
    }

    @PutMapping("/{id}/rollback-rewards")
    public ResponseEntity<EventResponseDTO> rollbackRewards(
            @PathVariable Long id,
            @RequestBody RollbackRequest req) {
        return ResponseEntity.ok(eventService.rollbackRewards(id, req.actorId(), req.reason()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<EventResponseDTO> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
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

    @GetMapping("/my-participations")
    public ResponseEntity<List<Long>> getMyParticipations(@RequestParam Long userId) {
        return ResponseEntity.ok(eventService.getParticipatingEventIds(userId));
    }
}