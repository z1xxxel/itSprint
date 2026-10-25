# ==========================================
# ЭТАП 1: СБОРКА (BUILD)
# ==========================================
# Используем образ с Maven и JDK для сборки
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Сначала копируем только pom.xml, чтобы закешировать зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Затем копируем исходный код и собираем JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ЭТАП 2: ЗАПУСК (RUNTIME)
# ==========================================
# Используем лёгкий образ только с JRE для запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем собранный JAR из первого этапа
# Если у вас Java 17, замените "21" на "17" в обоих FROM
COPY --from=builder /app/target/*.jar app.jar

# Открываем порт, который слушает приложение
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]