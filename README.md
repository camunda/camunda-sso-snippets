# Camunda BPM Webapp with SSO for Oracle WebLogic Server

This project integrates a Single Sign On (SSO) mechanism into the CamundaBPM cockpit. The main mechanic is done by 
the WebLogic implementation. Therefore, it is needed to configure an Active Directory and the WebLogic installation.

### Problem

The camunda cockpit have to be secured.

### Business Requirements

* Login once via a SSO mechanic
* Security has to be implemented by standard WebLogic configuration

### Technical Requirements

* Windows Active Directory (AD)
* Oracle WebLogic Server 12c
* AD User and Groups for integration in Camunda
* Dedicated AD User for WebLogic Service

### Solution

* Add Kerberos and AD Security-Negotiation to Oracle WebLogic Server
* Configure Camunda-Webapp

### Acceptance tests

* AD User can login into CamundaBPM
* All GUIs and Restapis are only accessible via AD Login

## Get started

### Configuration of AD and WebLogic Server

Have a look at the tutorial [How To Configure Browser-based SSO with Kerberos/SPNEGO and Oracle WebLogic Server](http://www.oracle.com/technetwork/articles/idm/weblogic-sso-kerberos-1619890.html)

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


## Maintainer


## License

Apache License, Version 2.0
