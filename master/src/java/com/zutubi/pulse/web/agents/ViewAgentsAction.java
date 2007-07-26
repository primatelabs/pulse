package com.zutubi.pulse.web.agents;

import com.zutubi.pulse.agent.Agent;
import com.zutubi.pulse.agent.AgentManager;
import com.zutubi.pulse.agent.DefaultAgent;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.model.AgentState;
import com.zutubi.pulse.web.ActionSupport;

import java.util.List;

/**
 * An action to display all agents attached to this master, including the
 * master itself.
 */
public class ViewAgentsAction extends ActionSupport
{
    private List<Agent> agents;
    private MasterConfigurationManager configurationManager;
    private AgentManager agentManager;

    public List<Agent> getAgents()
    {
        return agents;
    }

    public int getServerPort()
    {
        return configurationManager.getSystemConfig().getServerPort();
    }

    public boolean upgrading(DefaultAgent agent)
    {
        return agent.getEnableState() == AgentState.EnableState.UPGRADING;
    }

    public boolean failedUpgrade(DefaultAgent agent)
    {
        return agent.getEnableState() == AgentState.EnableState.FAILED_UPGRADE;
    }

    public String execute() throws Exception
    {
        agents = agentManager.getAllAgents();
        return SUCCESS;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }
}
