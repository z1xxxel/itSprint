package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_settings")
@Data
public class SystemSetting {
    @Id
    private String settingKey; // Например, "limit.peer.daily"

    @Column(nullable = false)
    private String settingValue;
}