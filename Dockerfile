# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.5.1
ARG PLATFORM=""
FROM hmctspublic.azurecr.io/base/java${PLATFORM}:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/camunda-bpm.jar /opt/app/

EXPOSE 8999

CMD ["camunda-bpm.jar"]
