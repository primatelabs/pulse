package com.zutubi.pulse.master.model;

import com.zutubi.pulse.core.PulseException;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.core.personal.PatchArchive;
import com.zutubi.pulse.master.security.SecureParameter;
import com.zutubi.pulse.master.security.SecureResult;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.tove.security.AccessManager;

import java.util.Collection;
import java.util.List;

/**
 */
public interface ProjectManager extends EntityManager<Project>
{
    String GLOBAL_PROJECT_NAME     = "global project template";
    String TRIGGER_CATEGORY_MANUAL = "manual";

    @SecureResult
    List<ProjectConfiguration> getAllProjectConfigs(boolean allowInvalid);

    @SecureResult
    ProjectConfiguration getProjectConfig(String name, boolean allowInvalid);

    @SecureResult
    ProjectConfiguration getProjectConfig(long id, boolean allowInvalid);

    @SecureResult
    Project getProject(String name, boolean allowInvalid);

    @SecureResult
    Project getProject(long id, boolean allowInvalid);

    @SecureResult
    List<Project> getProjects(boolean allowInvalid);

    boolean isProjectValid(Project project);

    int getProjectCount();

    void abortUnfinishedBuilds(Project project, String message);

    @SecureParameter(action = ProjectConfigurationActions.ACTION_PAUSE)
    Project pauseProject(Project project);

    @SecureParameter(action = ProjectConfigurationActions.ACTION_PAUSE)
    void resumeProject(Project project);

    @SecureParameter(action = AccessManager.ACTION_DELETE)
    void delete(Project project);

    void save(Project project);

    @SecureParameter(action = AccessManager.ACTION_WRITE)
    void checkWrite(Project project);

    /**
     * Triggers a build of the given project by raising appropriate build
     * request events.  Multiple events may be raised depending on
     * configuration, for example if the project is marked for changelist
     * isolation.
     *
     * @param project       the project to trigger a build of
     * @param reason        the reason the build was triggered
     * @param revision      the revision to build, or null if the revision is
     *                      not fixed (in which case changelist isolation may
     *                      result in multiple build requests
     * @param source        a freeform source for the trigger, used to
     *                      identify related triggers for superceding
     * @param replaceable   if true, while queue this build request may be
     *                      replaced by another with the same source (has no
     *                      effect if isolating changelists)
     * @param force         if true, force a build to occur even if the
     *                      latest has been built
     */
    @SecureParameter(action = ProjectConfigurationActions.ACTION_TRIGGER, parameterType = ProjectConfiguration.class)
    void triggerBuild(ProjectConfiguration project, BuildReason reason, Revision revision, String source, boolean replaceable, boolean force);

    @SecureParameter(action = ProjectConfigurationActions.ACTION_TRIGGER, parameterType = Project.class)
    void triggerBuild(long number, Project project, User user, PatchArchive archive) throws PulseException;

    @SecureParameter(action = AccessManager.ACTION_VIEW)
    long getNextBuildNumber(Project project);

    // These are secured as they use mapConfigsToProjects underneath
    Collection<ProjectGroup> getAllProjectGroups();
    ProjectGroup getProjectGroup(String name);

    @SecureResult
    List<Project> mapConfigsToProjects(Collection<ProjectConfiguration> projects);

    void removeReferencesToAgent(long agentStateId);

    @SecureParameter(action = AccessManager.ACTION_WRITE)
    void markForCleanBuild(Project project);
}
