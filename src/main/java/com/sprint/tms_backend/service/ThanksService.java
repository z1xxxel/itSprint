package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.ThanksFeedResponseDTO;
import com.sprint.tms_backend.model.CoinTransaction;
import com.sprint.tms_backend.model.CoinTransactionType;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.CoinTransactionRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}