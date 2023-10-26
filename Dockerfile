FROM clojure:temurin-17-alpine AS builder
MAINTAINER Humberto Henrique Campos Pinheiro <humbhenri@gmail.com>
ENV CLOJURE_VERSION=1.11.1.1182
RUN mkdir -p /build
WORKDIR /build
COPY deps.edn /build/
RUN clojure -P -X:build
COPY ./ /build
RUN clojure -T:build uber

FROM eclipse-temurin:17-alpine
ARG DB_PORT
ARG DB_USERNAME
ARG DB_PASSWORD
ARG DB_DATABASE
EXPOSE 8080
RUN mkdir -p /service
WORKDIR /service
COPY --from=builder /build/target/rinha-0.0.1-standalone.jar /service/rinha-0.0.1-standalone.jar
ENTRYPOINT ["java", "-jar", "/service/rinha-0.0.1-standalone.jar"]
