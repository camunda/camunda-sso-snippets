# Single Sign-on for Camunda BPM Webapp on Wildfly/JBoss AS7 (Container-based Authentication)

This project adds Single Sign On (SSO) support to the [Camunda BPM Webapp](https://docs.camunda.org/manual/latest/webapps/), which contains Tasklist, Cockpit and Admin.
Fortunately, application servers can do the actual authentication of a user before a request is forwarded to the application.
The only thing that needs to be done inside the Camunda REST API, is to take the user id and optionally also the group ids provided by the container through the Servlet API and put them into the Servlet session of the REST API.
Thats why we also call this Container-based Authentication.

As a particular example, this project shows how to do SSO with Kerberos/Active Directory and Wildfly.
However, the [Container-based Authentication Filter](src/main/java/de/novatec/bpm/webapp/impl/security/auth/ContainerBasedUserAuthenticationFilter.java)
is only using the standard Servlet and Java Security APIs.
Therefore it works exactly the same on all Servlet containers and with any authentication mechanism supported by the container.
For example the [fork for Single Sign-on on Weblogic](https://github.com/camunda-consulting/camunda-sso-weblogic/) uses the same Java code.

There are two variations of the Authentication Filter:
One [takes the user's groups from the Camunda IdentityService](src/main/java/de/novatec/bpm/webapp/impl/security/auth/ContainerBasedUserAuthenticationFilter.java)
and requires the LDAP plugin or another identity provider.
The other one [takes the groups from the container](src/main/java/de/novatec/bpm/webapp/impl/security/auth/ContainerBasedUserAndGroupsAuthenticationFilter.java)
and leverages, e.g. an LDAP support inside the container. However, it falls back to the Camunda IdentityService of the container doesn't provide groups.

The project also shows how to configure the Camund Webapp in a way that allows for smooth updates to future Camunda BPM versions.
The [config-processor-maven-plugin](https://github.com/lehphyro/maven-config-processor-plugin)
helps to gently modify the original deployment decriptors
[web.xml](src/assembly/web.updates.xml),
[jboss-web.xml](src/assembly/jboss-web.updates.xml)
and [jboss-deployment-structure.xml](src/assembly/jboss-deployment-structure.updates.xml)
provided inside Camunda binary packages.


## Variations (Branches)

This Git repository contains different [branches](https://github.com/camunda/camunda-sso-jboss/branches) with slight variations of the implementation:

- [enterprise-edition](https://github.com/camunda/camunda-sso-jboss/tree/enterprise-edition): uses correct dependencies to build a Camunda Webapp that conatains the Enterprise Edition features
- [local-test-basic-auth](https://github.com/camunda/camunda-sso-jboss/tree/local-test-basic-auth): uses HTTP Basic Authentication for local testing without Kerberos/Active Directory server
- [local-test-basic-auth-ee](https://github.com/camunda/camunda-sso-jboss/tree/local-test-basic-auth-ee): combines both of the above mentioned branches for local testing
- [local-test-basic-auth-groups](https://github.com/camunda/camunda-sso-jboss/tree/local-test-basic-auth-groups): uses HTTP Basic Authentication for local testing of the filter that gets the user's groups from the container
- [keycloak](https://github.com/camunda/camunda-sso-jboss/tree/keycloak): uses keycloak as identity provider

## Documentation

### Problem

The Camunda BPM Webapp has to be secured.

### Business Requirements

* Login once via a SSO mechanism
* Security has to be implemented by standard JBoss configuration

### Technical Requirements

* Windows Active Directory (AD)
* JBoss AS7 / Widlfly / EAP
* AD User and Groups for integration in Camunda
* Dedicated AD User for JBoss Service

### Solution

* Add Kerberos and AD Security-Negotiation to JBoss
* Configure Camunda-Webapp

### Acceptance tests

* AD User can login into CamundaBPM
* All GUIs and REST APIs are only accessible via AD Login

## Get started

### Configuration AD

Have a look at the tutorial [How To Configure Browser-based SSO with Kerberos/SPNEGO and JBoss Application Server](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/how-to-set-up-sso-with-kerberos/how-to-set-up-sso-with-kerberos/)

A fully configured AD is needed, because the authentication needs users and groups. Therefore we use the test users 
eh, TestUser, Administrator and the test group TestGroup. They can be created with the AD standard tooling of a windows 
server.

1. Create an AD User (Example: eh)
2. Create an AD Group (Example: TestGroup)
3. Execute on cmd Line with admin rights:
Create the User Principal: 
```
setspn -A HTTP/nbookeh2.novaDomain.local eh
```
Create the KeyTab file:
```
ktpass /out krb5.keytab /mapuser eh@NOVADOMAIN.LOCAL /princ HTTP/nbookeh2.novaDomain.local@NOVADOMAIN.LOCAL /pass 
    jessica_4321 /kvno 0 /crypto all`
```
### JBoss Configuration

Therefore, we using JBoss AS7/EAP6 is a requirement. So it is needed to add a few configuration lines.
1. Modify the standalone.xml and add your specific AD information!
```xml
<server xmlns="urn:jboss:domain:1.5">
...
    <system-properties>
        <property name="java.security.krb5.realm" value="NOVADOMAIN.LOCAL"/>
        <property name="java.security.krb5.kdc" value="192.168.0.1"/>
        <property name="java.security.auth.login.config" value="C:\login.conf"/>
        <property name="java.security.krb5.conf" value="C:\Windows\krb5.ini"/>
        <property name="sun.security.krb5.debug" value="true"/>
        <property name="jboss.security.disable.secdomain.option" value="true"/>
        <property name="javax.net.debug" value="true"/>
        <property name="java.security.debug" value="true"/>
    </system-properties>
...
    <profile>
    ...
        <subsystem xmlns="urn:jboss:domain:security:1.2">
        ...
            <security-domains>
                <security-domain name="myhost" cache-type="default">
                    <authentication>
                        <login-module code="Kerberos" flag="required">
                            <module-option name="storeKey" value="true"/>
                            <module-option name="useKeyTab" value="true"/>
                            <module-option name="principal" value="HTTP/nbookeh2.novaDomain.local@NOVADOMAIN.LOCAL"/>
                            <module-option name="keyTab" value="C:/krb5.keytab"/>
                            <module-option name="doNotPrompt" value="true"/>
                            <module-option name="refeshKrb5Config" value="false"/>
                            <module-option name="debug" value="true"/>
                        </login-module>
                    </authentication>
                </security-domain>
                <security-domain name="SPNEGO" cache-type="default">
                    <authentication>
                        <login-module code="SPNEGO" flag="requisite">
                            <module-option name="password-stacking" value="useFirstPass"/>
                            <module-option name="serverSecurityDomain" value="myhost"/>
                            <module-option name="removeRealmFromPrincipal" value="true"/>
                        </login-module>
                        <login-module code="AdvancedAdLdap" flag="requisite">
                            <module-option name="password-stacking" value="useFirstPass"/>
                            <module-option name="bindDN" value="cn=Administrator,cn=Users,dc=novaDomain,dc=local"/>
                            <module-option name="bindCredential" value="jessica_4321"/>
                            <module-option name="java.naming.provider.url" value="ldap://vmserver2015.novaDomain.local:389"/>
                            <module-option name="java.naming.referral" value="follow"/>
                            <module-option name="baseCtxDN" value="DC=novaDomain,DC=local"/>
                            <module-option name="baseFilter" value="(cn={0})"/>
                            <module-option name="roleAttributeID" value="memberOf"/>
                            <module-option name="allowEmptyPassword" value="false"/>
                        </login-module>
                    </authentication>
                </security-domain>
            </security-domains>
        </subsystem>
        ...
        <subsystem xmlns="urn:org.camunda.bpm.jboss:1.1">
            <process-engines>
                <process-engine name="default" default="true">
                    <plugins>
                        <plugin>
                            <class>
                                org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin
                            </class>
                            <properties>
                                <property name="serverUrl">
                                   ldap://192.168.0.1:389
                                </property>
                                <property name="managerDn">
                                    cn=Administrator,cn=Users,dc=novaDomain,dc=local
                                </property>
                                <property name="managerPassword">
                                    jessica_4321
                                </property>
                                <property name="baseDn">
                                    dc=novaDomain,dc=local
                                </property>
                                <property name="userSearchBase">
                                    cn=Users
                                </property>
                                <property name="userSearchFilter">
                                   (objectCategory=person)
                                </property>
                                <property name="userIdAttribute">
                                   cn
                                </property>
                                <property name="userFirstnameAttribute">
                                    name
                                </property>
                                <property name="userLastnameAttribute">
                                    sn
                                </property>
                                <property name="userEmailAttribute">
                                    mail
                                </property>
                                <property name="userPasswordAttribute">
                                    userPassword
                                </property>
                                <property name="groupSearchBase">
                                    cn=Users
                                </property>
                                <property name="groupSearchFilter">
                                    (objectCategory=group)
                                </property>
                                <property name="groupIdAttribute">
                                    cn
                                </property>
                                <property name="groupNameAttribute">
                                    name
                                </property>
                                <property name="groupMemberAttribute">
                                    member
                                </property>
                            </properties>
                        </plugin>
                        <plugin>
                            <class>
                                org.camunda.bpm.engine.impl.plugin.AdministratorAuthorizationPlugin
                            </class>
                            <properties>
                                <property name="administratorUserName">
                                    eh
                                </property>
                            </properties>
                        </plugin>
                    </plugins>
                </process-engine>
            </process-engines>
            ...
        </subsystem>
        ...
    </profile>
    ...
</server>
```
2. Create a login.conf and link it in the JBoss property configuration
```
com.sun.security.jgss.login {
    com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true renewTGT=true;
};
com.sun.security.jgss.initiate {
    com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true renewTGT=true;
};
com.sun.security.jgss.accept {
    com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true debug=true renewTGT=true;
};
```
### Camunda Webapp Configuration
For getting the authentication and authorization from AD, it is needed to modify the camunda webapp. There are 3 
configuration steps and a copy step with an adapted security filter of camunda. In this changes, it is important to add 
the specific authorization group to the web.xml.

### Configuration of Windows Kerberos
Kerberos now needs the connection information. This krb5.ini is the standard configuration file of a java kerberos 
client. It is needed to add the specific AD configuration information.
```
[appdefaults]
    validate=false
[domain_realm]
    novaDomain.local = "NOVADOMAIN.LOCAL"
    .novaDomain.local = "NOVADOMAIN.LOCAL"
[libdefaults]
    ticket_lifetime = 600
    default_tkt_enctypes = aes256-cts-hmac-sha1-96 aes256-cts aes128-cts des3-cbc-sha1 rc4-hmac des-cbc-md5 des-cbc-crc
    default_tgt_enctypes = aes256-cts-hmac-sha1-96 aes256-cts aes128-cts des3-cbc-sha1 rc4-hmac des-cbc-md5 des-cbc-crc
    default_tgs_enctypes = aes256-cts-hmac-sha1-96 aes256-cts aes128-cts des3-cbc-sha1 rc4-hmac des-cbc-md5 des-cbc-crc
    dns_lookup_kdc = "true"
    forwardable = true
    default_realm = "NOVADOMAIN.LOCAL"
[logging]
[realms]
    NOVADOMAIN.LOCAL = {
        admin_server = "192.168.0.1"
        kdc = "192.168.0.1"
        }
```
### Modify JRE/JDK
The standard JRE/JDK only supports a standard encryption of 128 bit. For getting more security install the other policys.
Install UnlimitedJCEPolicyJDK7.zip from oracle webpage into the jre/jdk installation dir.

## External Documentation Links

- [EAP 7.0 > How to Configure Server Security > Chapter 2. Securing the Server and Its Interfaces > 2.2.2. Configure the Management Interfaces for HTTPS > Create a keystore to secure the management interfaces](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/paged/how-to-configure-server-security/chapter-2-securing-the-server-and-its-interfaces#create_a_keystore_to_secure_the_management_interfaces)

- [EAP 7.0: How to Set Up SSO with Kerberos](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/how-to-set-up-sso-with-kerberos/how-to-set-up-sso-with-kerberos)

- [EAP 7.0: How to Configure Identity Management](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/paged/how-to-configure-identity-management/)

- [EAP 7.0: Configuration Guide](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform/7.0/paged/configuration-guide/)

- [EAP 7.0](https://access.redhat.com/documentation/en/red-hat-jboss-enterprise-application-platform?version=7.0)

- [JBoss Negotiation Toolkit](https://repository.jboss.org/org/jboss/security/jboss-negotiation-toolkit/)

- [Steps to configure Kerberos / SPNEGO / NTLM authentication with Weblogic Server running on Oracle JDK](https://blogs.oracle.com/blogbypuneeth/entry/configure_kerberos_with_weblogic_server)

- [EAP 7.0: Security Architecture](https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.0/html-single/security_architecture/)
    - > "A security realm is effectively an identity store of usernames, passwords, and group membership information"
    - > "A security domain is a set of Java Authentication and Authorization Service (JAAS) declarative security configurations that one or more applications use to control authentication, authorization, auditing, and mapping."
    - > "Web applications and EJB deployments can only use security domains directly."
    - > "Security mapping adds the ability to combine authentication and authorization information after the authentication or authorization happens but before the information is passed to your application."
    - > "The JBoss EAP security subsystem is actually based on the JAAS API."
- [Oracle Java EE 7 Tutorial: Securing Web Applications](https://docs.oracle.com/javaee/7/tutorial/security-webtier002.htm#GKBAA)
    - > "If there is no authorization constraint, the container must accept the request without requiring user authentication."
    - > "If there is an authorization constraint but no roles are specified within it, the container will not allow access to constrained requests under any circumstances."
    - > "Each role name specified here must either correspond to the role name of one of the security-role elements defined for this web application or be the specially reserved role name *, which indicates all roles in the web application."
    - > "The roles defined for the application must be mapped to users and groups defined on the server, except when default principal-to-role mapping is used."


## Resources

* [Issue Tracker](https://github.com/camunda/camunda-sso-jboss/issues)
* [Roadmap](https://github.com/camunda/camunda-sso-jboss#roadmap)
* [Changelog](https://github.com/camunda/camunda-sso-jboss/commits/master)
* [Download](https://github.com/camunda/camunda-sso-jboss/archive/master.zip)
* [Contributing](https://help.github.com/articles/about-pull-requests/)


## Roadmap

**todo**
- Contribution to Camunda

**done**
- Security filter to map users and groups 


## Maintainer

- Eberhard Heber (eberhardheber@novatec-gmbh.de)
- Falko Menge (falko.menge@camunda.com)

## License

Apache License, Version 2.0
