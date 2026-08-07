# unimall-gateway — 网关（统一入口，对外 10011）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-gateway"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10011
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
