package com.zutubi.tove.type;

/**
 */
public class TemplatedMapType extends MapType
{
    public TemplatedMapType(Type collectionType, TypeRegistry typeRegistry) throws TypeException
    {
        super(collectionType, typeRegistry);
    }

    public boolean isTemplated()
    {
        return true;
    }
}
