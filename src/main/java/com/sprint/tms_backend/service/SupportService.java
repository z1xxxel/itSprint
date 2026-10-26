package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.*;
import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository; // <-- ВОТ ЭТОГО ПОЛЯ НЕ ХВАТАЛО
    private final UserRepository userRepository;
    private final RatingTransactionRepository ratingTxRepository;
    private final AchievementService achievementService; // Для выдачи ачивок из поддержки

    // === 1. Пользователь создает тикет ===
    @Transactional
    public SupportTicketDTO createTicket(CreateTicketRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        SupportTicket ticket = new SupportTicket();
        ticket.setTopic(request.topic());
        ticket.setDescription(request.description());
        ticket.setCreator(user);
        ticket.setStatus(TicketStatus.PENDING);

        return mapToTicketDto(ticketRepository.save(ticket));
    }

    // === 2. Сотрудник поддержки берет тикет в работу ===
    @Transactional
    public SupportTicketDTO takeTicket(Long ticketId, Long supportId) {
        User support = userRepository.findById(supportId)
                .orElseThrow(() -> new RuntimeException("Сотрудник поддержки не найден"));

        if (!support.getRole().has(Permission.TICKET_MANAGE)) {
            throw new SecurityException("Нет прав на управление тикетами");
        }

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        ticket.setAssignee(support);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return mapToTicketDto(ticketRepository.save(ticket));
    }

    // === 3. Отправка сообщения в чат (с картинкой или без) ===
    @Transactional
    public TicketMessageDTO sendMessage(Long ticketId, SendMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        if (ticket.getStatus() == TicketStatus.COMPLETED) {
            throw new RuntimeException("Тикет уже завершен, переписка закрыта");
        }

        User sender = userRepository.findById(request.senderId())
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));

        boolean isCreator = ticket.getCreator().getId().equals(sender.getId());
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(sender.getId());

        if (!isCreator && !isAssignee) {
            throw new SecurityException("Вы не участник этого тикета");
        }

        TicketMessage msg = new TicketMessage();
        msg.setTicket(ticket);
        msg.setSender(sender);
        msg.setText(request.text());
        msg.setAttachmentUrl(request.attachmentUrl()); // <-- ССЫЛКА НА ФОТО

        return mapToMessageDto(messageRepository.save(msg)); // <-- ТЕПЕРЬ РАБОТАЕТ
    }

    // === 4. Завершение тикета ===
    @Transactional
    public SupportTicketDTO completeTicket(Long ticketId, Long supportId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(supportId)) {
            throw new SecurityException("Только назначенный сотрудник может завершить тикет");
        }

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setClosedAt(LocalDateTime.now());
        return mapToTicketDto(ticketRepository.save(ticket));
    }

    // === 5. Оставление отзыва ===
    @Transactional
    public SupportTicketDTO leaveReview(Long ticketId, LeaveReviewRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        if (!ticket.getCreator().getId().equals(request.userId())) {
            throw new SecurityException("Только автор тикета может оставить отзыв");
        }
        if (ticket.getStatus() != TicketStatus.COMPLETED) {
            throw new RuntimeException("Можно оценить только завершенный тикет");
        }
        if (request.rating() < 1 || request.rating() > 5) {
            throw new RuntimeException("Оценка должна быть от 1 до 5");
        }

        ticket.setRating(request.rating());
        ticket.setReviewComment(request.comment());
        ticketRepository.save(ticket);

        User support = ticket.getAssignee();
        if (support != null) {
            double ratingBonus = request.rating() >= 4 ? (request.rating() - 3) : 0.0;

            if (ratingBonus > 0) {
                double current = support.getRating() != null ? support.getRating() : 0.0;
                support.setRating(current + ratingBonus);
                userRepository.save(support);

                RatingTransaction rt = new RatingTransaction();
                rt.setUserId(support.getId());
                rt.setAmount(ratingBonus);
                rt.setReason("Отличный отзыв за тикет #" + ticket.getId());
                ratingTxRepository.save(rt);
            }
        }

        return mapToTicketDto(ticket);
    }

    // === 6. Получение чата ===
    @Transactional(readOnly = true)
    public TicketChatResponseDTO getTicketChat(Long ticketId, Long actorId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        User actor = userRepository.findById(actorId).orElseThrow();
        boolean isCreator = ticket.getCreator().getId().equals(actorId);
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actorId);
        boolean isSupport = actor.getRole().has(Permission.TICKET_MANAGE);

        if (!isCreator && !isAssignee && !isSupport) {
            throw new SecurityException("Нет доступа к этому тикету");
        }

        List<TicketChatResponseDTO.MessageDTO> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(m -> new TicketChatResponseDTO.MessageDTO(
                        m.getId(),
                        m.getSender().getName(),
                        m.getText(),
                        m.getAttachmentUrl(), // <-- ДОБАВЛЯЕМ
                        m.getCreatedAt()
                )).toList();

        return new TicketChatResponseDTO(
                ticket.getId(), ticket.getTopic(), ticket.getDescription(),
                ticket.getCreator().getName(),
                ticket.getAssignee() != null ? ticket.getAssignee().getName() : null,
                ticket.getStatus(), ticket.getCreatedAt(), messages
        );
    }

    // === 7. ВЫДАЧА АЧИВКИ ИЗ ТИКЕТА (Только для Support) ===
    @Transactional
    public void grantAchievementFromTicket(Long ticketId, Long supportId, String achievementCode) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Тикет не найден"));

        if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(supportId)) {
            throw new SecurityException("Только назначенный сотрудник поддержки может выдать награду по этому тикету");
        }

        // Выдаем ачивку пользователю, который создал тикет
        achievementService.grantAchievementByCode(ticket.getCreator().getId(), achievementCode);
    }

    // === 8. Получение списков тикетов ===
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getMyTickets(Long userId) {
        return ticketRepository.findByCreatorIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToTicketDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getPendingTickets() {
        return ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.PENDING).stream()
                .map(this::mapToTicketDto).toList();
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ МАППИНГА ===
    private SupportTicketDTO mapToTicketDto(SupportTicket ticket) {
        return new SupportTicketDTO(
                ticket.getId(), ticket.getTopic(), ticket.getDescription(),
                ticket.getCreator().getId(), ticket.getCreator().getName(),
                ticket.getAssignee() != null ? ticket.getAssignee().getId() : null,
                ticket.getAssignee() != null ? ticket.getAssignee().getName() : null,
                ticket.getStatus(), ticket.getCreatedAt(), ticket.getClosedAt(),
                ticket.getRating(), ticket.getReviewComment()
        );
    }

    private TicketMessageDTO mapToMessageDto(TicketMessage msg) {
        return new TicketMessageDTO(
                msg.getId(), msg.getTicket().getId(), msg.getSender().getId(),
                msg.getSender().getName(), msg.getText(), msg.getCreatedAt()
        );
    }
}