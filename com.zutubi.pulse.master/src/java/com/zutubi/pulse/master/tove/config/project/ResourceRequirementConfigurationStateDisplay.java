package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.PulseScope;
import com.zutubi.pulse.core.resources.ResourceRequirement;
import com.zutubi.pulse.core.resources.api.ResourcePropertyConfiguration;
import com.zutubi.pulse.master.agent.AgentManager;
import com.zutubi.pulse.master.model.ResourceManager;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.master.tove.format.MessagesAware;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.config.api.Configurations;
import com.zutubi.util.Sort;

import java.util.*;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Formats state fields for resource requirements.
 */
public class ResourceRequirementConfigurationStateDisplay implements MessagesAware
{
    private AgentManager agentManager;
    private ConfigurationProvider configurationProvider;
    private ResourceManager resourceManager;
    private Messages messages;

    /**
     * Returns information about the agents that are compatible with the
     * collection of requirements.  Note that if the parent instance is a build
     * stage, the requirements for the containing project are also taken into
     * account when deciding compatible agents.
     *
     * @param requirementConfigurations the configured requirements
     * @param parentInstance            instance that the requirements are
     *                                  configured on
     * @return if a strict subset of all agents, including more than one agent,
     *         are compatible, a list of those agent names; in all other cases
     *         a simple string describing compatibility (one of "none",
     *         "all agents" or a single agent name)
     */
    public Object formatCollectionCompatibleAgents(Collection<ResourceRequirementConfiguration> requirementConfigurations, Configuration parentInstance)
    {
        List<ResourceRequirementConfiguration> requirementConfigs = new LinkedList<ResourceRequirementConfiguration>(requirementConfigurations);
        PulseScope variables = new PulseScope();
        if (parentInstance instanceof BuildStageConfiguration)
        {
            ProjectConfiguration project = configurationProvider.getAncestorOfType(parentInstance, ProjectConfiguration.class);
            requirementConfigs.addAll(project.getRequirements());
            for (ResourcePropertyConfiguration property: project.getProperties().values())
            {
                variables.add(property.asResourceProperty());
            }
            
            BuildStageConfiguration stage = (BuildStageConfiguration) parentInstance;
            for (ResourcePropertyConfiguration property: stage.getProperties().values())
            {
                variables.add(property.asResourceProperty());
            }            
        }
        else
        {
            ProjectConfiguration project = (ProjectConfiguration) parentInstance;
            for (ResourcePropertyConfiguration property: project.getProperties().values())
            {
                variables.add(property.asResourceProperty());
            }            
        }

        List<ResourceRequirement> requirements = newArrayList(transform(requirementConfigs, new ResourceRequirementConfigurationToRequirement(variables)));
        Set<AgentConfiguration> compatibleAgents = resourceManager.getCapableAgents(requirements);
        int compatibleCount = compatibleAgents.size();
        if (compatibleCount == 0)
        {
            return messages.format("compatibleAgents.none");
        }
        else if (compatibleCount == agentManager.getAgentCount())
        {
            return messages.format("compatibleAgents.all");
        }
        else if (compatibleCount == 1)
        {
            return compatibleAgents.iterator().next().getName();
        }
        else
        {
            List<String> agentNames = newArrayList(transform(compatibleAgents, Configurations.toConfigurationName()));

            Collections.sort(agentNames, new Sort.StringComparator());
            return agentNames;
        }
    }

    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setResourceManager(ResourceManager resourceManager)
    {
        this.resourceManager = resourceManager;
    }

    public void setMessages(Messages messages)
    {
        this.messages = messages;
    }

}
