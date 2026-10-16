package com.sprint.tms_backend.service;

import com.sprint.tms_backend.Exception.InsufficientFundsException;
import com.sprint.tms_backend.Exception.InvalidTransferException;
import com.sprint.tms_backend.Exception.UserNotFoundException;
import com.sprint.tms_backend.model.Transaction;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.TransactionRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transferCoins(Long fromUserId, Long toUserId, BigDecimal amount) {
        // 1. Валидация
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Сумма перевода должна быть положительной");
        }
        if (fromUserId == null || toUserId == null) {
            throw new InvalidTransferException("ID пользователей обязательны");
        }
        if (fromUserId.equals(toUserId)) {
            throw new InvalidTransferException("Нельзя перевести монеты самому себе");
        }

        // 2. Блокировка в детерминированном порядке (по ID)
        User fromUser;
        User toUser;

        if (fromUserId.compareTo(toUserId) < 0) {
            fromUser = lockUser(fromUserId, "Отправитель не найден");
            toUser   = lockUser(toUserId,   "Получатель не найден");
        } else {
            toUser   = lockUser(toUserId,   "Получатель не найден");
            fromUser = lockUser(fromUserId, "Отправитель не найден");
        }

        // 3. Проверка достаточности средств
        if (fromUser.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на балансе");
        }

        // 4. Изменение балансов
        fromUser.setBalance(fromUser.getBalance().subtract(amount));
        toUser.setBalance(toUser.getBalance().add(amount));

        // 5. Сохранение (можно и без явного save, если сущности в managed-состоянии,
        //    но так нагляднее)
        userRepository.save(fromUser);
        userRepository.save(toUser);

        // 6. Запись в историю
        Transaction tx = new Transaction();
        tx.setFromUserId(fromUserId);
        tx.setToUserId(toUserId);
        tx.setAmount(amount);
        transactionRepository.save(tx);
    }

    private User lockUser(Long id, String notFoundMessage) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new UserNotFoundException(notFoundMessage));
    }
}