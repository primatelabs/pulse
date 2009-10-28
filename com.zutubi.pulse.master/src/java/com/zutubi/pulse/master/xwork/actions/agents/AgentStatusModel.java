package com.zutubi.pulse.master.xwork.actions.agents;

import com.zutubi.pulse.master.agent.Agent;
import com.zutubi.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Model for the agent status tab.
 */
public class AgentStatusModel
{
    private Agent agent;
    private String location;
    private List<Pair<String, String>> statusInfo = new LinkedList<Pair<String, String>>();

    public AgentStatusModel(Agent agent, String location)
    {
        this.agent = agent;
        this.location = location;
    }

    public Agent getAgent()
    {
        return agent;
    }

    public String getLocation()
    {
        return location;
    }

    public List<Pair<String, String>> getStatusInfo()
    {
        return statusInfo;
    }

    public void addStatusInfo(String name, String value)
    {
        statusInfo.add(new Pair<String, String>(name, value));
    }
}