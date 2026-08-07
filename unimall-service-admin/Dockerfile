# unimall-service-admin — 后台管理服务（Feign 聚合层）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-admin"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10021
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
