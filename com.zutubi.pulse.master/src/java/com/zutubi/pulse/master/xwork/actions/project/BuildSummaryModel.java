package com.zutubi.pulse.master.xwork.actions.project;

import com.google.common.base.Predicate;
import static com.google.common.collect.Iterables.find;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Comment;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.pulse.master.tove.model.ActionLink;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.pulse.master.xwork.actions.CommentModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Model for JSON data for the build summary tab.
 */
public class BuildSummaryModel
{
    private ProjectResponsibilityModel responsibility;
    private BuildModel build;
    private List<CommentModel> comments = new LinkedList<CommentModel>();
    private BuildFeaturesModel errors;
    private BuildFeaturesModel warnings;
    private List<BuildStageTestFailuresModel> failures;
    private List<ActionLink> actions = new LinkedList<ActionLink>();
    private Set<ActionLink> links = new TreeSet<ActionLink>();
    private List<ActionLink> hooks = new LinkedList<ActionLink>();

    public BuildSummaryModel(BuildResult buildResult, Urls urls)
    {
        build = new BuildModel(buildResult, urls, true);
        if (buildResult.hasMessages(Feature.Level.ERROR))
        {
            errors = new BuildFeaturesModel(buildResult, Feature.Level.ERROR, urls);
        }
        if (buildResult.hasMessages(Feature.Level.WARNING))
        {
            warnings = new BuildFeaturesModel(buildResult, Feature.Level.WARNING, urls);
        }
        if(buildResult.hasBrokenTests())
        {
            failures = new LinkedList<BuildStageTestFailuresModel>();
            for (RecipeResultNode stageResult: buildResult.getStages())
            {
                if (stageResult.hasBrokenTests())
                {
                    failures.add(new BuildStageTestFailuresModel(buildResult, stageResult, urls));
                }
            }
        }
    }

    public ProjectResponsibilityModel getResponsibility()
    {
        return responsibility;
    }

    public void setResponsibility(ProjectResponsibilityModel responsibility)
    {
        this.responsibility = responsibility;
    }

    public BuildModel getBuild()
    {
        return build;
    }

    public List<CommentModel> getComments()
    {
        return comments;
    }

    public void addComment(Comment comment, boolean canDelete)
    {
        comments.add(new CommentModel(comment, canDelete));
    }

    public BuildFeaturesModel getErrors()
    {
        return errors;
    }

    public BuildFeaturesModel getWarnings()
    {
        return warnings;
    }

    public List<BuildStageTestFailuresModel> getFailures()
    {
        return failures;
    }

    public List<ActionLink> getActions()
    {
        return actions;
    }

    public void addAction(ActionLink action)
    {
        actions.add(action);
    }
    
    public Set<ActionLink> getLinks()
    {
        return links;
    }

    public void addLink(ActionLink link)
    {
        links.add(link);
    }

    public List<ActionLink> getHooks()
    {
        return hooks;
    }

    public void addHook(ActionLink hook)
    {
        hooks.add(hook);
    }

    public BuildStageModel getStage(final String stageName)
    {
        return find(build.getStages(), new Predicate<BuildStageModel>()
        {
            public boolean apply(BuildStageModel buildStageModel)
            {
                return buildStageModel.getName().equals(stageName);
            }
        }, null);
    }

}
