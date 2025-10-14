# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml và download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build ứng dụng
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023

WORKDIR /app

# Copy JAR từ builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (thay đổi nếu cần)
EXPOSE 8080

# Run ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]