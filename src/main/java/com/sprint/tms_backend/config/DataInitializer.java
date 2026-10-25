package com.sprint.tms_backend.config;

import com.sprint.tms_backend.model.*;
import com.sprint.tms_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("База данных уже содержит пользователей. Пропускаем инициализацию.");
            return;
        }

        // --- Создание отделов ---
        Department backend = createDepartment("Backend Team", "Серверная разработка");
        Department frontend = createDepartment("Frontend Team", "Клиентская разработка");
        Department qa = createDepartment("QA Team", "Тестирование");

        // --- Создание пользователей ---
        createUser("Сергей Админов", "admin@tms.local", "123", UserRole.ADMIN, 10000, null);
        createUser("Ольга HR", "hr@tms.local", "123", UserRole.HR, 1500, null);
        createUser("Иван Бэкендов", "mgr_back@tms.local", "123", UserRole.MANAGER, 2000, backend);
        createUser("Петр Кодеров", "emp_back@tms.local", "123", UserRole.EMPLOYEE, 500, backend);
        createUser("Мария Верстальщикова", "emp_front@tms.local", "123", UserRole.EMPLOYEE, 300, frontend);
        createUser("Алексей Тестировщиков", "emp_qa@tms.local", "123", UserRole.EMPLOYEE, 150, qa);

        // --- Создание категорий и товаров ---
        Category merch = createCategory("Мерч", "Футболки и кружки");
        Category edu = createCategory("Обучение", "Курсы и книги");
        createProduct("Кружка", 150, 10, merch);
        createProduct("Худи", 1000, 5, merch);
        createProduct("Курс по Java", 5000, 20, edu);

        log.info("✅ Тестовые данные успешно добавлены.");
    }

    private Department createDepartment(String name, String description) {
        Department d = new Department();
        d.setName(name);
        d.setDescription(description);
        return departmentRepository.save(d);
    }

    private User createUser(String name, String email, String password, UserRole role, double balance, Department department) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role);
        u.setBalance(BigDecimal.valueOf(balance));
        u.setRating(0.0);
        u.setDepartment(department);
        return userRepository.save(u);
    }

    private Category createCategory(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        c.setSortOrder(1);
        return categoryRepository.save(c);
    }

    private Product createProduct(String name, double price, int stock, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStock(stock);
        p.setCategory(category);
        p.setType(ProductType.PHYSICAL);
        return productRepository.save(p);
    }
}