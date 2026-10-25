package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CreateTemplateRequest;
import com.sprint.tms_backend.DTO.TemplateResponseDTO;
import com.sprint.tms_backend.model.SurveyTemplate;
import com.sprint.tms_backend.service.SurveyTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class SurveyTemplateController {

    private final SurveyTemplateService templateService;

    @PostMapping
    public ResponseEntity<SurveyTemplate> createTemplate(@RequestBody CreateTemplateRequest request) {
        return new ResponseEntity<>(templateService.createTemplate(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponseDTO>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponseDTO> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTemplate(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        templateService.deleteTemplate(id, actorId);
        return ResponseEntity.ok("Шаблон удален");
    }
}