# unimall-service-sendmsg — 消息服务（短信验证码模拟 + 站内信）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-sendmsg"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10019
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
