# Single Sign-on for Camunda BPM Webapp on Wildfly/JBoss AS7 (Container-based Authentication)

This project adds Single Sign On (SSO) support to the [Camunda BPM Webapp](https://docs.camunda.org/manual/latest/webapps/), which contains Tasklist, Cockpit and Admin.

As a particular example, this project shows how to do SSO with keycloak and Wildfly.

## Documentation

### Problem

The Camunda BPM Webapp has to be secured.

### Business Requirements

* Login once via a SSO mechanism
* Security has to be implemented by standard JBoss configuration

### Technical Requirements

* Keycloak, or use the Dockerfile provided in keycloak-demo-server/Dockerfile
* JBoss AS7 / Widlfly / EAP

### Solution

* Add Keycloak openId connect adapter to JBoss
* Configure Camunda-Webapp

### Acceptance tests

* Keycloak user can login into CamundaBPM
* All GUIs and REST APIs are only accessible via Keycloak Login

## Get started

### Configuration Keycloak

We need a keycloak with configured domain (for example 'demo') and users/roles for camunda.

You can use the Dockerfile located in keycloak-demo-server/, this starts a keycloak server with preconfigured domain 'demo' and a user 'demo' with password 'notdemo' who is in roles 'camunda-admin' and 'management', start it for example with
```{r, engine='bash', count_lines}
cd keycloak-demo-server
docker build -t keycloak-demo-server .
docker run --rm -d -p 8081:8080 keycloak-demo-server
```

### JBoss Configuration

Follow the instructions in [Keycloak Manual](https://www.keycloak.org/docs/4.3/securing_apps/index.html#jboss-eap-wildfly-adapter).
It suffices to download and extract the adapter and call the appropriate jboss-cli script.

### Camunda Webapp Configuration
For getting the authentication and authorization from Keycloak, it is needed to modify the camunda webapp.

We need to:

- modify web.xml to point to our custom authorization provider and add security constraints
- modify web.xml to configure login by KEYCLOAK
- adding keycloak.json to make the adapter connect to the right server/domain

All that can be found in [Assembly scripts](src/assembly/)

## Testing

Try to log in into Camunda Webapp.
You will be redirected to the keycloak login page, login with demo/notdemo.
Now you are logged in as user demo with the Camunda groups corresponding to the keycloak roles.

## Maintainer

- Ragnar Nevries (ragnar.nevries@camunda.com)

## License

Apache License, Version 2.0
