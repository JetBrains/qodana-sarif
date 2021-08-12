FROM amazoncorretto:11
RUN curl -o /usr/local/bin/dumb-init -L https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64 \
    && chmod +x /usr/local/bin/dumb-init
RUN curl --silent --location https://rpm.nodesource.com/setup_14.x | bash - \
    && yum -y install nodejs \
    && npm install --global http-server
WORKDIR app
COPY build/install/sarif-converter .
COPY ui ui
COPY entrypoint.sh .
EXPOSE 8080:8080
ENTRYPOINT ["dumb-init", "--", "/app/entrypoint.sh"]
