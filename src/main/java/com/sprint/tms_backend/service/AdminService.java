package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.AdminDashboardDTO;
import com.sprint.tms_backend.DTO.AdminQuestDTO;
import com.sprint.tms_backend.DTO.SupportTicketDTO;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final SupportTicketRepository ticketRepository;
    private final UserQuestRepository userQuestRepository;
    private final SystemSettingRepository settingRepository;

    // ==========================================
    // 1. АНАЛИТИКА (ДАШБОРД)
    // ==========================================

    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboard(Long actorId) {
        checkAdminRights(actorId);

        BigDecimal totalCoins = userRepository.sumAllBalances();
        if (totalCoins == null) totalCoins = BigDecimal.ZERO;

        return new AdminDashboardDTO(
                userRepository.count(),
                productRepository.count(),
                totalCoins,
                orderRepository.countByStatus(OrderStatus.PENDING),
                eventRepository.countByEventStatus(EventStatus.NEW),
                ticketRepository.countByStatus(TicketStatus.PENDING),
                userQuestRepository.countByStatus(QuestProgressStatus.PENDING_REVIEW)
        );
    }

    // ==========================================
    // 2. УПРАВЛЕНИЕ НАСТРОЙКАМИ (ЭКОНОМИКА)
    // ==========================================

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings(Long actorId) {
        checkAdminRights(actorId);
        return settingRepository.findAll();
    }

    @Transactional
    public SystemSetting updateSetting(String key, String value, Long actorId) {
        checkAdminRights(actorId);

        SystemSetting setting = settingRepository.findById(key)
                .orElse(new SystemSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(value);

        return settingRepository.save(setting);
    }

    // ==========================================
    // 3. ГЛОБАЛЬНЫЙ ПРОСМОТР (МОДЕРАЦИЯ)
    // ==========================================

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets(Long actorId) {
        checkAdminRights(actorId);

        return ticketRepository.findAll().stream()
                .map(t -> new SupportTicketDTO(
                        t.getId(),
                        t.getTopic(),
                        t.getDescription(),
                        t.getCreator().getId(),
                        t.getCreator().getName(),
                        t.getAssignee() != null ? t.getAssignee().getId() : null,
                        t.getAssignee() != null ? t.getAssignee().getName() : null,
                        t.getStatus(),
                        t.getCreatedAt(),
                        t.getClosedAt(),
                        t.getRating(),
                        t.getReviewComment()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminQuestDTO> getAllQuests(Long actorId) {
        checkAdminRights(actorId);

        return userQuestRepository.findAll().stream()
                .map(uq -> new AdminQuestDTO(
                        uq.getId(),
                        uq.getQuest().getTitle(),
                        uq.getUser().getName(),
                        uq.getStatus(),
                        uq.getStartedAt(),
                        uq.getSubmittedAt(),
                        uq.getCompletedAt(),
                        uq.getAdminComment()
                )).toList();
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================

    private void checkAdminRights(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Доступ разрешен только ADMIN или HR
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.HR) {
            throw new SecurityException("Нет доступа к административной панели");
        }
    }
}