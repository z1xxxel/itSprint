package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.ThanksFeedResponseDTO;
import com.sprint.tms_backend.DTO.ThanksStatsDTO;
import com.sprint.tms_backend.DTO.TopThankedDTO;
import com.sprint.tms_backend.DTO.UserThanksDTO;
import com.sprint.tms_backend.model.CoinTransaction;
import com.sprint.tms_backend.model.CoinTransactionType;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThanksService {

    private final CoinTransactionRepository coinTxRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ThanksFeedResponseDTO> getFeed() {
        // 1. Получаем последние 50 переводов типа PEER_THANKS
        List<CoinTransaction> txs = coinTxRepository.findByTypeOrderByCreatedAtDesc(
                CoinTransactionType.PEER_THANKS, PageRequest.of(0, 50)
        ).getContent();

        if (txs.isEmpty()) return List.of();

        // 2. Собираем ID всех пользователей (отправителей и получателей)
        Set<Long> userIds = new HashSet<>();
        for (CoinTransaction tx : txs) {
            if (tx.getFromUserId() != null) userIds.add(tx.getFromUserId());
            if (tx.getToUserId() != null) userIds.add(tx.getToUserId());
        }

        // 3. Одним запросом достаем их имена (защита от N+1)
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        // 4. Собираем красивый DTO
        // В методе getFeed() при сборке DTO:
        return txs.stream().map(tx -> new ThanksFeedResponseDTO(
                tx.getId(),
                userNames.getOrDefault(tx.getFromUserId(), "Неизвестный"),
                userNames.getOrDefault(tx.getToUserId(), "Неизвестный"),
                tx.getAmount(),
                tx.getTag(), // <-- Передаем enum
                tx.getComment(),
                tx.getCreatedAt()
        )).toList();
    }
    // Получить благодарности пользователя (received / sent / all)
    @Transactional(readOnly = true)
    public Page<UserThanksDTO> getUserThanks(Long userId, String direction, int page, int size) {
        // Проверяем, что пользователь существует
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Пользователь не найден");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<CoinTransaction> txs;

        if ("received".equalsIgnoreCase(direction)) {
            txs = coinTxRepository.findReceivedThanks(userId, pageable);
        } else if ("sent".equalsIgnoreCase(direction)) {
            txs = coinTxRepository.findSentThanks(userId, pageable);
        } else {
            txs = coinTxRepository.findHistoryByUser(userId, pageable)
                    .map(t -> t) // чтобы тип совпал
                    .map(t -> t.getType() == CoinTransactionType.PEER_THANKS ? t : null);
            // Проще: сделать findAllThanksByUser в репозитории
        }

        // Собираем имена всех участников одним запросом
        Set<Long> ids = new HashSet<>();
        txs.getContent().forEach(t -> {
            if (t.getFromUserId() != null) ids.add(t.getFromUserId());
            if (t.getToUserId() != null) ids.add(t.getToUserId());
        });
        Map<Long, String> names = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return txs.map(t -> new UserThanksDTO(
                t.getId(),
                t.getFromUserId(), names.getOrDefault(t.getFromUserId(), "—"),
                t.getToUserId(),   names.getOrDefault(t.getToUserId(), "—"),
                t.getAmount(), t.getTag(), t.getComment(), t.getCreatedAt()
        ));
    }

    // Статистика по благодарностям
    @Transactional(readOnly = true)
    public ThanksStatsDTO getStats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Пользователь не найден");
        }

        long received = coinTxRepository.countReceived(userId);
        long sent = coinTxRepository.countSent(userId);
        BigDecimal sumRecv = coinTxRepository.sumReceived(userId);
        BigDecimal sumSent = coinTxRepository.sumSent(userId);

        // Топ-5 благодарителей
        List<Object[]> topRaw = coinTxRepository.findTopThankers(userId, PageRequest.of(0, 5));
        Set<Long> ids = topRaw.stream().map(o -> (Long) o[0]).collect(Collectors.toSet());
        Map<Long, String> names = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<ThanksStatsDTO.TopThanker> top = topRaw.stream()
                .map(o -> new ThanksStatsDTO.TopThanker(
                        names.getOrDefault((Long) o[0], "—"),
                        ((Number) o[1]).longValue()))
                .toList();

        return new ThanksStatsDTO(received, sent, sumRecv, sumSent, top);
    }

    @Transactional(readOnly = true)
    public List<TopThankedDTO> getTopWeek(int limit) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        List<CoinTransaction> txs = coinTxRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, 500)).getContent().stream()
                .filter(t -> t.getType() == CoinTransactionType.PEER_THANKS)
                .filter(t -> t.getCreatedAt().isAfter(weekAgo))
                .filter(t -> t.getToUserId() != null)
                .toList();

        Map<Long, Long> counts = txs.stream()
                .collect(Collectors.groupingBy(
                        CoinTransaction::getToUserId,
                        Collectors.counting()
                ));

        List<Long> topIds = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, User> users = userRepository.findAllById(topIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return topIds.stream()
                .map(id -> {
                    User u = users.get(id);
                    return new TopThankedDTO(
                            id,
                            u != null ? u.getName() : "—",
                            u != null ? u.getAvatarUrl() : null,
                            counts.get(id)
                    );
                })
                .toList();
    }
}