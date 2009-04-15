package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.Select;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.validation.annotations.Constraint;
import com.zutubi.validation.annotations.Required;

import java.util.LinkedList;
import java.util.List;

import org.apache.ivy.core.module.status.StatusManager;

/**
 * The manually configured dependencies.
 */
@SymbolicName("zutubi.dependenciesConfiguration")
@Form(fieldOrder = {"publicationPattern", "retrievalPattern", "status"})
public class DependenciesConfiguration extends AbstractConfiguration
{
    private List<DependencyConfiguration> dependencies = new LinkedList<DependencyConfiguration>();

    @Required
    @Constraint("com.zutubi.pulse.core.dependency.ivy.IvyPatternValidator")
    private String publicationPattern = "build/[artifact].[ext]";
    
    @Required
    @Constraint("com.zutubi.pulse.core.dependency.ivy.IvyPatternValidator")
    private String retrievalPattern = "lib/[artifact].[ext]";

    @Required
    @Constraint("com.zutubi.pulse.core.dependency.StatusValidator")
    @Select(optionProvider = "com.zutubi.pulse.master.tove.config.project.BuildStatusOptionProvider")
    private String status = StatusManager.getCurrent().getDefaultStatus();

    private List<PublicationConfiguration> publications = new LinkedList<PublicationConfiguration>();

    public List<DependencyConfiguration> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies(List<DependencyConfiguration> dependencies)
    {
        this.dependencies = dependencies;
    }

    public String getPublicationPattern()
    {
        return publicationPattern;
    }

    public void setPublicationPattern(String publicationPattern)
    {
        this.publicationPattern = publicationPattern;
    }

    public String getRetrievalPattern()
    {
        return retrievalPattern;
    }

    public void setRetrievalPattern(String retrievalPattern)
    {
        this.retrievalPattern = retrievalPattern;
    }

    public List<PublicationConfiguration> getPublications()
    {
        return publications;
    }

    public void setPublications(List<PublicationConfiguration> publications)
    {
        this.publications = publications;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
