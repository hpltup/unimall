# unimall-service-search — 搜索服务（Elasticsearch，虚拟机上 192.168.89.101:9200）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-search"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10020
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
