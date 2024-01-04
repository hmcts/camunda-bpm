## Integrating Spring Security with Camunda
 
### Overview

This project adds the integration of [Spring Security](https://projects.spring.io/spring-security/) with Camunda, so one can create a 
SSO solution based on Spring Security and the [Camunda Spring Boot starter](https://docs.camunda.org/manual/latest/user-guide/spring-boot-integration/).

The main idea is to offload authentication to Spring Security which then makes it easy to plug in any authentication mechanism. 

### Onboard a new team/ tenant

- One off configuration is needed by teams to add tenant to Camunda. See [example PR](https://github.com/hmcts/camunda-bpm/pull/403)
- A member of chosen Azure AD Group chosen above should login into Camunda UI on that environment for the new tenant to show up.

### Optimize setup

- Docker image is imported to hmctsprivate from `registry.camunda.cloud` ( credentials in `rpe-prod` keyvault)
- Elastic search is added in [camunda-shared-infrastructure](https://github.com/hmcts/camunda-shared-infrastructure)
- Installed to AKS using [cnp-flux-config](https://github.com/hmcts/cnp-flux-config/blob/master/k8s/namespaces/camunda/camunda-optimize/)
- It currently doesn't support Azure AD auth, admin password is in `camunda-{env}`
- It currently doesn't pick license on start up, we need to add it one-off per environment (license in `rpe-prod` keyvault) 
