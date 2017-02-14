# Camunda BPM Webapp with SSO for JBoss AS7/Wildfly Server

This project integrates a Single Sign On (SSO) mechanism into the CamundaBPM cockpit. The main mechanic is done by 
the JBoss implementation. Therefore, it is needed to configure an Active Directory and the JBoss installation.

### Problem

The camunda cockpit have to be secured.

### Business Requirements

* Login once via a SSO mechanic
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
* All GUIs and Restapis are only accessible via AD Login

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
    novadDmain.local = "NOVADOMAIN.LOCAL"
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

## Resources

* [Issue Tracker](link-to-issue-tracker) _use github unless you got your own_
* [Roadmap](link-to-issue-tracker-filter) _if in terms of tagged issues_
* [Changelog](link-to-changelog) _lets users track progress on what has been happening_
* [Download](link-to-downloadable-archive) _if downloadable_
* [Contributing](link-to-contribute-guide) _if desired, best to put it into a CONTRIBUTE.md file_


## Roadmap

**todo**
- Contribution to Camunda

**done**
- Security filter to map users and groups 


## Maintainer

Eberhard Heber
eberhardheber@novatec-gmbh.de


## License

Apache License, Version 2.0
