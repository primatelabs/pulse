package com.zutubi.pulse.master.xwork.actions.project;

import com.google.common.base.Function;
import com.zutubi.pulse.master.dependency.DependencyGraphData;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.util.EnumUtils;
import com.zutubi.util.StringUtils;
import com.zutubi.util.WebUtils;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Holds data for a cell in a dependency grid.  This data contains the
 * necessary information to render the cell in an HTML table.  Instances should
 * be created using the make* factory methods.
 */
public class ProjectDependencyData
{
    public static final String CLASS_BOX           = "gbox";
    public static final String CLASS_BOTTOM_BORDER = "gbottom";
    public static final String CLASS_LEFT_BORDER   = "gleft";
    public static final String CLASS_RIGHT_BORDER  = "gright";
    public static final String CLASS_HIGHLIGHTED   = "ghighlighted";
    public static final String CLASS_UNDERSTATED   = "gunderstated";

    private Project project;
    private ProjectHealth health;
    private String url;
    private int rowspan = 1;
    private boolean dead = false;
    private List<String> classes = new LinkedList<String>();

    private ProjectDependencyData()
    {
        // Use the make* factory methods
    }

    /**
     * Makes data for a cell holding a project.  These are the boxes in the
     * diagram.  As boxes occupy two rows, the cells directly below them should
     * be marked as dead.
     *
     * @param data   data about the project that the cell displays
     * @param health health of the project that the cell displays
     * @return data for a box cell displaying the given project
     */
    public static ProjectDependencyData makeBox(DependencyGraphData data, ProjectHealth health, String url, boolean root)
    {
        ProjectDependencyData dd = new ProjectDependencyData();
        dd.project = data.getProject();
        dd.health = health;
        dd.url = url;
        dd.classes.add(CLASS_BOX);
        if (root)
        {
            dd.classes.add(CLASS_HIGHLIGHTED);
        }

        if (data.isSubtreeFiltered())
        {
            dd.classes.add(CLASS_UNDERSTATED);
        }

        dd.rowspan = 2;
        return dd;
    }

    /**
     * Makes data for a dead cell.  This is a cell that is not rendered as it
     * is superceded by the rowspan of another cell.
     *
     * @return data for a cell that should not be rendered into the table
     */
    public static ProjectDependencyData makeDead()
    {
        ProjectDependencyData dd = new ProjectDependencyData();
        dd.dead = true;
        return dd;
    }

    /**
     * Makes data for a cell that has some borders turned on.
     *
     * @param right  if true, the cell will have a right hand border
     * @param bottom if true, the cell will have a bottom border
     * @return data for a cell with some borders
     */
    public static ProjectDependencyData makeBordered(boolean right, boolean bottom)
    {
        ProjectDependencyData dd = new ProjectDependencyData();
        if (right)
        {
            dd.classes.add(CLASS_RIGHT_BORDER);
        }

        if (bottom)
        {
            dd.classes.add(CLASS_BOTTOM_BORDER);
        }

        return dd;
    }

    /**
     * Horizontally flips a cell.  In effect, switches any right borders to
     * left and vice-versa.
     */
    public void flipHorizontal()
    {
        classes = newArrayList(transform(classes, new Function<String, String>()
        {
            public String apply(String s)
            {
                if (s.equals(CLASS_LEFT_BORDER))
                {
                    return CLASS_RIGHT_BORDER;
                }
                else if (s.equals(CLASS_RIGHT_BORDER))
                {
                    return CLASS_LEFT_BORDER;
                }

                return s;
            }
        }));
    }

    /**
     * Retrieves the project name for a box cell.
     *
     * @return name of the project represented by this cell, null if this cell
     *         is not a box
     */
    public String getName()
    {
        return project == null ? null : project.getName();
    }

    /**
     * Retrieves the project name for a box cell, converted as safe to use as
     * an HTML name.
     *
     * @return name of the project represented by this cell, converted to a
     *         safe HTML name, null if this cell is not a box
     */
    public String getHtmlName()
    {
        return project == null ? null : WebUtils.toValidHtmlName("a" + project.getName()).substring(1);
    }
    
    /**
     * Retrieves the health of the project for a box cell.
     *
     * @return health of the project represented by this cell, null if this
     *         cell is not a box
     */
    public String getHealth()
    {
        return health == null ? null : EnumUtils.toPrettyString(health);
    }

    /**
     * Retrieves the url the project home page for a box cell.
     *
     * @return url of the project represented by this cell, null if this cell
     *         is not a box
     */
    public String getUrl()
    {
        return url;
    }
    
    /**
     * Retrieves the state of the project for a box cell.
     *
     * @return state of the project represented by this cell, null if this cell
     *         is not a box
     */
    public String getState()
    {
        return project == null ? null : EnumUtils.toPrettyString(project.getState());
    }

    /**
     * Retrieves the rowspan of this cell.
     *
     * @return the number of rows this cell should occupy
     */
    public int getRowspan()
    {
        return rowspan;
    }

    /**
     * Indicates if this cell is superceded by another's rowspan and should not
     * be rendered.
     *
     * @return true if this cell should not be rendered
     */
    public boolean isDead()
    {
        return dead;
    }

    /**
     * Returns the CSS classes that should be applied to this cell in a single
     * string (suitable as the attribute value).
     *
     * @return the CSS classes for this cell
     */
    public String getClasses()
    {
        return StringUtils.join(" ", classes);
    }

    public boolean isRoot()
    {
        return classes.contains(CLASS_HIGHLIGHTED);
    }

    public boolean hasLeftBorder()
    {
        return classes.contains(CLASS_LEFT_BORDER) || classes.contains(CLASS_BOX);
    }

    public boolean hasRightBorder()
    {
        return classes.contains(CLASS_RIGHT_BORDER) || classes.contains(CLASS_BOX);
    }

    public boolean hasTopBorder()
    {
        return classes.contains(CLASS_BOX);
    }

    public boolean hasBottomBorder()
    {
        return classes.contains(CLASS_BOTTOM_BORDER) || classes.contains(CLASS_BOX);
    }
}
