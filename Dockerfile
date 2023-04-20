ARG APP_INSIGHTS_AGENT_VERSION=3.2.6

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-17-distroless-1.4

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/et-msg-handler.jar /opt/app/

EXPOSE 8085
CMD [ "et-msg-handler.jar" ]
