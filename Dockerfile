FROM maven:3.6.0-jdk-8 as build

WORKDIR /project
ADD . /project
RUN cd /project

RUN mvn package

FROM graylog/graylog:3.3

COPY --from=build /project/target/original-* /usr/share/graylog/plugin/
