# unimall-service-seckill — 秒杀服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-seckill"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10016
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
