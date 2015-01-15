#!/bin/bash
# 
# This script builds and runs the form generator on a local Jetty Web server instance. After running this script,
# the form generator will be accessible (via a Web browser) at http://localhost:8085. Java and Ant are required.
#
# Compile sources and produce the war and javadocs
echo "Building from sources..." && ant
#
# Deploy form-generator.jar on a local Jetty instance 
echo ""
echo " !! Once the server is running, point your Web browser to http://localhost:8085 to generate and submit forms !!"
echo ""
echo "Starting server..."
java -jar jetty/jetty-runner.jar --port 8085 form-generator.war