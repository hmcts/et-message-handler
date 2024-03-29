ARG APP_INSIGHTS_AGENT_VERSION=3.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/et-msg-handler.jar /opt/app/

EXPOSE 8085
CMD [ "et-msg-handler.jar" ]
