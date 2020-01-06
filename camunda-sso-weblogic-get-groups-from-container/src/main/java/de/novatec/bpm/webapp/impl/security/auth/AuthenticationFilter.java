package de.novatec.bpm.webapp.impl.security.auth;

import static org.camunda.bpm.engine.authorization.Permissions.ACCESS;
import static org.camunda.bpm.engine.authorization.Resources.APPLICATION;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.*;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.camunda.bpm.cockpit.Cockpit;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.spi.ProcessEngineProvider;
import org.camunda.bpm.webapp.impl.security.SecurityActions;
import org.camunda.bpm.webapp.impl.security.SecurityActions.SecurityAction;
import org.camunda.bpm.webapp.impl.security.auth.Authentication;
import org.camunda.bpm.webapp.impl.security.auth.Authentications;
import org.camunda.bpm.webapp.impl.security.auth.UserAuthentication;

/**
 * This security filter maps the user to the camunda user and group management
 *
 * @author Eberhard Heber
 */
public class AuthenticationFilter implements Filter {

    private static final String[] APPS = new String[] { "cockpit", "tasklist" };
    private static final String APP_MARK = "/app/";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    protected ProcessEngine lookupProcessEngine(String engineName) {

        ServiceLoader<ProcessEngineProvider> serviceLoader = ServiceLoader.load(ProcessEngineProvider.class);
        Iterator<ProcessEngineProvider> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            ProcessEngineProvider provider = iterator.next();
            return provider.getProcessEngine(engineName);

        }
        return null;

    }

    protected boolean isAuthorizedForApp(AuthorizationService authorizationService, String username, List<String> groupIds, String application) {
        return authorizationService.isUserAuthorized(username, groupIds, ACCESS, APPLICATION, application);
    }

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

                final ProcessEngine processEngine = lookupProcessEngine(engineName);
                if (processEngine != null) {
                    String username = principal.getName();
                    // throw new InvalidRequestException(Status.BAD_REQUEST,
                    // "Process engine with name "+engineName+" does not exist");
                    // get user's groups
                    AccessControlContext acc = AccessController.getContext();
                    Subject subject = Subject.getSubject(acc);
                    Set<Principal> groupPrincipals = subject.getPrincipals();
                    // transform into array of strings:
                    List<String> groupIds = new ArrayList<String>();
                    for (Principal groupPrincipal : groupPrincipals) {
                        groupIds.add(groupPrincipal.getName());
                    }

                    // check user's app authorizations
                    AuthorizationService authorizationService = processEngine.getAuthorizationService();
                    HashSet<String> authorizedApps = new HashSet<String>();
                    for (String application : APPS) {
                        if (isAuthorizedForApp(authorizationService, username, groupIds, application)) {
                            authorizedApps.add(application);
                        }
                    }
                    authorizedApps.add("admin");
                    if (authorizedApps.contains(appName)) {
                        UserAuthentication newAuthentication = new UserAuthentication(username, engineName);
                        newAuthentication.setGroupIds(groupIds);
                        newAuthentication.setAuthorizedApps(authorizedApps);

                        authentications.addAuthentication(newAuthentication);
                    }
                }
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

    protected void clearProcessEngineAuthentications(Authentications authentications) {
        for (Authentication authentication : authentications.getAuthentications()) {
            ProcessEngine processEngine = Cockpit.getProcessEngine(authentication.getProcessEngineName());
            if (processEngine != null) {
                processEngine.getIdentityService().clearAuthentication();
            }
        }
    }

    @Override
    public void destroy() {

    }
}
