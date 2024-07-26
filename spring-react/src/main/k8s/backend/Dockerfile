FROM eclipse-temurin:21-jdk-alpine AS builder
ARG JAR_FILE=../../../../target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted
COPY . .

##

FROM eclipse-temurin:21-jdk-alpine
COPY --from=builder extracted/dependencies ./
COPY --from=builder extracted/snapshot-dependencies ./
COPY --from=builder extracted/spring-boot-loader ./
COPY --from=builder extracted/application/ ./
ENTRYPOINT ["java", "-jar", "application.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]