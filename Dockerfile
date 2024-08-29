FROM nas.nald.me:35003/docker/settings/openjdk:17-alpine

ARG JAR_FILE
ENV TARGET_JAR_FILE ${JAR_FILE}
ADD ./target/${TARGET_JAR_FILE} /${TARGET_JAR_FILE}

ENV JAVA_OPTS "-Djava.security.egd=file:/dev/./urandom"
ENV VER=1.0
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en

RUN apk update && \
    apk add --no-cache openjdk11 && \
    apk add openssh && \
#    apk add zip && \
    apk add curl && \
    apk add ffmpeg && \
    apk add nano

RUN mkdir -p /root/.ssh
RUN chmod 700 /root/.ssh

RUN mkdir -p /nald   \
             /Logs

USER root
ENV TZ 'Asia/Seoul'
RUN echo $TZ > /etc/timezone

RUN apk update && \
    apk add --no-cache tzdata

EXPOSE 8000
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /${TARGET_JAR_FILE}"]
