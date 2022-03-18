ARG APP_INSIGHTS_AGENT_VERSION=3.2.8
FROM hmctspublic.azurecr.io/base/java:openjdk-17-distroless-1.5.2

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/camunda-bpm.jar /opt/app/

EXPOSE 8999

CMD ["camunda-bpm.jar"]
