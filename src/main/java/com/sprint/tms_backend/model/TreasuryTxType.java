package com.sprint.tms_backend.model;

public enum TreasuryTxType {
    FUND("Пополнение", true),
    WITHDRAW("Списание", false),
    REWARD_PAYOUT("Выплата награды", false),
    QUEST_PAYOUT("Выплата за квест", false),
    EVENT_PAYOUT("Выплата за событие", false),
    SEASON_PAYOUT("Выплата за сезон", false),
    PENALTY_RETURN("Возврат штрафа", true),
    QUEST_CANCEL_RETURN("Возврат за отмену квеста", true),
    TRANSACTION_CANCEL_RETURN("Возврат отменённой транзакции", true),
    OTHER("Другое", true);

    private final String label;
    private final boolean incoming;

    TreasuryTxType(String label, boolean incoming) {
        this.label = label;
        this.incoming = incoming;
    }

    public String getLabel() { return label; }
    public boolean isIncoming() { return incoming; }
}