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
//                    System.out.println(((HttpServletRequest) request).getSession().getId() + " already authorized.");
                    return;
                }
            }
            String url = req.getRequestURL().toString();
            String[] appInfo = getAppInfo(url);

            String engineName = getEngineName(appInfo);
            String username = principal.getName();

            new ContainerBasedUserAuthenticationResource().doLogin(engineName, username, authentications);
            
//            System.out.println(((HttpServletRequest) request).getSession().getId() + " " + username + " " + engineName);
//        } else {
//          System.out.println(((HttpServletRequest) request).getSession().getId() + " no user provided from application server!");
        }
    }

    protected String getEngineName(String[] appInfo) {
        if (appInfo != null && appInfo.length >= 2) {
          return appInfo[1];
        } else {
          return "default";
        }
    }

    /**
     * Retrieve app name and engine name from URL,
     * e.g. http://localhost:8080/camunda/app/tasklist/default/
     * 
     * TODO detect engine name for API calls,
     * e.g. http://localhost:8080/camunda/api/engine/engine/default/process-definition
     * or http://localhost:8080/camunda/api/cockpit/plugin/base/default/process-definition/invoice:2:b613aca2-71ed-11e7-8f37-0242d5fdf76e/called-process-definitions
     * 
     * Currently, API requests will always be authorized using the
     * process engine named "default". 
     */
    protected String[] getAppInfo(String url) {
        String[] appInfo = null;
        int index = url.indexOf(APP_MARK);
        if (index >= 0) {
          try {
            String apps = url.substring(index + APP_MARK.length(), url.length() - 1);
            String[] aa = apps.split("/");
            if (aa.length >= 1) {
              if (url.endsWith("/")) {
                appInfo = aa;
              } else {
                appInfo = new String[]{aa[0]};
              }
            }
          } catch (StringIndexOutOfBoundsException e) {
            
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
