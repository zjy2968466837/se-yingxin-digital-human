# syntax=docker/dockerfile:1
# 多阶段构建：容器内完成 Maven 打包，云服务器只需装 Docker，无需本地 Java/Maven。
# 构建上下文为 backend/ 目录。

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY services/ ./services/
# m2 缓存挂载：多次构建之间复用依赖，显著加速
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/qa-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
# 基础镜像不带 wget/curl，装上 curl 供 compose 健康检查使用
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# 单机跑多个服务，堆内存需控制，避免整机 OOM
ENV JAVA_OPTS="-Xmx256m -Xss512k -XX:+UseSerialGC"
COPY --from=build /build/services/qa-service/target/qa-service-*.jar /app/app.jar
EXPOSE 8101
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
