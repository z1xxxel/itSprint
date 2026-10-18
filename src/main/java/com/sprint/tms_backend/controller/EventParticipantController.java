package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.ParticipantResponseDTO;
import com.sprint.tms_backend.DTO.RegisterParticipantRequest;
import com.sprint.tms_backend.service.EventParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/participants")
@RequiredArgsConstructor
public class EventParticipantController {

    private final EventParticipantService participantService;

    @PostMapping
    public ResponseEntity<ParticipantResponseDTO> register(
            @PathVariable Long eventId,
            @RequestBody RegisterParticipantRequest request) {
        ParticipantResponseDTO response = participantService.register(eventId, request.userId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unregister(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        participantService.unregister(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ParticipantResponseDTO>> getParticipants(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(participantService.getParticipants(eventId));
    }
}