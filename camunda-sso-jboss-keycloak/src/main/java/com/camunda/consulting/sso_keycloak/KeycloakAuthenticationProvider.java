package com.camunda.consulting.sso_keycloak;

import java.util.ArrayList;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.keycloak.KeycloakPrincipal;

public class KeycloakAuthenticationProvider implements AuthenticationProvider {

  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
    KeycloakPrincipal<?> principal = (KeycloakPrincipal<?>) request.getUserPrincipal();

    if (principal == null) {
      return AuthenticationResult.unsuccessful();
    }

    String name = principal.getName();
    if (name == null || name.isEmpty()) {
      return AuthenticationResult.unsuccessful();
    }

    Set<String> roles = principal.getKeycloakSecurityContext().getToken().getRealmAccess().getRoles();
    
    AuthenticationResult result = AuthenticationResult.successful(name);
    result.setGroups(new ArrayList<String>(roles));
    return result;
  }

  @Override
  public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
    // TODO Auto-generated method stub

  }

}
