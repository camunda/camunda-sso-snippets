package de.novatec.bpm.webapp.impl.security.auth;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.camunda.bpm.webapp.impl.security.SecurityActions;
import org.camunda.bpm.webapp.impl.security.SecurityActions.SecurityAction;
import org.camunda.bpm.webapp.impl.security.auth.Authentication;
import org.camunda.bpm.webapp.impl.security.auth.Authentications;
import org.camunda.bpm.webapp.impl.security.auth.UserAuthenticationResource;

/**
 * This security filter maps the user provided by the application server
 * to the Camunda user and group management.
 * 
 * It is largely based on code from {@link org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter}
 * and {@link UserAuthenticationResource}. 
 *
 * @author Eberhard Heber
 * @author Falko Menge
 */
public class AuthenticationFilter extends org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter {

    private static final String APP_MARK = "/app/";

    protected void setKnownPrinicipal(final ServletRequest request, Authentications authentications) {
        final HttpServletRequest req = (HttpServletRequest) request;

        Principal principal = req.getUserPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isEmpty()) {
            for (Authentication aut : authentications.getAuthentications()) {
                if (aut.getName() == principal.getName()) {
                    // already in the list - nothing to do
                    return;
                }
            }
            String url = req.getRequestURL().toString();
            String[] appInfo = getAppInfo(url);

            if (appInfo != null) {
                String engineName = getEngineName(appInfo);
                String appName = getAppName(appInfo);

                new ContainerBasedUserAuthenticationResource().doLogin(engineName, username, authentications);
            }
        }

    }

    private String getAppName(String[] appInfo) {
        return appInfo[0];
    }

    private String getEngineName(String[] appInfo) {
        return appInfo[1];
    }

    /**
     * Retrieve app name and engine name from URL,
     * e.g. http://localhost:8080/camunda/app/tasklist/default/
     */
    private String[] getAppInfo(String url) {
        String[] appInfo = null;
        if (url.endsWith("/")) {
            int index = url.indexOf(APP_MARK);
            if (index >= 0) {
                String apps = url.substring(index + APP_MARK.length(), url.length() - 1);
                String[] aa = apps.split("/");
                if (aa.length == 2) {
                    appInfo = aa;
                }
            }
        }
        return appInfo;
    }

    /**
     * This method is a copy of {@link org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     * except for the invocation of {@link #setKnownPrinicipal(ServletRequest, Authentications)}.
     * 
     * It should be kept in sync with the latest version from Camunda,
     * e.g. by doing a diff between the Java files.
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;

        // get authentication from session
        Authentications authentications = Authentications.getFromSession(req.getSession());
        setKnownPrinicipal(request, authentications);
        Authentications.setCurrent(authentications);
        try {

            SecurityActions.runWithAuthentications(new SecurityAction<Void>() {
                public Void execute() {
                    try {
                        chain.doFilter(request, response);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            }, authentications);
        } finally {
            Authentications.clearCurrent();
            Authentications.updateSession(req.getSession(), authentications);
        }

    }

}
