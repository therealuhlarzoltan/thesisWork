FROM eclipse-temurin:22-jdk-jammy AS builder
WORKDIR extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:22-jdk-jammy
RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/Europe/Budapest /etc/localtime && \
    echo "Europe/Budapest" > /etc/timezone
ENV TZ=Europe/Budapest
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./

EXPOSE 8080 5005

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]