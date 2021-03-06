package com.zutubi.pulse.master.security;

import com.zutubi.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A text property converter to allow easy configuration of the authority definitions
 * within a spring context file.
 */
public class AuthorityDefinitionsEditor  extends PropertyEditorSupport
{
    public void setAsText(String text) throws IllegalArgumentException
    {
        AuthorityDefinitions instance = new AuthorityDefinitions();
        try
        {
            text = text.trim();
            BufferedReader reader = new BufferedReader(new StringReader(text));
            String line;
            while (StringUtils.stringSet(line = reader.readLine()))
            {
                StringTokenizer tokens = new StringTokenizer(line, ",", false);
                String path = tokens.nextToken().trim();
                String role = tokens.nextToken().trim();
                List<String> methods = new LinkedList<String>();
                while (tokens.hasMoreTokens())
                {
                    methods.add(tokens.nextToken().trim());
                }
                instance.addPrivilege(path, role, methods.toArray(new String[methods.size()]));
            }
        }
        catch (IOException e)
        {
            // this will not happen.
        }

        setValue(instance);
    }
}

