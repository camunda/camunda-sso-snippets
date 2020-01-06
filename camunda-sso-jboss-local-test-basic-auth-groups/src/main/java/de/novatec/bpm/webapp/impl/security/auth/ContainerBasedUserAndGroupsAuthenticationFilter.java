package de.novatec.bpm.webapp.impl.security.auth;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.camunda.bpm.webapp.impl.security.auth.Authentications;

/**
 * This Servlet filter relies on the Servlet container (application server) to
 * authenticate a user and only forward a request to the application upon
 * successful authentication.
 *
 * In addition this variation expects the container to also look up the groups.
 *
 * The filter passes the username and groups provided by the container through
 * the Servlet API into the Servlet session used by the Camunda REST API.
 *
 * @author Falko Menge
 */
public class ContainerBasedUserAndGroupsAuthenticationFilter extends ContainerBasedUserAuthenticationFilter {

  @Override
  protected void doLogin(Authentications authentications, String username, String engineName) {
    // initialize with null, to allow fall back to identity service, if nothing provided by container
    List<String> groupIds = null;

    // get user's groups
    AccessControlContext acc = AccessController.getContext();
    Subject subject = Subject.getSubject(acc);
    if (subject != null) {
      Set<Principal> groupPrincipals = subject.getPrincipals();
      if (groupPrincipals != null && !groupPrincipals.isEmpty()) {
        // transform into array of strings:
        groupIds = new ArrayList<String>();
        for (Principal groupPrincipal : groupPrincipals) {
          groupIds.add(groupPrincipal.getName());
        }
      }
    }

    new ContainerBasedUserAuthenticationResource().doLogin(engineName, username, authentications, groupIds);
  }

}
