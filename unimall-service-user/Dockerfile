# unimall-service-user — 用户服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-user"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10012
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
