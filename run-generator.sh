#!/bin/bash
# 
# This script builds and runs the form generator on a local Jetty Web server instance. After running this script,
# the form generator will be accessible (via a Web browser) at http://localhost:8085. Java 1.7+ and Ant are required.
#
# Compile sources and produce the war and javadocs
echo ""
echo "Building from sources..."
echo ""
ant
#
# Deploy form-generator.war on a local Jetty instance, in the port specified below
#
port="8085"
echo ""
echo "Starting Jetty server on port $port..."
echo ""
echo "Once the server is running, point your Web browser to http://localhost:$port to generate and submit forms"
echo ""
open http://localhost:$port
java -jar jetty/jetty-runner.jar --port $port form-generator.war

