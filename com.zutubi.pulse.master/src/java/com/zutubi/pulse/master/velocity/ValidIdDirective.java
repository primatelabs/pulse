package com.zutubi.pulse.master.velocity;

import com.zutubi.util.WebUtils;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

/**
 * A directive that converts a path to a valid HTML id.  In particular, all
 * characters that are not valid in an id are replaced with a colon (':').
 */
public class ValidIdDirective extends Directive
{
    public String getName()
    {
        return "id";
    }

    public int getType()
    {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        String in = String.valueOf(node.jjtGetChild(0).value(context));
        writer.write(WebUtils.toValidHtmlName(in));
        return true;
    }
}
