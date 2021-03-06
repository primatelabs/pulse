package com.zutubi.pulse.master.tove.config.agent;

import com.zutubi.pulse.master.model.AgentStateManager;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.util.TransactionContext;
import com.zutubi.tove.config.cleanup.RecordCleanupTask;

import java.util.Arrays;
import java.util.List;

/**
 * Adds a custom cleanup task for agent configuration that deletes the
 * agent state.
 */
public class AgentConfigurationCleanupTasks
{
    private AgentStateManager agentStateManager;
    private TransactionContext transactionContext;

    public List<RecordCleanupTask> getTasks(AgentConfiguration instance)
    {
        return Arrays.<RecordCleanupTask>asList(new AgentStateCleanupTask(instance, agentStateManager, transactionContext));
    }

    public void setAgentStateManager(AgentStateManager agentStateManager)
    {
        this.agentStateManager = agentStateManager;
    }

    public void setTransactionContext(TransactionContext transactionContext)
    {
        this.transactionContext = transactionContext;
    }
}
