FROM openjdk:11
ARG JAR_NAME=dc-file-downloader-all.jar
ENV TZ=Asia/Shanghai
RUN useradd dowonloader
COPY --chown=dowonloader ${JAR_NAME} /app/
COPY --chown=dowonloader entry-point.sh /app/bin/
RUN ln -s /app/${JAR_NAME} /app/application.jar && chmod 744 /app/bin/entry-point.sh
USER dowonloader
WORKDIR  /app
CMD ["/app/bin/entry-point.sh"]