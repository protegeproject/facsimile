#!/bin/bash
# 
# This script builds (if necessary) and runs the form generator on a local Jetty Web server instance. After
# running this script, the form generator should be accessible (via a Web browser) at http://localhost:8080
# The project requires Java and ant installed.
#
# Compile sources and produce the war and javadocs (if form-generator.war does not exist)
[ -f form-generator.war ] || (echo "Building from sources..." && ant -buildfile build-servlet.xml)
#
# Deploy form-generator.jar on a local Jetty instance 
java -jar lib/jetty-runner.jar form-generator.war