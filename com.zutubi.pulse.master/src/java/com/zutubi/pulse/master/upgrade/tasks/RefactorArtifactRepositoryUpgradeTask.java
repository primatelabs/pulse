package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.pulse.core.dependency.ivy.IvyConfiguration;
import com.zutubi.pulse.core.dependency.ivy.IvyModuleDescriptor;
import com.zutubi.pulse.master.util.monitor.TaskException;
import com.zutubi.util.Predicate;
import com.zutubi.util.io.FileSystemUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;

import java.io.File;
import java.util.List;

/**
 * An upgrade task that allows the artifact repository format to be adjusted.
 */
public abstract class RefactorArtifactRepositoryUpgradeTask extends AbstractUpgradeTask
{
    private static final String MODULE_PATTERN = "([organisation]/)[module]";
    private static final String IVY_PATTERN = MODULE_PATTERN + "/ivy-[revision].xml";

    private File repositoryBase = null;

    public boolean haltOnFailure()
    {
        // failure will render old artifacts unaccessible.
        return true;
    }

    public void execute(String existingArtifactPattern, String newArtifactPattern) throws TaskException
    {
        List<File> ivyFiles = FileSystemUtils.filter(repositoryBase, new Predicate<File>()
        {
            public boolean satisfied(File file)
            {
                String filename = file.getName();
                return filename.startsWith("ivy") && filename.endsWith(".xml");
            }
        });

        IvyConfiguration existingIvyConfiguration = new IvyConfiguration(repositoryBase, MODULE_PATTERN, existingArtifactPattern, IVY_PATTERN);

        for (File ivyFile : ivyFiles)
        {
            try
            {
                IvyModuleDescriptor descriptor = IvyModuleDescriptor.newInstance(ivyFile, existingIvyConfiguration);

                Artifact[] artifacts = descriptor.getAllArtifacts();

                for (Artifact artifact : artifacts)
                {
                    File sourceFile = null;
                    File destinationFile = null;
                    try
                    {
                        sourceFile = new File(repositoryBase, IvyPatternHelper.substitute(existingArtifactPattern, artifact));
                        destinationFile = new File(repositoryBase, IvyPatternHelper.substitute(newArtifactPattern, artifact));
                        FileSystemUtils.rename(sourceFile, destinationFile, true);
                    }
                    catch (Exception e)
                    {
                        addError("Failed to rename " + sourceFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath());
                    }
                }
            }
            catch (Exception e)
            {
                addError(e.getMessage());
            }
        }
    }

    public void setRepositoryBase(File repositoryBase)
    {
        this.repositoryBase = repositoryBase;
    }
}
