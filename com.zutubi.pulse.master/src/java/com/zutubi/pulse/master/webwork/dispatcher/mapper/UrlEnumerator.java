package com.zutubi.pulse.master.webwork.dispatcher.mapper;

import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;
import com.zutubi.util.StringUtils;
import com.zutubi.util.adt.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple helper class for calculating valid URLs.
 */
public class UrlEnumerator
{
    /**
     * Calculates the valid URLs starting at the base resolver.  The URLs may
     * be infinite, which will be expressed using the * wildcard.
     *
     * @param base resolver to start with
     * @return a list of all valid URLs reachable from the resolver
     */
    public static List<String> enumerate(ActionResolver base)
    {
        List<String> result = new LinkedList<String>();
        process(base, Collections.<Pair<String, Class>>emptyList(), result);
        return result;
    }

    private static void process(ActionResolver resolver, List<Pair<String, Class>> currentPath, List<String> paths)
    {
        for (String element: resolver.listChildren())
        {
            ActionResolver child = resolver.getChild(element);
            List<Pair<String, Class>> pathCopy = new LinkedList<Pair<String, Class>>(currentPath);
            Pair<String, Class> childPair = new Pair<String, Class>(element, child.getClass());
            if (currentPath.contains(childPair))
            {
                // We have repetition.  Go back to where it was, popping
                // incomplete paths on the way so we can add them as a single
                // repeating element.
                String repeatedPart = "";
                Pair<String, Class> pair;
                do
                {
                    pair = pathCopy.remove(pathCopy.size() - 1);
                    repeatedPart = pair.first + "/" + repeatedPart;
                    paths.remove(paths.size() - 1);
                }
                while(pair.second != child.getClass());

                paths.add(collapse(pathCopy) + "/(" + repeatedPart + ")*");
            }
            else
            {
                pathCopy.add(childPair);
                if (resolver.getAction() != null)
                {
                    paths.add(collapse(pathCopy) + "/");
                }
                
                process(child, pathCopy, paths);
            }
        }
    }

    private static String collapse(List<Pair<String, Class>> currentPath)
    {
        return StringUtils.join("/", CollectionUtils.map(currentPath, new Mapping<Pair<String, Class>, String>()
        {
            public String map(Pair<String, Class> pair)
            {
                return pair.first;
            }
        }));
    }
}
