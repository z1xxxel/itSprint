package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.ThanksFeedResponseDTO;
import com.sprint.tms_backend.service.ThanksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/thanks")
@RequiredArgsConstructor
public class ThanksController {

    private final ThanksService thanksService;

    @GetMapping("/feed")
    public ResponseEntity<List<ThanksFeedResponseDTO>> getFeed() {
        return ResponseEntity.ok(thanksService.getFeed());
    }
}