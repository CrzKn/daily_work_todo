# ===== 后端镜像：Maven 构建 + JRE 运行（多阶段） =====
# 构建阶段：用 Maven + JDK 17 打出可执行 jar
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 配置阿里云 Maven 镜像，加速国内依赖下载
COPY docker/maven/settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# 运行阶段：仅含 JRE，体积更小
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/target/back-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 38080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
