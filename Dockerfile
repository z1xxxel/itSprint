# Этап 1: Сборка
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Создаем папку для загрузок
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]