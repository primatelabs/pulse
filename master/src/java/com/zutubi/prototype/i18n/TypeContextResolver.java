package com.zutubi.prototype.i18n;

import com.zutubi.i18n.context.ContextResolver;
import com.zutubi.i18n.context.ExtendedClassContextResolver;
import com.zutubi.i18n.context.ExtendedClassContext;
import com.zutubi.prototype.type.Type;
import com.zutubi.prototype.type.CollectionType;

/**
 *
 *
 */
public class TypeContextResolver implements ContextResolver<TypeContext>
{
    public String[] resolve(TypeContext context)
    {
        Type type = context.getType();
        if (type instanceof CollectionType)
        {
            type = type.getTargetType();
        }
        Class clazz = type.getClazz();

        ContextResolver<ExtendedClassContext> resolver = new ExtendedClassContextResolver();
        return resolver.resolve(new ExtendedClassContext(clazz));
    }

    public Class<TypeContext> getContextType()
    {
        return TypeContext.class;
    }
}
