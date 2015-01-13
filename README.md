facsimile
=========

The goal of the FACSIMILE project is to create a semantic framework for modeling structured functional assessment data, and demonstrate how such data can be obtained from assessment instruments derived from such a model.


usage
--------------------
The form generator can be used in two ways:

* Using the provided Jetty server, via the script `run-generator.sh`
* Deployed in some other Web server / Java servlet container (local or remotely)
 
In order to run it locally all you need to do is run the script `run-generator.sh` from the command line, or, alternatively, build `form-generator.war` using ant, and then execute:

`java -jar lib/jetty-runner.jar form-generator.war`

The form generator should then be available at: http://localhost:8080 on any Web browser.