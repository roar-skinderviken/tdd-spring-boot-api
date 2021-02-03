#mkdir -p target/dependency
#(cd target/dependency; jar -xf ../*.jar)
#docker build -t tdd-spring-boot-app .
#docker run -p 8080:8080 -t tdd-spring-boot-app

FROM openjdk:11-jre-slim
RUN groupadd -r spring && useradd -r -gspring spring
USER spring:spring
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","no.javatec.hoaxify.HoaxifyApplication"]