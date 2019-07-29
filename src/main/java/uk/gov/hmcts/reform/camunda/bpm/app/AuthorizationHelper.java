package uk.gov.hmcts.reform.camunda.bpm.app;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;

public class AuthorizationHelper {

    private AuthorizationService authorizationService;

    public AuthorizationHelper(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void deploymentAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.DEPLOYMENT)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.DEPLOYMENT);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void taskAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.TASK)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.TASK);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void processDefinition(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.PROCESS_DEFINITION)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.PROCESS_DEFINITION);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void processInstance(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.PROCESS_INSTANCE)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.PROCESS_INSTANCE);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void batchAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.BATCH)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.BATCH);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void decisionDefinitionAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.DECISION_DEFINITION)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.DECISION_DEFINITION);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void optimiseAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.OPTIMIZE)
            .groupIdIn(groupId).count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.OPTIMIZE);
            authorization.setResourceId("*");
            authorization.setGroupId(groupId);

            authorizationService.saveAuthorization(authorization);
        }
    }

    public void cockpitAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.APPLICATION)
            .groupIdIn(groupId).resourceId("cockpit").count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setGroupId(groupId);

            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.APPLICATION);
            authorization.setResourceId("cockpit");
            authorizationService.saveAuthorization(authorization);
        }
    }

    public void tasklistAccess(String groupId) {
        boolean authExists = authorizationService.createAuthorizationQuery().resourceType(Resources.APPLICATION)
            .groupIdIn(groupId).resourceId("tasklist").count() > 0;
        if (!authExists) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setGroupId(groupId);

            authorization.addPermission(Permissions.ALL);
            authorization.setResource(Resources.APPLICATION);
            authorization.setResourceId("tasklist");
            authorizationService.saveAuthorization(authorization);
        }
    }
}
