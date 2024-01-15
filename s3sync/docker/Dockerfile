ARG OPEN_JDK=docker-registry-ops-dev.lab.zjvis.net:32443/docker.io/library/openjdk:11-oraclelinux8
FROM ${OPEN_JDK}
ENV TZ=Asia/Shanghai
ARG JAR_NAME=s3-sync-1.0.0.jar
COPY --chmod=600 ${JAR_NAME} /app/application.jar
COPY --chmod=755 entry-point.sh /app/entry-point.sh
WORKDIR  /app
CMD ["/app/entry-point.sh"]
