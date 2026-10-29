package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.CreateNewsRequest;
import com.sprint.tms_backend.DTO.NewsDto;
import com.sprint.tms_backend.DTO.UpdateNewsRequest;
import com.sprint.tms_backend.model.News;
import com.sprint.tms_backend.model.NewsRating;
import com.sprint.tms_backend.model.Permission;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.NewsRatingRepository;
import com.sprint.tms_backend.repository.NewsRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsRatingRepository newsRatingRepository;
    private final UserRepository userRepository;

    // ==========================================
    // === ЧТЕНИЕ ===
    // ==========================================

    @Transactional(readOnly = true)
    public List<NewsDto> getAll(Long actorId) {
        List<News> all = newsRepository.findByIsArchivedFalseOrderByIsPinnedDescCreatedAtDesc();
        if (all.isEmpty()) return List.of();
        Map<Long, RatingStats> statsMap = buildStats(all.stream().map(News::getId).toList());
        return all.stream()
                .map(n -> toDto(n, statsMap.get(n.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsDto> getArchive(Long actorId) {
        List<News> all = newsRepository.findByIsArchivedTrueOrderByArchivedAtDesc();
        if (all.isEmpty()) return List.of();
        Map<Long, RatingStats> statsMap = buildStats(all.stream().map(News::getId).toList());
        return all.stream()
                .map(n -> toDto(n, statsMap.get(n.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsDto> getLatest(int limit, Long actorId) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<News> all = newsRepository.findByIsArchivedFalseOrderByIsPinnedDescCreatedAtDesc();
        List<News> slice = all.stream().limit(safeLimit).toList();
        if (slice.isEmpty()) return List.of();
        Map<Long, RatingStats> statsMap = buildStats(slice.stream().map(News::getId).toList());
        return slice.stream()
                .map(n -> toDto(n, statsMap.get(n.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public NewsDto getById(Long id, Long actorId) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));
        return buildDetail(news, actorId);
    }

    @Transactional
    public NewsDto incrementView(Long id, Long actorId) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));
        news.setViews((news.getViews() == null ? 0 : news.getViews()) + 1);
        newsRepository.save(news);
        return buildDetail(news, actorId);
    }

    // ==========================================
    // === ОЦЕНКА ===
    // ==========================================

    @Transactional
    public NewsDto rate(Long newsId, Long userId, Integer value) {
        if (value == null || value < 1 || value > 5) {
            throw new RuntimeException("Оценка должна быть от 1 до 5");
        }
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        NewsRating rating = newsRatingRepository.findByNewsIdAndUserId(newsId, userId)
                .orElseGet(() -> {
                    NewsRating r = new NewsRating();
                    r.setNewsId(newsId);
                    r.setUserId(userId);
                    return r;
                });
        rating.setValue(value);
        newsRatingRepository.save(rating);
        return buildDetail(news, userId);
    }

    // ==========================================
    // === АРХИВ ===
    // ==========================================

    @Transactional
    public NewsDto archive(Long id, Long actorId) {
        User actor = checkRights(actorId);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));

        if (Boolean.TRUE.equals(news.getIsArchived())) {
            throw new RuntimeException("Новость уже в архиве");
        }
        news.setIsArchived(true);
        news.setArchivedAt(LocalDateTime.now());
        News saved = newsRepository.save(news);
        log.info("{} отправил новость #{} в архив вручную", actor.getEmail(), id);
        return buildDetail(saved, actorId);
    }

    @Transactional
    public NewsDto restore(Long id, Long actorId) {
        User actor = checkRights(actorId);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));

        if (!Boolean.TRUE.equals(news.getIsArchived())) {
            throw new RuntimeException("Новость не в архиве");
        }
        news.setIsArchived(false);
        news.setArchivedAt(null);
        // Если у новости истёк срок — восстановление его сбросит (иначе снова уйдёт в архив)
        if (news.getExpiresAt() != null && news.getExpiresAt().isBefore(LocalDateTime.now())) {
            news.setExpiresAt(null);
        }
        News saved = newsRepository.save(news);
        log.info("{} восстановил новость #{} из архива", actor.getEmail(), id);
        return buildDetail(saved, actorId);
    }

    /** Планировщик: автоархивирует всё, у чего истёк срок */
    @Transactional
    public int autoArchiveExpired() {
        List<Long> ids = newsRepository.findIdsToArchive(LocalDateTime.now());
        if (ids.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            newsRepository.findById(id).ifPresent(n -> {
                n.setIsArchived(true);
                n.setArchivedAt(now);
                newsRepository.save(n);
                log.info("Новость #{} автоматически отправлена в архив (истёк срок)", id);
            });
        }
        return ids.size();
    }

    // ==========================================
    // === CRUD ===
    // ==========================================

    @Transactional
    public NewsDto create(Long actorId, CreateNewsRequest req) {
        User actor = checkRights(actorId);
        validate(req.title(), req.content());

        News news = new News();
        news.setTitle(req.title().trim());
        news.setContent(req.content().trim());
        news.setCoverUrl(trimOrNull(req.coverUrl()));
        news.setIsPinned(Boolean.TRUE.equals(req.isPinned()));
        news.setExpiresAt(req.expiresAt());
        news.setIsArchived(false);
        news.setViews(0);
        news.setAuthor(actor);

        News saved = newsRepository.save(news);
        return buildDetail(saved, actorId);
    }

    @Transactional
    public NewsDto update(Long id, Long actorId, UpdateNewsRequest req) {
        User actor = checkRights(actorId);
        validate(req.title(), req.content());

        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));

        news.setTitle(req.title().trim());
        news.setContent(req.content().trim());
        news.setCoverUrl(trimOrNull(req.coverUrl()));
        news.setIsPinned(Boolean.TRUE.equals(req.isPinned()));
        news.setExpiresAt(req.expiresAt());

        // Если новость была в архиве, но срок передвинули в будущее — вернуть в активные
        if (Boolean.TRUE.equals(news.getIsArchived())
                && req.expiresAt() != null
                && req.expiresAt().isAfter(LocalDateTime.now())) {
            news.setIsArchived(false);
            news.setArchivedAt(null);
        }

        return buildDetail(newsRepository.save(news), actorId);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        User actor = checkRights(actorId);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Новость не найдена"));
        newsRepository.delete(news);
        log.info("{} удалил новость #{}", actor.getEmail(), id);
    }

    // ==========================================
    // === ХЕЛПЕРЫ ===
    // ==========================================

    private record RatingStats(double avg, long count) {}

    private Map<Long, RatingStats> buildStats(List<Long> ids) {
        Map<Long, RatingStats> map = new HashMap<>();
        for (Object[] row : newsRatingRepository.findStatsByNewsIds(ids)) {
            Long nid = (Long) row[0];
            double avg = ((Number) row[1]).doubleValue();
            long cnt = ((Number) row[2]).longValue();
            map.put(nid, new RatingStats(Math.round(avg * 10.0) / 10.0, cnt));
        }
        return map;
    }

    private NewsDto buildDetail(News n, Long actorId) {
        Double avgRaw = newsRatingRepository.findAverageByNewsId(n.getId());
        long count = newsRatingRepository.countByNewsId(n.getId());
        Double avg = (avgRaw != null && count > 0) ? Math.round(avgRaw * 10.0) / 10.0 : null;

        Integer myRating = null;
        if (actorId != null) {
            myRating = newsRatingRepository.findByNewsIdAndUserId(n.getId(), actorId)
                    .map(NewsRating::getValue).orElse(null);
        }

        RatingStats stats = new RatingStats(avg != null ? avg : 0, count);
        return toDto(n, stats, myRating);
    }

    private NewsDto toDto(News n, RatingStats stats, Integer myRating) {
        Double avg = (stats != null && stats.count() > 0) ? stats.avg() : null;
        long count = stats != null ? stats.count() : 0;
        return new NewsDto(
                n.getId(),
                n.getTitle(),
                n.getContent(),
                n.getCoverUrl(),
                n.getIsPinned(),
                n.getAuthor().getId(),
                n.getAuthor().getName(),
                n.getViews() == null ? 0 : n.getViews(),
                avg,
                count,
                myRating,
                n.getExpiresAt(),
                n.getIsArchived(),
                n.getArchivedAt(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }

    private User checkRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (!actor.getRole().has(Permission.NEWS_MANAGE)) {
            throw new SecurityException("Нет прав на управление новостями");
        }
        return actor;
    }

    private void validate(String title, String content) {
        if (title == null || title.trim().isEmpty()) {
            throw new RuntimeException("Заголовок обязателен");
        }
        if (title.length() > 200) {
            throw new RuntimeException("Заголовок не длиннее 200 символов");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Текст новости обязателен");
        }
        if (content.length() > 5000) {
            throw new RuntimeException("Текст не длиннее 5000 символов");
        }
    }

    private String trimOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }
}