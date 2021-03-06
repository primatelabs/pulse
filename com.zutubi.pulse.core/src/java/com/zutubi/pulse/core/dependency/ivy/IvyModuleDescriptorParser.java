package com.zutubi.pulse.core.dependency.ivy;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;

import java.net.URL;
import java.text.ParseException;
import java.io.IOException;

/**
 * The IvyModuleDescriptorParser is a wrapper around Ivy's parsers
 * that provides synchronisation.  The ivy parser uses unsynchronised
 * access to a static instance of a SimpleDateFormat.
 */
public class IvyModuleDescriptorParser
{
    private static final Object lock = new Object();

    /**
     * @see ModuleDescriptorParser#parseDescriptor(ParserSettings, java.net.URL, boolean)
     */
    @SuppressWarnings({"JavaDoc"})
    public static ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, boolean validate) throws ParseException, IOException
    {
        return parseDescriptor(ivySettings, descriptorURL, new URLResource(descriptorURL), validate);
    }

    /**
     * @see ModuleDescriptorParser#parseDescriptor(ParserSettings, java.net.URL, Resource, boolean)
     */
    @SuppressWarnings({"JavaDoc"})
    public static ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, Resource res, boolean validate) throws ParseException, IOException
    {
        // serialise access to the underlying ivy parser as it is not thread safe.
        synchronized (lock)
        {
            ModuleDescriptorParser delegateParser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
            return delegateParser.parseDescriptor(ivySettings, descriptorURL, res, validate);
        }
    }

}
