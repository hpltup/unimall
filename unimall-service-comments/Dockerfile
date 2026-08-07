# unimall-service-comments — 评论服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-comments"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10017
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
