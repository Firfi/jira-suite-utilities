package com.googlecode.jsu.workflow.function;

import static com.googlecode.jsu.workflow.WorkflowUpdateIssueCustomFieldFunctionPluginFactory.TARGET_FIELD_NAME;
import static com.googlecode.jsu.workflow.WorkflowUpdateIssueCustomFieldFunctionPluginFactory.TARGET_FIELD_VALUE;

import java.sql.Timestamp;
import java.util.Map;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.util.I18nHelper;
import com.googlecode.jsu.util.WorkflowUtils;
import com.opensymphony.module.propertyset.PropertySet;
import com.atlassian.crowd.embedded.api.User;
import com.opensymphony.workflow.WorkflowException;

/**
 * Class related to the execution of the plugin.
 *
 * @author Cristiane Fontana
 * @version 1.0
 *
 */
public class UpdateIssueCustomFieldPostFunction extends AbstractPreserveChangesPostFunction {
    private final WorkflowUtils workflowUtils;
    private final I18nHelper.BeanFactory beanFactory;

    /**
     * @param workflowUtils
     */
    public UpdateIssueCustomFieldPostFunction(WorkflowUtils workflowUtils, I18nHelper.BeanFactory beanFactory) {
        this.workflowUtils = workflowUtils;
        this.beanFactory = beanFactory;
    }

    /* (non-Javadoc)
     * @see com.googlecode.jsu.workflow.function.AbstractPreserveChangesPostFunction#executeFunction(java.util.Map, java.util.Map, com.opensymphony.module.propertyset.PropertySet, com.atlassian.jira.issue.util.IssueChangeHolder)
     */
    @Override
    protected void executeFunction(
            Map<String, Object> transientVars, Map<String, String> args,
            PropertySet ps, IssueChangeHolder holder
    ) throws WorkflowException {
        String fieldKey = args.get(TARGET_FIELD_NAME);

        final Field field = workflowUtils.getFieldFromKey(fieldKey);
        final String fieldName = (field != null) ? field.getName() : "null";

        String configuredValue = args.get(TARGET_FIELD_VALUE);
        Object newValue;

        User currentUser = null;
        try {
            currentUser = getCaller(transientVars, args);
        } catch (Exception e) {
            log.error("Unable to find caller for function", e);
            throw(new WorkflowException(e));
        }

        if ("null".equals(configuredValue)) {
            newValue = null;
        } else if ("%%CURRENT_USER%%".equals(configuredValue)) {
            newValue = currentUser.getName();
        } else if ("%%CURRENT_DATETIME%%".equals(configuredValue)) {
            newValue = new Timestamp(System.currentTimeMillis());
        } else {
            newValue = configuredValue;
        }

        MutableIssue issue = null;

        try {
            issue = getIssue(transientVars);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Updating custom field '%s - %s' in issue [%s] with value [%s]",
                        fieldKey, fieldName,
                        issueToString(issue),
                        newValue
                ));
            }

            workflowUtils.setFieldValue(currentUser, issue, fieldKey, newValue, holder);
        } catch (Exception e) {
            I18nHelper i18nh = this.beanFactory.getInstance(
                ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser());
            String message = i18nh.getText("updateissuefield-function-view.unable_to_update",fieldKey,fieldName,issueToString(issue));

            log.error(message, e);

            throw new WorkflowException(message);
        }
    }

    private String issueToString(MutableIssue issue) {
        return ((issue != null) ? ((issue.getKey() == null) ? "new issue" : issue.getKey()) : "null");
    }
}
