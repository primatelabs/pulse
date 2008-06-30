package com.zutubi.pulse.core.postprocessors;

import com.zutubi.pulse.core.BuildException;
import com.zutubi.util.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p>
 * Support base class for post processors that read text files.  Handles
 * setup of a reader and IOExceptions.
 * </p>
 * <p>
 * Note that processors which deal with text files line-by-line may be better
 * served by the extra features in {@link LineBasedPostProcessorSupport}.
 * </p>
 *
 * @see LineBasedPostProcessorSupport
 */
public abstract class TextFilePostProcessorSupport extends PostProcessorSupport
{
    protected void process(File artifactFile, PostProcessorContext ppContext)
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(artifactFile));
            process(reader, ppContext);
        }
        catch (IOException e)
        {
            throw new BuildException(e);
        }
        finally
        {
            IOUtils.close(reader);
        }
    }

    /**
     * Called once for each file to be processed by this post-processor.  The
     * given reader is ready to read at the beginning of the file and will be
     * closed by this implementation after this call completes.  Any
     * information found during processing should be reported using the given
     * context.
     *
     * @param reader    a reader intialised with the file to process
     * @param ppContext context in which the processing is executing
     * @throws IOException if there is an error interacting with the reader
     */
    protected abstract void process(BufferedReader reader, PostProcessorContext ppContext) throws IOException;
}
