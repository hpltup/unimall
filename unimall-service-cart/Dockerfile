# unimall-service-cart — 购物车服务
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-cart"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10014
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
