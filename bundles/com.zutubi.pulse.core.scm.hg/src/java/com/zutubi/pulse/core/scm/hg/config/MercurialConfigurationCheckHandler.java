package com.zutubi.pulse.core.scm.hg.config;

import com.zutubi.pulse.core.scm.api.ScmClientFactory;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.hg.MercurialClient;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Wire;
import com.zutubi.tove.config.api.AbstractConfigurationCheckHandler;

/**
 * Tests connections to mercurial repositories.
 */
@Wire
@SymbolicName("zutubi.mercurialConfigurationCheckHandler")
public class MercurialConfigurationCheckHandler extends AbstractConfigurationCheckHandler<MercurialConfiguration>
{
    private ScmClientFactory<? super MercurialConfiguration> scmClientFactory;

    public void test(MercurialConfiguration configuration) throws ScmException
    {
        MercurialClient client = null;
        try
        {
            client = (MercurialClient) scmClientFactory.createClient(configuration);
            client.testConnection();
        }
        finally
        {
            if (client != null)
            {
                client.close();
            }
        }
    }

    public void setScmClientFactory(ScmClientFactory<? super MercurialConfiguration> scmClientManager)
    {
        this.scmClientFactory = scmClientManager;
    }
}

