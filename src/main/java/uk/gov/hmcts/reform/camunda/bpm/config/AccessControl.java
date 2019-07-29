package uk.gov.hmcts.reform.camunda.bpm.config;

public class AccessControl {

    private boolean deploymentAccess;
    private boolean taskAccess;
    private boolean processDefinition;
    private boolean processInstance;
    private boolean batchAccess;
    private boolean decisionDefinitionAccess;
    private boolean optimiseAccess;

    public boolean isDeploymentAccess() {
        return deploymentAccess;
    }

    public void setDeploymentAccess(boolean deploymentAccess) {
        this.deploymentAccess = deploymentAccess;
    }

    public boolean isTaskAccess() {
        return taskAccess;
    }

    public void setTaskAccess(boolean taskAccess) {
        this.taskAccess = taskAccess;
    }

    public boolean isProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(boolean processDefinition) {
        this.processDefinition = processDefinition;
    }

    public boolean isProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(boolean processInstance) {
        this.processInstance = processInstance;
    }

    public boolean isBatchAccess() {
        return batchAccess;
    }

    public void setBatchAccess(boolean batchAccess) {
        this.batchAccess = batchAccess;
    }

    public boolean isDecisionDefinitionAccess() {
        return decisionDefinitionAccess;
    }

    public void setDecisionDefinitionAccess(boolean decisionDefinitionAccess) {
        this.decisionDefinitionAccess = decisionDefinitionAccess;
    }

    public boolean isOptimiseAccess() {
        return optimiseAccess;
    }

    public void setOptimiseAccess(boolean optimiseAccess) {
        this.optimiseAccess = optimiseAccess;
    }

}
