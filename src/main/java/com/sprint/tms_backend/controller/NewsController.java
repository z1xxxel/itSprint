package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<List<NewsDto>> getAll(@RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(newsService.getAll(actorId));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<NewsDto>> getArchive(@RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(newsService.getArchive(actorId));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<NewsDto>> getLatest(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(newsService.getLatest(limit, actorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(newsService.getById(id, actorId));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<NewsDto> view(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(newsService.incrementView(id, actorId));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<NewsDto> rate(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody RateNewsRequest request) {
        return ResponseEntity.ok(newsService.rate(id, actorId, request.value()));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<NewsDto> archive(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(newsService.archive(id, actorId));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<NewsDto> restore(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        return ResponseEntity.ok(newsService.restore(id, actorId));
    }

    @PostMapping
    public ResponseEntity<NewsDto> create(
            @RequestParam Long actorId,
            @RequestBody CreateNewsRequest request) {
        return new ResponseEntity<>(newsService.create(actorId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsDto> update(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestBody UpdateNewsRequest request) {
        return ResponseEntity.ok(newsService.update(id, actorId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestParam Long actorId) {
        newsService.delete(id, actorId);
        return ResponseEntity.ok("Новость удалена");
    }
}