facsimile
=========

The goal of the FACSIMILE project is to create a semantic framework for modeling structured functional assessment data, and demonstrate how such data can be obtained from assessment instruments derived from such a model.


usage
--------------------
The Web application can be deployed using the provided Jetty server, via the script `run-generator`, or deployed in some other Web server / Java servlet container (local or remotely)
 
In order to run it locally all you need to do is execute the script `run-generator` for your operating system (use `run-generator.sh` on UNIX-based systems and `run-generator.bat` on Windows). Alternatively, one can build `form-generator.war` using ant, and then deploy it by executing:

`java -jar jetty/jetty-runner.jar form-generator.war`

The application should then be available at: http://localhost:8085 on any Web browser.