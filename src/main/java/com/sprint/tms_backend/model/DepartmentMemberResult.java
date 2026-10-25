package com.sprint.tms_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "department_member_results")
@Data
public class DepartmentMemberResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private Long departmentId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private Integer placeInDepartment;

    @Column(nullable = false)
    private Double finalRating;

    // reward_coins убрали, так как теперь дают ачивки
}