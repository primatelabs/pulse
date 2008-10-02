package com.zutubi.pulse.servlet;

import com.zutubi.pulse.core.spring.SpringComponentContext;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.model.UserManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

/**
 *
 */
public class ProfileServlet extends HttpServlet
{
    // This needs review. Where should this servlet direct a user? Can you view another users
    // profile?

    private static final String DISPLAY_PROFILE_PATH = "/admin/viewUser.action?userId={0}";

    private UserManager userManager;

    public UserManager getUserManager()
    {
        if (userManager == null)
        {
            // a) servlet is not autowired.
            // b) when servlet is initialised, the userManager is not available.
            userManager = (UserManager) SpringComponentContext.getBean("userManager");
        }
        return userManager;
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        String requestPath = httpServletRequest.getPathInfo();
        if (requestPath == null)
        {
            httpServletResponse.sendError(404);
            return;
        }

        if (requestPath.startsWith("/"))
        {
            requestPath = requestPath.substring(1);
        }

        String loginName = requestPath;

        // check the validity of the path.
        User user = getUserManager().getUser(loginName);
        if (user == null)
        {
            httpServletResponse.sendError(404);
            return;
        }

        String pathToForward = MessageFormat.format(DISPLAY_PROFILE_PATH, String.valueOf(user.getId()));

        httpServletRequest.getRequestDispatcher(pathToForward).forward(httpServletRequest, httpServletResponse);
    }

}
