FROM clojure:temurin-17-tools-deps-bullseye-slim AS builder

RUN mkdir -p /build
WORKDIR /build

COPY deps.edn /build/
RUN clojure -P -X:build
COPY build.clj /build/
COPY src /build/src

RUN clj -T:uberjar uber

FROM eclipse-temurin:17

RUN addgroup usergroup; adduser  --ingroup usergroup --disabled-password user
USER user

RUN mkdir -p /home/user
WORKDIR /home/user
COPY --from=builder /build/target/rinha.jar rinha.jar

# There's probably a good combination of flags to use here. But I don't know them.
ENTRYPOINT ["java", "-Xms2g", "-Xmx2g", "-XX:+UseContainerSupport", "-jar", "rinha.jar"]
