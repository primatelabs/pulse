package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.core.test.TestUtils;
import com.zutubi.pulse.core.util.PulseZipUtils;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.servercore.bootstrap.SystemConfiguration;
import com.zutubi.util.*;
import com.zutubi.util.config.Config;
import com.zutubi.util.config.FileConfig;
import com.zutubi.util.config.ReadOnlyConfig;
import com.zutubi.util.io.IOUtils;
import freemarker.template.utility.StringUtil;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AcceptanceTestUtils
{
    /**
     * The acceptance test system property for the built pulse package.
     */
    protected static final String PROPERTY_PULSE_PACKAGE = "pulse.package";

    /**
     * The acceptance test system property for the built agent package.
     */
    protected static final String PROPERTY_AGENT_PACKAGE = "agent.package";

    /**
     * The acceptance test system property for the built dev package.
     */
    protected static final String PROPERTY_DEV_PACKAGE = "dev.package";
    
    /**
     * The acceptance test system property for the pulse startup port.
     */
    public static final String PROPERTY_PULSE_PORT = "pulse.port";

    /**
     * The acceptance test system property for the agent startup port.
     */
    public static final String PROPERTY_AGENT_PORT = "agent.port";

    public static final String PROPERTY_WORK_DIR = "work.dir";

    /**
     * Id of the plugin which can be used to make test-specific plugins with
     * {@link #createTestPlugin(java.io.File, String, String)}. 
     */
    public static final String PLUGIN_ID_TEST = "com.zutubi.pulse.core.postprocessors.test";
    
    private static final long STATUS_TIMEOUT = 30000;
    /**
     * The credentials for the admin user.
     */
    public static final UsernamePasswordCredentials ADMIN_CREDENTIALS = new UsernamePasswordCredentials("admin", "admin");

    public static int getPulsePort()
    {
        return Integer.getInteger(PROPERTY_PULSE_PORT, 8080);
    }

    public static void setPulsePort(int port)
    {
        System.setProperty(PROPERTY_PULSE_PORT, Integer.toString(port));
    }

    public static int getAgentPort()
    {
        return Integer.getInteger(PROPERTY_AGENT_PORT, 8890);
    }

    public static File getWorkingDirectory()
    {
        // from IDEA, the working directory is located in the same directory as where the projects are run.
        File workingDir = new File("./working");
        if (isAntBuild())
        {
            // from the acceptance test suite, the work.dir system property is specified
            workingDir = new File(System.getProperty(PROPERTY_WORK_DIR));
        }
        return workingDir;
    }

    private static boolean isAntBuild()
    {
        return System.getProperties().containsKey(PROPERTY_WORK_DIR);
    }

    public static File getDataDirectory() throws IOException
    {
        // Acceptance tests should all be using the user.home directory in the /working directory.
        File userHome = new File(getWorkingDirectory(), "user.home");
        Config config = loadConfigFromHome(userHome);
        if (config != null)
        {
            return new File(config.getProperty(SystemConfiguration.PULSE_DATA));
        }

        // Guess at the ./data directory in the current working directory.
        File data = new File("./data");
        if (data.exists())
        {
            return data;
        }

        // chances are that if we pick up the systems user.home we may or may not
        // be picking up the right config so we try it last.
        userHome = new File(System.getProperty("user.home"));
        config = loadConfigFromHome(userHome);
        if (config != null)
        {
            return new File(config.getProperty(SystemConfiguration.PULSE_DATA));
        }

        return null;
    }

    /**
     * Returns the admin token for the testing agent.
     * 
     * @return the admin token for the testing agent
     * @throws IOException on error reading the token file
     */
    public static String getAgentAdminToken() throws IOException
    {
        File configDir;
        if (isAntBuild())
        {
            File agentWork = new File(getWorkingDirectory(), AcceptanceTestSuiteSetupTeardown.WORK_DIR_AGENT);
            File versionHome = new File(FileSystemUtils.findFirstChildMatching(agentWork, "pulse-agent-.*"), "versions");
            configDir = new File(FileSystemUtils.findFirstChildMatching(versionHome, "[0-9]+"), "system/config");
        }
        else
        {
            configDir = new File("com.zutubi.pulse.slave/etc");
        }
        
        File tokenFile = new File(configDir, "admin.token");
        return IOUtils.fileToString(tokenFile);
    }

    /**
     * Load the config.properties instance from the specified user home directory.
     *
     * @param userHome  the user home directory being used by Pulse.
     * @return  the config instance of null if the config.properties file was not located.
     */
    private static Config loadConfigFromHome(File userHome)
    {
        File configFile = new File(userHome, MasterConfigurationManager.CONFIG_DIR + "/config.properties");
        if (configFile.exists())
        {
            return new ReadOnlyConfig(new FileConfig(configFile));
        }
        return null;
    }

    /**
     * Wait for the condition to be true before returning.  If the condition does not return true with
     * the given timeout, a runtime exception is generated with a message based on the description.  Note
     * that the wait will last at least as long as the timeout period, and maybe a little longer.
     *
     * @param condition     the condition which needs to be satisfied before returning
     * @param timeout       the amount of time given for the condition to return true before
     * generating a runtime exception
     * @param description   a human readable description of what the condition is waiting for which will be
     * used in the message of the generated timeout exception
     *
     * @throws RuntimeException if the timeout is reached or if this thread is interrupted.
     */
    public static void waitForCondition(Condition condition, long timeout, String description)
    {
        long endTime = System.currentTimeMillis() + timeout;
        while (!condition.satisfied())
        {
            if (System.currentTimeMillis() > endTime)
            {
                throw new RuntimeException("Timed out waiting for " + description);
            }

            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("Interrupted waiting for " + description);
            }
        }
    }

    /**
     * Returns the location of a Pulse package, based on the pulse.package
     * system property.
     *
     * @return file reference to the pulse package
     * @throws IllegalStateException if pulse.package os not set or does not
     *                               refer to a valid file
     */
    public static File getPulsePackage()
    {
        return getPackage(PROPERTY_PULSE_PACKAGE);
    }

    /**
     * Returns the location of the Pulse agent package, based on the agent.package
     * system property.
     *
     * @return file reference to the pulse agent package.
     */
    public static File getAgentPackage()
    {
        return getPackage(PROPERTY_AGENT_PACKAGE);
    }

    /**
     * Returns the location of the Pulse dev package, based on the dev.package
     * system property.
     *
     * @return file reference to the pulse agent package.
     */
    public static File getDevPackage()
    {
        return getPackage(PROPERTY_DEV_PACKAGE);
    }
    
    private static File getPackage(String packageProperty)
    {
        String pkgProperty = System.getProperty(packageProperty);
        if (!StringUtils.stringSet(pkgProperty))
        {
            throw new IllegalStateException("No package specified (use the system property " + packageProperty + ")");
        }
        File pkg = new File(pkgProperty);
        if (!pkg.exists())
        {
            throw new IllegalStateException("Unexpected invalid " + packageProperty + ": " + pkg + " does not exist.");
        }
        return pkg;
    }

    /**
     * Reads the text content available at the given Pulse URI and returns it
     * as a string.  Supplies administrator credentials to log in to Pulse.
     *
     * @param contentUri uri to download the content from
     * @return the content available at the given URI, as a string
     * @throws IOException on error
     */
    public static String readUriContent(String contentUri) throws IOException
    {
        return readUriContent(contentUri, ADMIN_CREDENTIALS);
    }

    /**
     * Reads the text content available at the given Pulse URI and returns it
     * as a string.  Supplies the given credentials to log in to Pulse.
     *
     * @param contentUri  uri to download the content from
     * @param credentials credentials of a Pulse user to log in as
     * @return the content available at the given URI, as a string
     * @throws IOException on error
     */
    public static String readUriContent(String contentUri, Credentials credentials) throws IOException
    {
        InputStream input = null;
        GetMethod get = null;
        try
        {
            get = httpGet(contentUri, credentials);
            input = get.getResponseBodyAsStream();
            return IOUtils.inputStreamToString(input);
        }
        finally
        {
            IOUtils.close(input);
            releaseConnection(get);
        }
    }

    /**
     * Reads and returns an HTTP header from the given Pulse URI, returning it
     * for further inspection.  Supplies administrator credentials to log in to
     * Pulse.
     *
     * @param uri        uri to read the header from
     * @param headerName name of the header to retrieve
     * @return the found header, or null if there was no such header
     * @throws IOException on error
     */
    public static Header readHttpHeader(String uri, String headerName) throws IOException
    {
        return readHttpHeader(uri, headerName, ADMIN_CREDENTIALS);
    }

    /**
     * Reads and returns an HTTP header from the given Pulse URI, returning it
     * for further inspection.  Supplies the given credentials to log in to
     * Pulse.
     *
     * @param uri         uri to read the header from
     * @param headerName  name of the header to retrieve
     * @param credentials credentials of a Pulse user to log in as
     * @return the found header, or null if there was no such header
     * @throws IOException on error
     */
    public static Header readHttpHeader(String uri, final String headerName, Credentials credentials) throws IOException
    {
        GetMethod get = null;
        try
        {
            get = AcceptanceTestUtils.httpGet(uri, credentials);
            Header[] headers = get.getResponseHeaders();

            return CollectionUtils.find(headers, new Predicate<Header>()
            {
                public boolean satisfied(Header header)
                {
                    return header.getName().equals(headerName);
                }
            });
        }
        finally
        {
            releaseConnection(get);
        }
    }

    /**
     * Executes an HTTP get of the given Pulse URI and returns the {@link org.apache.commons.httpclient.methods.GetMethod}
     * instance for further processing.  The caller is responsible for
     * releasing the connection (by calling {@link org.apache.commons.httpclient.methods.GetMethod#releaseConnection()})
     * when it is no longer required.  Supplies the given credentials to log in
     * to Pulse.
     *
     * @param uri         uri to GET
     * @param credentials credentials of a Pulse user to log in as, or null if
     *                    no credentials should be specified
     * @return the {@link org.apache.commons.httpclient.methods.GetMethod}
     *         instance used to access the URI
     * @throws IOException on error
     */
    public static GetMethod httpGet(String uri, Credentials credentials) throws IOException
    {
        HttpClient client = new HttpClient();


        if (credentials != null)
        {
            client.getState().setCredentials(AuthScope.ANY, credentials);
            client.getParams().setAuthenticationPreemptive(true); // our Basic authentication does not challenge.
        }

        GetMethod get = new GetMethod(uri);
        int status = client.executeMethod(get);
        if (status != HttpStatus.SC_OK)
        {
            throw new RuntimeException("Get request returned status '" + status + "'");
        }

        return get;
    }

    private static void releaseConnection(GetMethod get)
    {
        if (get != null)
        {
            get.releaseConnection();
        }
    }

    /**
     * Waits for the pop-down status pane to appear with the given message.
     *
     * @param browser browser pointing at pulse
     * @param message message to wait for
     */
    public static void waitForStatus(final SeleniumBrowser browser, String message)
    {
        browser.waitForElement(IDs.STATUS_MESSAGE, STATUS_TIMEOUT);
        waitForCondition(new Condition()
        {
            public boolean satisfied()
            {
                return StringUtils.stringSet(browser.getText(IDs.STATUS_MESSAGE));
            }
        }, STATUS_TIMEOUT, "status message to be set.");

        String text = browser.getText(IDs.STATUS_MESSAGE);
        assertThat(text, containsString(message));
    }

    /**
     * Sets the value of an Ext combo box with a given component id.
     *
     * @param browser selenium instance
     * @param comboId component id of the combo
     * @param value   value to set the combo to
     */
    public static void setComboByValue(SeleniumBrowser browser, String comboId, String value)
    {
        String indexExpression;
        // Annoyingly ext stores can't find the empty string value...
        if (StringUtils.stringSet(value))
        {
            indexExpression = "store.find(store.fields.first().name, '" + StringUtil.javaScriptStringEnc(value) + "')";
        }
        else
        {
            indexExpression = "0";
        }

        browser.evalExpression(
                "var combo = selenium.browserbot.getCurrentWindow().Ext.getCmp('" + comboId + "');" +
                "combo.setValue('" + StringUtil.javaScriptStringEnc(value) + "');" +
                "var store = combo.getStore();" +
                "combo.fireEvent('select', combo, store.getAt(" + indexExpression + "));"
        );
    }

    /**
     * Retrieves the available options in the Ext combo box with the given
     * component id.
     * 
     * @param browser selenium instance
     * @param comboId component id of the combo
     * @return available options in the combo
     */
    public static String[] getComboOptions(SeleniumBrowser browser, String comboId)
    {
        String js = "var result = function() { " +
                        "var combo = selenium.browserbot.getCurrentWindow().Ext.getCmp('" + comboId + "'); " +
                        "var values = []; " +
                        "combo.store.each(function(r) { values.push(r.get(combo.valueField)); }); " +
                        "return values; " +
                    "}(); " +
                    "result";
        return browser.evalExpression(js).split(",");
    }

    /**
     * Retrieves the displayed strings for available options in the Ext combo
     * box with the given component id.
     *
     * @param browser selenium instance
     * @param comboId component id of the combo
     * @return displayed strings for available options in the combo
     */
    public static String[] getComboDisplays(SeleniumBrowser browser, String comboId)
    {
        String js = "var result = function() { " +
                        "var combo = selenium.browserbot.getCurrentWindow().Ext.getCmp('" + comboId + "'); " +
                        "var values = []; " +
                        "combo.store.each(function(r) { values.push(r.get(combo.displayField)); }); " +
                        "return values; " +
                    "}(); " +
                    "result";
        return browser.evalExpression(js).split(",");
    }

    /**
     * Creates a test plugin jar with the given id and name in the given
     * directory.
     * 
     * @param tmpDir temporary directory within which to create the plugin jar
     * @param id     id of the plugin to create (should be unique to the test)
     * @param name   name of the plugin to create (should be unique to the test)
     * @return the location of the plugin jar file
     * @throws IOException on error reading from the prototype plugin or
     *                     writing to the new plugin
     */
    public static File createTestPlugin(File tmpDir, String id, String name) throws IOException
    {
        File testPlugin = new File(TestUtils.getPulseRoot(), FileSystemUtils.composeFilename("com.zutubi.pulse.acceptance", "src", "test", "misc", PLUGIN_ID_TEST + ".jar"));
        File unzipDir = new File(tmpDir, "unzip");
        PulseZipUtils.extractZip(testPlugin, unzipDir);

        rewritePluginManifest(id, name, unzipDir);
        rewritePluginXml(id, unzipDir);

        File pluginJarFile = new File(tmpDir, id + ".jar");
        PulseZipUtils.createZip(pluginJarFile, unzipDir, null);
        return pluginJarFile;
    }

    private static void rewritePluginManifest(String id, String name, File unzipDir) throws IOException
    {
        File manifestFile = new File(unzipDir, FileSystemUtils.composeFilename("META-INF", "MANIFEST.MF"));
        String manifest = IOUtils.fileToString(manifestFile);
        manifest = manifest.replaceAll(PLUGIN_ID_TEST, id);
        manifest = manifest.replaceAll("Test Post-Processor", name);
        FileSystemUtils.createFile(manifestFile, manifest);
    }

    private static void rewritePluginXml(String id, File unzipDir) throws IOException
    {
        File pluginXmlFile = new File(unzipDir, "plugin.xml");
        String xml = IOUtils.fileToString(pluginXmlFile);
        xml = xml.replaceAll("test\\.pp", id + ".pp");
        FileSystemUtils.createFile(pluginXmlFile, xml);
    }
}
