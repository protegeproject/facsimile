facsimile
=========

The goal of the FACSIMILE project is to create a semantic framework for modeling structured functional assessment data, and demonstrate how such data can be obtained from assessment instruments derived from such a model.

requirements
--------------------
* Java (version 7+)
* Apache Ant

usage
--------------------
The Web application can be deployed using the provided Jetty server, via the script `run-generator`, or deployed in some other Web server / Java servlet container (local or remotely)
 
In order to run it locally all you need to do is execute the script `run-generator` for your operating system (use `run-generator.sh` on UNIX-based systems and `run-generator.bat` on Windows). Alternatively, one can build `form-generator.war` using ant, and then deploy it by executing:

`java -jar jetty/jetty-runner.jar form-generator.war`

The application should then be available at: http://localhost:8085 on any Web browser.

There are several example forms built into the application, which users can experiment with. To generate a new form, users are encouraged to inspect the example forms (and corresponding XML configuration files), and then create an XML file specifying their forms.

sample data
--------------------
To examine or experiment with sample data generated using this tool, we have placed our ontologies, data gathered using our supplied forms, as well as example SPARQL queries in the `ontology` folder. The `dbq_data_merge.owl` ontology file is the starting point. This ontology imports all relevant ontologies needed to run SPARQL queries. We placed some example SPARQL queries under the `ontology/sparql` folder. To try out these queries, we recommend cloning the entire repository (or the `ontology` folder), loading the ontology `dbq_data_merge.owl` into [Protégé](http://protege.stanford.edu), and run the sample queries against this ontology using the SPARQL Query plugin.
