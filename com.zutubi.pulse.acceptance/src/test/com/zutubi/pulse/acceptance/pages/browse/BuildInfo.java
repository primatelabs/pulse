package com.zutubi.pulse.acceptance.pages.browse;

import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.util.StringUtils;

import java.util.Map;

/**
 * Holds information about a summarised build.
 */
public class BuildInfo
{
    public String project;
    public int number;
    public ResultState status;
    public String revision;

    public BuildInfo(int number, ResultState status, String revision)
    {
        this(null, number, status, revision);
    }

    public BuildInfo(String project, int number, ResultState status, String revision)
    {
        this.project = project;
        this.number = number;
        this.status = status;
        this.revision = revision;
    }

    public BuildInfo(Map<String, String> row)
    {
        project = row.get("project");
        number = Integer.parseInt(StringUtils.stripPrefix(row.get("number"), "build "));
        status = ResultState.fromPrettyString(row.get("status"));
        revision = row.get("revision");
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        BuildInfo buildInfo = (BuildInfo) o;

        if (number != buildInfo.number)
        {
            return false;
        }
        if (project != null ? !project.equals(buildInfo.project) : buildInfo.project != null)
        {
            return false;
        }
        if (revision != null ? !revision.equals(buildInfo.revision) : buildInfo.revision != null)
        {
            return false;
        }
        if (status != buildInfo.status)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = project != null ? project.hashCode() : 0;
        result = 31 * result + number;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "BuildInfo{" +
                "project=" + project +
                ", number=" + number +
                ", status=" + status +
                ", revision='" + revision + '\'' +
                '}';
    }
}
