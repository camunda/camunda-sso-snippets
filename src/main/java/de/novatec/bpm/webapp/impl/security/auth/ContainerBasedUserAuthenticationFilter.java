package de.novatec.bpm.webapp.impl.security.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.camunda.bpm.webapp.impl.security.SecurityActions;
import org.camunda.bpm.webapp.impl.security.SecurityActions.SecurityAction;
import org.camunda.bpm.webapp.impl.security.auth.Authentication;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.camunda.bpm.webapp.impl.security.auth.Authentications;
import org.camunda.bpm.webapp.impl.security.auth.UserAuthenticationResource;

/**
 * This Servlet filter relies on the Servlet container (application server) to
 * authenticate a user and only forward a request to the application upon
 * successful authentication.
 * 
 * It passes the username provided by the container through the Servlet API into
 * the Servlet session used by the Camunda REST API.
 *
 * The implementation is largely based on code from {@link AuthenticationFilter}
 * and {@link UserAuthenticationResource}.
 *
 * @author Eberhard Heber
 * @author Falko Menge
 */
public class ContainerBasedUserAuthenticationFilter extends AuthenticationFilter {

    protected static final String APP_MARK = "/app/";

    private final Logger LOGGER = Logger.getLogger(ContainerBasedUserAuthenticationFilter.class.getName());

    protected void setKnownPrinicipal(final HttpServletRequest request, Authentications authentications) {
      String username = getUserName(request);
      if (username != null && !username.isEmpty()) {
        for (Authentication auth : authentications.getAuthentications()) {
          if (username.equals(auth.getName())) {
            // already in the list - nothing to do
            LOGGER.fine(request.getSession().getId() + " already authorized.");
            return;
          }
        }
        String engineName = getEngineName(request);
        
        doLogin(authentications, username, engineName);
        
        LOGGER.fine(request.getSession().getId() + " " + username + " " + engineName);
      } else {
        LOGGER.fine(request.getSession().getId() + " no user provided from application server!");
      }
    }

    protected void doLogin(Authentications authentications, String username, String engineName) {
      new ContainerBasedUserAuthenticationResource().doLogin(engineName, username, authentications);
    }

    protected String getUserName(final HttpServletRequest request) {
      Principal principal = request.getUserPrincipal();
      return principal != null ? principal.getName() : null;
    }

    protected String getEngineName(final HttpServletRequest request) {
      String url = request.getRequestURL().toString();
      String[] appInfo = getAppInfo(url);
      return getEngineName(appInfo);
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
        setKnownPrinicipal(req, authentications);
        Authentications.setCurrent(authentications);
        try {

            SecurityActions.runWithAuthentications(new SecurityAction<Void>() {
                public Void execute() throws IOException, ServletException {
                    chain.doFilter(request, response);
                    return null;
                }
            }, authentications);
        } finally {
            Authentications.clearCurrent();
            Authentications.updateSession(req.getSession(), authentications);
        }

    }

}
