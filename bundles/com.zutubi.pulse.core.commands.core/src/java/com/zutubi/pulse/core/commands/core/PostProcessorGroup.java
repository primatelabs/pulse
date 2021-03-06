package com.zutubi.pulse.core.commands.core;

import com.google.common.base.Function;
import com.zutubi.pulse.core.postprocessors.api.PostProcessor;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorConfiguration;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorContext;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorFactory;

import java.io.File;
import java.util.Collection;

import static com.google.common.collect.Collections2.transform;


/**
 * A group of post-processors.  Simply applies all processors in the group in
 * order.
 */
public class PostProcessorGroup implements PostProcessor
{
    private PostProcessorFactory postProcessorFactory;
    private PostProcessorGroupConfiguration config;

    public PostProcessorGroup(PostProcessorGroupConfiguration config)
    {
        this.config = config;
    }

    public PostProcessorGroupConfiguration getConfig()
    {
        return config;
    }

    protected PostProcessor createChildProcessor(PostProcessorConfiguration childConfig)
    {
        return postProcessorFactory.create(childConfig);
    }

    public void process(File artifactFile, PostProcessorContext ppContext)
    {
        Collection<PostProcessor> processors = transform(config.getProcessors().values(), new Function<PostProcessorConfiguration, PostProcessor>()
        {
            public PostProcessor apply(PostProcessorConfiguration childConfig)
            {
                return createChildProcessor(childConfig);
            }
        });

        for (PostProcessor processor : processors)
        {
            processor.process(artifactFile, ppContext);
        }
    }

    public void setPostProcessorFactory(PostProcessorFactory postProcessorFactory)
    {
        this.postProcessorFactory = postProcessorFactory;
    }
}
