# unimall-config — 配置中心（git 模式，从 gitee 拉配置）
# 前置：mvn package 生成 target/*.jar（不在容器内编译，依赖已缓存本地仓库）
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="unimall-config"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 10010
ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
