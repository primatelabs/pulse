package com.zutubi.pulse.master.events.build;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.RecipeResultNode;

/**
 * An event raised just after a build stage completes.
 */
public class PostStageEvent extends StageEvent
{
    public PostStageEvent(Object source, BuildResult result, RecipeResultNode stageNode, PulseExecutionContext context)
    {
        super(source, result, stageNode, context);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("Post Stage Event");
        if (getBuildResult() != null)
        {
            builder.append(": ").append(getBuildResult().getId());
        }
        if (getStageNode() != null)
        {
            builder.append(": ").append(getStageNode().getStageName());
        }
        return builder.toString();
    }
}
