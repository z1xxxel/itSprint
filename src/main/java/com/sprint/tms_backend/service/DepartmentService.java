package com.sprint.tms_backend.service;

import com.sprint.tms_backend.DTO.CreateDepartmentRequest;
import com.sprint.tms_backend.DTO.DepartmentDTO;
import com.sprint.tms_backend.model.Department;
import com.sprint.tms_backend.model.User;
import com.sprint.tms_backend.repository.DepartmentRepository;
import com.sprint.tms_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Department createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new RuntimeException("Подразделение с таким названием уже существует");
        }
        Department d = new Department();
        d.setName(request.name());
        d.setDescription(request.description());
        return departmentRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream().map(d -> {
            List<User> members = userRepository.findByDepartmentId(d.getId());
            double totalRating = members.stream()
                    .mapToDouble(u -> u.getRating() != null ? u.getRating() : 0.0)
                    .sum();
            return new DepartmentDTO(d.getId(), d.getName(), d.getDescription(),
                    members.size(), totalRating);
        }).toList();
    }

    // Назначить пользователя в подразделение
    @Transactional
    public void assignUserToDepartment(Long userId, Long departmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Подразделение не найдено"));
        user.setDepartment(dept);
        userRepository.save(user);
    }

    // Убрать пользователя из подразделения
    @Transactional
    public void removeUserFromDepartment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setDepartment(null);
        userRepository.save(user);
    }
}