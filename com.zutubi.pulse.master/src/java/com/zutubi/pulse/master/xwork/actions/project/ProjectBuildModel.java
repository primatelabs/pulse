package com.zutubi.pulse.master.xwork.actions.project;

import com.opensymphony.util.TextUtils;
import static com.opensymphony.util.TextUtils.htmlEncode;
import com.opensymphony.xwork.ActionContext;
import com.zutubi.pulse.core.ReferenceResolver;
import com.zutubi.pulse.core.ResolutionException;
import com.zutubi.pulse.core.engine.api.HashReferenceMap;
import com.zutubi.pulse.core.engine.api.Property;
import com.zutubi.pulse.core.engine.api.ReferenceMap;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.model.TestResultSummary;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.changeviewer.ChangeViewerConfiguration;
import com.zutubi.pulse.master.tove.config.user.ProjectsSummaryConfiguration;
import com.zutubi.pulse.master.tove.webwork.ToveUtils;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.util.CollectionUtils;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.util.Mapping;
import com.zutubi.util.Pair;
import com.zutubi.util.StringUtils;
import flexjson.JSON;

import java.util.LinkedList;
import java.util.List;

/**
 * JSON-encodable object representing a single build result.  The configurable
 * build columns are pre-rendered as HTML, as an optimisation (less rendering
 * time on the client side, and significantly simpler data format).
 */
public class ProjectBuildModel
{
    private static final int MAX_COMMENT_LENGTH = 60;

    private static final String LABEL_REMAINING = "remaining";
    private static final String LABEL_TIME = "time";

    private static final String REASON_NONE = "none";
    private static final String REVISION_PERSONAL = "personal";
    private static final String REVISION_NONE = "none";
    private static final String TESTS_NONE = "none";
    private static final String VERSION_NONE = "none";

    private long number;
    private ResultState state;
    private String status;
    private String statusIcon;
    private String responsibleMessage;
    private String responsibleComment;
    private List<String> columns = new LinkedList<String>();

    public ProjectBuildModel(final BuildResult buildResult, User loggedInUser, ProjectsSummaryConfiguration configuration, final Urls urls)
    {
        number = buildResult.getNumber();
        state = buildResult.getState();
        status = buildResult.getState().getPrettyString();
        statusIcon = ToveUtils.getStatusIcon(buildResult);

        BuildResponsibility responsibility = buildResult.getResponsibility();
        if (responsibility != null)
        {
            responsibleMessage = responsibility.getMessage(loggedInUser);
            responsibleComment = StringUtils.trimmedString(responsibility.getComment(), MAX_COMMENT_LENGTH);
        }

        columns = CollectionUtils.map(configuration.getColumns(), new Mapping<String, String>()
        {
            public String map(String column)
            {
                return renderColumn(buildResult, column, urls);
            }
        });
    }

    public long getNumber()
    {
        return number;
    }

    @JSON(include = false)
    public ResultState getState()
    {
        return state;
    }

    public String getStatus()
    {
        return status;
    }

    public String getStatusIcon()
    {
        return statusIcon;
    }

    public String getResponsibleMessage()
    {
        return responsibleMessage;
    }

    public String getResponsibleComment()
    {
        return responsibleComment;
    }

    @JSON
    public List<String> getColumns()
    {
        return columns;
    }

    private String renderColumn(BuildResult buildResult, String column, Urls urls)
    {
        String label = column;
        String content;
        if (column.equals(BuildColumns.KEY_VERSION))
        {
            String version = buildResult.getVersion();
            if (!TextUtils.stringSet(version))
            {
                version = VERSION_NONE;
            }

            content = htmlEncode(version);
        }
        else if (column.equals(BuildColumns.KEY_ERRORS))
        {
            content = Integer.toString(buildResult.getErrorFeatureCount());
        }
        else if (column.equals(BuildColumns.KEY_REASON))
        {
            BuildReason reason = buildResult.getReason();
            if (reason == null)
            {
                content = REASON_NONE;
            }
            else
            {
                content = htmlEncode(reason.getSummary());
            }
        }
        else if (column.equals(BuildColumns.KEY_REVISION))
        {
            if (buildResult.isPersonal())
            {
                content = REVISION_PERSONAL;
            }
            else
            {
                Revision revision = buildResult.getRevision();
                if (revision == null)
                {
                    content = REVISION_NONE;
                }
                else
                {
                    ChangeViewerConfiguration changeViewer = buildResult.getProject().getConfig().getChangeViewer();
                    String revisionString = renderRevisionString(revision);
                    if (changeViewer == null)
                    {
                        content = revisionString;
                    }
                    else
                    {
                        content = link(revisionString, changeViewer.getRevisionURL(revision));
                    }
                }
            }
        }
        else if (column.equals(BuildColumns.KEY_ELAPSED))
        {
            if (buildResult.completed())
            {
                label = LABEL_TIME;
                content = buildResult.getStamps().getPrettyElapsed();
            }
            else
            {
                label = LABEL_REMAINING;
                content = buildResult.getStamps().getPrettyEstimatedTimeRemaining();
            }
        }
        else if (column.equals(BuildColumns.KEY_WHEN))
        {
            content = merge("<a href='#' class='unadorned' title='${date}' onclick=\\\"toggleDisplay('${timeId}'); toggleDisplay('${dateId}'); return false;\\\">" +
                                "<img alt='toggle format' src='${base}images/calendar.gif'/>" +
                            "</a> " +
                            "<span id='${timeId}'>${time}</span>" +
                            "<span id='${dateId}' style='display: none'>${date}</span>",
                    asPair("base", urls.base()),
                    asPair("timeId", "time." + buildResult.getId()),
                    asPair("dateId", "date." + buildResult.getId()),
                    asPair("date", buildResult.getStamps().getPrettyStartDate(ActionContext.getContext().getLocale())),
                    asPair("time", buildResult.getStamps().getPrettyStartTime()));
        }
        else if (column.equals(BuildColumns.KEY_TESTS))
        {
            TestResultSummary summary = buildResult.getTestSummary();
            if (summary == null || summary.getTotal() == 0)
            {
                content = TESTS_NONE;
            }
            else
            {
                if (summary.hasBroken())
                {
                    content = Integer.toString(summary.getBroken()) + " of " + summary.getTotal() + " broken";
                }
                else
                {
                    content = Integer.toString(summary.getTotal()) + " passed";
                }

                if (summary.hasSkipped())
                {
                    content += " (" + summary.getSkipped() + " skipped)";
                }

                content = link(content, urls.buildTests(buildResult));
            }
        }
        else if (column.equals(BuildColumns.KEY_WARNINGS))
        {
            content = Integer.toString(buildResult.getWarningFeatureCount());
        }
        else
        {
            content = "unknown";
        }

        return label + ": " + content;
    }

    private String renderRevisionString(Revision revision)
    {
        if (revision.isAbbreviated())
        {
            return merge("<span title='${rev}'>${shortRev}</span>",
                    asPair("rev", htmlEncode(revision.getRevisionString())),
                    asPair("shortRev", htmlEncode(revision.getAbbreviatedRevisionString())));
        }
        else
        {
            return htmlEncode(revision.getRevisionString());
        }
    }

    private String merge(String template, Pair<String, String>... properties)
    {
        ReferenceMap referenceMap = new HashReferenceMap();
        for (Pair<String, String> p: properties)
        {
            referenceMap.add(new Property(p.first, p.second));
        }

        try
        {
            return ReferenceResolver.resolveReferences(template, referenceMap, ReferenceResolver.ResolutionStrategy.RESOLVE_NON_STRICT);
        }
        catch (ResolutionException e)
        {
            // Never happens
            return template;
        }
    }

    private String link(String content, String url)
    {
        return merge("<a href='${url}'>${content}</a>", asPair("content", content), asPair("url", url));
    }
}