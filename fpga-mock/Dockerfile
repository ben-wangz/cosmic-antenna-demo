FROM openjdk:11
ARG JAR_NAME=fpga-mock-all.jar
ENV TZ=Asia/Shanghai
RUN useradd fpga-mock
COPY --chown=fpga-mock ${JAR_NAME} /app/
COPY --chown=fpga-mock entry-point.sh /app/bin/
RUN ln -s /app/${JAR_NAME} /app/application.jar && chmod 744 /app/bin/entry-point.sh
USER fpga-mock
WORKDIR  /app
CMD ["/app/bin/entry-point.sh"]