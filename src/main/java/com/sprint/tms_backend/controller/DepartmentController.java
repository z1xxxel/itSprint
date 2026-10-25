package com.sprint.tms_backend.controller;

import com.sprint.tms_backend.DTO.CreateDepartmentRequest;
import com.sprint.tms_backend.DTO.DepartmentDTO;
import com.sprint.tms_backend.model.Department;
import com.sprint.tms_backend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<Department> create(@RequestBody CreateDepartmentRequest request) {
        return new ResponseEntity<>(departmentService.createDepartment(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAll() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PutMapping("/{deptId}/users/{userId}")
    public ResponseEntity<String> assignUser(
            @PathVariable Long deptId,
            @PathVariable Long userId) {
        departmentService.assignUserToDepartment(userId, deptId);
        return ResponseEntity.ok("Пользователь добавлен в подразделение");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> removeUser(@PathVariable Long userId) {
        departmentService.removeUserFromDepartment(userId);
        return ResponseEntity.ok("Пользователь убран из подразделения");
    }
}