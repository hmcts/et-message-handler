ARG APP_INSIGHTS_AGENT_VERSION=3.4.11

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/et-msg-handler.jar /opt/app/

EXPOSE 8085
CMD [ "et-msg-handler.jar" ]
