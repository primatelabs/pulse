package com.zutubi.pulse.servercore;

import com.zutubi.pulse.core.Bootstrapper;
import com.zutubi.pulse.core.BootstrapperSupport;
import com.zutubi.pulse.core.api.PulseException;
import com.zutubi.pulse.core.commands.api.CommandContext;
import com.zutubi.pulse.core.engine.api.BuildException;
import com.zutubi.pulse.core.engine.api.BuildProperties;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;
import com.zutubi.pulse.core.scm.patch.PatchFormatFactory;
import com.zutubi.pulse.core.scm.patch.api.PatchFormat;
import com.zutubi.pulse.servercore.repository.FileRepository;
import com.zutubi.util.io.FileSystemUtils;
import com.zutubi.util.io.IOUtils;

import java.io.File;

import static com.zutubi.pulse.core.engine.api.BuildProperties.NAMESPACE_INTERNAL;
import static com.zutubi.pulse.core.engine.api.BuildProperties.PROPERTY_FILE_REPOSITORY;

/**
 * A bootstrapper that applies a patch to a working directory bootstrapped
 * by some other bootstrapper.
 */
public class PatchBootstrapper extends BootstrapperSupport implements ScmFeedbackHandler
{
    /**
     * It is the working directory created by this delegate bootstrapper
     * that is being patched.
     */
    private Bootstrapper delegate;
    private long userId;
    private long number;
    private String patchFormatType;

    public PatchBootstrapper(Bootstrapper delegate, long userId, long number, String patchFormatType)
    {
        this.delegate = delegate;
        this.userId = userId;
        this.number = number;
        this.patchFormatType = patchFormatType;
    }

    public void doBootstrap(CommandContext commandContext) throws BuildException
    {
        delegate.bootstrap(commandContext);

        ExecutionContext context = commandContext.getExecutionContext();
        FileRepository fileRepository = context.getValue(NAMESPACE_INTERNAL, PROPERTY_FILE_REPOSITORY, FileRepository.class);
        File patchFile;
        try
        {
            patchFile = fileRepository.getPatchFile(userId, number);
        }
        catch (PulseException e)
        {
            throw new BuildException("Unable to retrieve patch file '" + e.getMessage(), e);
        }

        PatchFormatFactory patchFormatFactory = context.getValue(NAMESPACE_INTERNAL, BuildProperties.PROPERTY_PATCH_FORMAT_FACTORY, PatchFormatFactory.class);
        PatchFormat patchFormat = patchFormatFactory.createByFormatType(patchFormatType);
        ScmClient scmClient = createScmClient(context);
        try
        {
            File baseDir = getBaseBuildDir(context);
            writeFeedback("Patching " + FileSystemUtils.getNormalisedAbsolutePath(baseDir) + " with " + FileSystemUtils.getNormalisedAbsolutePath(patchFile));
            for (Feature feature: patchFormat.applyPatch(context, patchFile, baseDir, scmClient, this))
            {
                commandContext.addFeature(feature);
            }
        }
        catch(PulseException e)
        {
            throw new BuildException("Unable to apply patch: " + e.getMessage(), e);
        }
        finally
        {
            IOUtils.close(scmClient);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected ScmClient createScmClient(ExecutionContext executionContext)
    {
        try
        {
            ScmConfiguration scmConfig = executionContext.getValue(NAMESPACE_INTERNAL, BuildProperties.PROPERTY_SCM_CONFIGURATION, ScmConfiguration.class);
            ScmClientFactory scmClientFactory = executionContext.getValue(NAMESPACE_INTERNAL, BuildProperties.PROPERTY_SCM_CLIENT_FACTORY, ScmClientFactory.class);
            return scmClientFactory.createClient(scmConfig);
        }
        catch (ScmException e)
        {
            throw new BuildException("Unable to create SCM client: " + e.getMessage(), e);
        }
    }

    private File getBaseBuildDir(ExecutionContext context)
    {
        return context.getWorkingDir();
    }

    public void terminate()
    {
        super.terminate();
        delegate.terminate();
    }

    public void status(String message)
    {
        writeFeedback(message);
    }

    public void checkCancelled() throws ScmCancelledException
    {
        if (isTerminated())
        {
            throw new ScmCancelledException("Operation cancelled");
        }
    }
}
