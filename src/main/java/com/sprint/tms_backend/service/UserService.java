package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.AchievementResponseDTO;
import com.sprint.tms_backend.DTO.CoinTransactionDTO;
import com.sprint.tms_backend.DTO.UserProfileResponseDTO;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTxRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final AchievementService achievementService; // <--- ДОБАВЛЕНО

    @Transactional
    public User createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email существует");
        }
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        if (user.getRating() == null) {
            user.setRating(0.0);
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 1. Считаем уровень
        double rating = user.getRating() != null ? user.getRating() : 0.0;
        String level;
        if (rating < 10) level = "Junior";
        else if (rating < 50) level = "Middle";
        else if (rating < 100) level = "Senior";
        else level = "Legend";

        // 2. Последние 5 транзакций
        List<CoinTransactionDTO> recentTx = coinTxRepository
                .findHistoryByUser(userId, PageRequest.of(0, 5))
                .map(t -> new CoinTransactionDTO(
                        t.getId(),
                        t.getFromUserId(),
                        t.getToUserId(),
                        t.getAmount(),
                        t.getType(),
                        t.getComment(),
                        t.getTag(),
                        t.getCreatedAt()
                )).getContent();

        // 3. Последние 5 заказов
        List<UserProfileResponseDTO.OrderShortDTO> recentOrders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .map(o -> new UserProfileResponseDTO.OrderShortDTO(
                        o.getId(),
                        o.getProduct().getName(),
                        o.getQuantity(),
                        o.getTotalPrice(),
                        o.getStatus(),
                        o.getAdminComment()
                )).getContent();

        // 4. Статистика
        long organizedEvents = eventRepository.countByOrganizerId(userId);
        long participatedEvents = participantRepository.countByUserId(userId);

        // 5. Ачивки
        List<AchievementResponseDTO> achievements = achievementService.getUserAchievements(userId);

        // 6. Сборка DTO
        return new UserProfileResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getRating(),
                level,
                organizedEvents,
                participatedEvents,
                recentTx,
                recentOrders,
                achievements // <--- ПЕРЕДАЕМ АЧИВКИ
        );
    }
}