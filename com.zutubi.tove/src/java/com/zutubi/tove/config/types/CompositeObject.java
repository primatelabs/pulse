package com.zutubi.tove.config.types;

import com.zutubi.tove.annotations.ID;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;

import java.util.List;
import java.util.Map;

/**
 */
@SymbolicName("Composite")
public class CompositeObject extends AbstractConfiguration
{
    @ID
    private String strA;
    private String strB;
    private SimpleObject simple;

    private List<String> list;
    private Map<String, SimpleObject> map;

    public String getStrA()
    {
        return strA;
    }

    public void setStrA(String strA)
    {
        this.strA = strA;
    }

    public String getStrB()
    {
        return strB;
    }

    public void setStrB(String strB)
    {
        this.strB = strB;
    }

    public SimpleObject getSimple()
    {
        return simple;
    }

    public void setSimple(SimpleObject simple)
    {
        this.simple = simple;
    }

    public List<String> getList()
    {
        return list;
    }

    public void setList(List<String> list)
    {
        this.list = list;
    }

    public Map<String, SimpleObject> getMap()
    {
        return map;
    }

    public void setMap(Map<String, SimpleObject> map)
    {
        this.map = map;
    }
}
