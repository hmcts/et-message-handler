ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/et-msg-handler.jar /opt/app/

EXPOSE 8085
CMD [ "et-msg-handler.jar" ]
