FROM openjdk:11

MAINTAINER Nikita Kochetkov <nikita.kochetkov@jetbrains.com>

ADD "sarif-converter/build/libs/sarifConverter.jar" "/bin/sarifConverter.jar"

WORKDIR "/data"

ENTRYPOINT ["java", "-Xmx4g", "-jar", "/bin/sarifConverter.jar"]
CMD ["-s", "/data/sarif.json", "-o", "/data/output"]