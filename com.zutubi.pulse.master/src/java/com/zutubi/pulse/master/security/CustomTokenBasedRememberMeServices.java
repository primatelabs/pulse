package com.zutubi.pulse.master.security;

import org.mortbay.http.HttpOnlyCookie;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A small extension of Acegi's remember me implementation that sets HttpOnly and Secure flags on
 * the remember me cookie as required.
 */
public class CustomTokenBasedRememberMeServices extends TokenBasedRememberMeServices
{
    private String getCookiePath(HttpServletRequest request)
    {
        String contextPath = request.getContextPath();
        return contextPath.length() > 0 ? contextPath : "/";
    }

    @Override
    protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response)
    {
        String cookieValue = encodeCookie(tokens);
        Cookie cookie = new HttpOnlyCookie(getCookieName(), cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath(getCookiePath(request));
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }
}
