package com.sprint.tms_backend.model;

public enum CoinTransactionType {
    PEER_THANKS,       // сотрудник → сотруднику
    EVENT_REWARD,      // система → участнику за событие
    MANAGER_BONUS,     // MANAGER → подчинённому
    PURCHASE,          // сотрудник → система (списание в каталоге)
    ACHIEVEMENT,       // система → сотруднику за ачивку
    ADMIN_ADJUSTMENT,   // ADMIN → сотруднику (ручная корректировка)
    REFUND // Возврат монет за отклоненный заказ
}