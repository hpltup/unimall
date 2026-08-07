# unimall-service-ai — AI 客服服务（需 DEEPSEEK_API_KEY 环境变量）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-ai"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10022
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
