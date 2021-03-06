package com.zutubi.pulse.master.model;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;

/**
 * A collection of predicates for projects.
 */
public class ProjectPredicates
{
    /**
     * A predicate that accepts project configurations that are concrete.
     *
     * @return a predicate that accepts project configurations that are concrete.
     */
    public static Predicate<ProjectConfiguration> concrete()
    {
        return new Predicate<ProjectConfiguration>()
        {
            public boolean apply(ProjectConfiguration projectConfiguration)
            {
                return projectConfiguration.isConcrete();
            }
        };
    }

    /**
     * Returns true if the specified project satisfies the concrete predicate.
     *
     * @param project   the project being tested.
     *
     * @return true if the project is concrete, false otherwise
     *
     * @see #concrete() 
     */
    public static boolean concrete(ProjectConfiguration project)
    {
        return concrete().apply(project);
    }

    /**
     * A predicate that accepts projects with an associated configuration.
     *
     * @return a predicate that accepts projects with an associated configuration.
     */
    public static Predicate<Project> hasConfig()
    {
        return Predicates.not(noConfig());
    }

    /**
     * A predicate that accepts projects with no associated configuration.  These
     * projects are typically considered orphaned projects, and should not be used.
     *
     * @return a predicate that accepts projects with no associated configuration.
     */
    public static Predicate<Project> noConfig()
    {
        return new Predicate<Project>()
        {
            public boolean apply(Project project)
            {
                return project.getConfig() == null;
            }
        };
    }

    /**
     * A predicate that accepts any project instances that are classified as existing.
     * That is, they are not null and they have a configuration.
     *
     * Only projects that 'exist' as defined by this predicate should be used / seen
     * within Pulse.  
     *
     * @return a predicate that accepts projects that are classified as existing.
     *
     * @see #hasConfig()
     */
    public static Predicate<Project> exists()
    {
        return Predicates.and(Predicates.notNull(), hasConfig());
    }

    /**
     * Returns true if the specified project is accepted by the notExists predicate.
     *
     * @param project   the project being tested.
     *
     * @return true if an only if the project is accepted by the notExists predicate, false otherwise.
     *
     * @see #notExists()
     */
    public static boolean notExists(Project project)
    {
        return notExists().apply(project);
    }

    /**
     * A predicate that accepts any project instances that are classified as not existing.
     * That is, they are null or they have no configuration.
     *
     * Any project that satisfies this predicate should be ignored.
     *
     * @return a predicate that accepts any project instances that are classified as not existing.
     */
    public static Predicate<Project> notExists()
    {
        return Predicates.not(exists());
    }

    /**
     * Returns true if the specified project satisfies the exists predicate.
     *
     * @param project   the project being tested.
     *
     * @return true if the project exists, false otherwise.
     *
     * @see #exists() 
     */
    public static boolean exists(Project project)
    {
        return exists().apply(project);
    }

    /**
     * A predicate that accepts only project that are initialised.
     *
     * @return a predicate that accepts any project that is initialised.
     */
    public static Predicate<Project> initialised()
    {
        return new Predicate<Project>()
        {
            public boolean apply(Project project)
            {
                return project.isInitialised();
            }
        };
    }
}
