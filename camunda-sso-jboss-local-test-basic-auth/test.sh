#!/bin/sh
firefox --private-window http://localhost:8080/camunda/
mvn clean wildfly:run -Ptest-wildfly
