# unimall-service-upload — 文件上传服务（本地磁盘存储，需挂载 volume）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-service-upload"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10018
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
