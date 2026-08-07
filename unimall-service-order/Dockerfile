# unimall-service-order — 订单服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-order"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10015
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
