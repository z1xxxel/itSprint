package com.sprint.tms_backend.DTO;

import com.sprint.tms_backend.model.EventStatus;
import com.sprint.tms_backend.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

public class CreateEventRequestDTO {

    private Long id;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private String category;
    private String nameEvent;
    private User organizator;
    private String description;
    private String eventPlace;
    private String commentToAdmin;
    private EventStatus eventStatus;
}
