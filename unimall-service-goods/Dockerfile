# unimall-service-goods — 商品服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-goods"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10013
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
