#!/bin/bash
# 
# This script builds (if necessary) and runs the form generator on a local Jetty Web server instance. After running this script,
# the form generator should be accessible (via a Web browser) at http://localhost:8085. Java and Ant are required.
#
# Compile sources and produce the war and javadocs (if form-generator.war does not exist)
[ -f form-generator.war ] || (echo "Building from sources..." && ant)
#
# Deploy form-generator.jar on a local Jetty instance 
echo ""
echo "Once the service is running, point your Web browser to http://localhost:8085 to generate and submit forms"
echo ""
java -jar lib/jetty-runner.jar --port 8085 form-generator.war