package edu.stanford.bmir.facsimile.dbq;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.generator.FormGenerator;
import edu.stanford.bmir.facsimile.dbq.question.QuestionParser;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Runner {
	
	/**
	 * Load ontology specified in a configuration
	 * @param conf	Configuration
	 * @return OWL ontology
	 */
	private static OWLOntology loadOntology(Configuration conf) {
		File f = new File(conf.getOntologyPath());
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
		
		System.out.print("Loading ontology: " + f.getAbsolutePath() + "... ");
		Map<IRI, String> map = conf.getImportsMap();
		for(IRI i : map.keySet())
			man.getIRIMappers().add(new SimpleIRIMapper(i, IRI.create("file:" + map.get(i))));
		
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new FileDocumentSource(f), config);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		System.out.println("done");
		return ont;
	}
	
	
	/**
	 * Print usage message 
	 */
	private static void printUsage() {
		System.out.println(" Usage:\n\t-ont [ONTOLOGY] -config [CONFIGURATION] [OPTIONS]");
		System.out.println();
		System.out.println("	[ONTOLOGY]	An input ontology file path or URL");
		System.out.println();
		System.out.println("	[CONFIGURATION]	An XML configuration file specifying class, property, and question bindings");
		System.out.println();
		System.out.println("	[OPTIONS]");
		System.out.println("	-v		verbose mode");
		System.out.println();
	}
	
	
	/**
	 * Main
	 * @param args	Configuration file path
	 * @throws IOException	IO exception
	 */
	public static void main(String[] args) throws IOException {
		Configuration conf = null; OWLOntology ont = null;
		boolean verbose = false;
		
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-config")) {
				if(++i == args.length) throw new RuntimeException("\n-config must be followed by a path to a configuration file.\n");
				System.out.print("Loading configuration: " + args[i] + "... ");
				if(!args[i].startsWith("-"))
					conf = new Configuration(new File(args[i].trim()), verbose);
				System.out.println("done");
			}
			if(arg.equalsIgnoreCase("-v"))
				verbose = true;
		}
		
		if(conf != null) {
			ont = Runner.loadOntology(conf);
			String outputPath = conf.getOutputFilePath();
			System.out.println("Output file: " + outputPath);
			
			QuestionParser gen = new QuestionParser(ont, conf, verbose);
			FormGenerator form = new FormGenerator(gen.getQuestions("_Back_"), verbose);
			form.generateHTMLForm(new File(outputPath), conf.getOutputFileTitle());
		}
		else {
			if(ont == null)
				System.err.println("\nCould not load ontology; an ontology URI must follow the -ont flag.\n");
			if(conf == null)
				System.err.println("\nCould not load configuration file; the path to the configuration must follow the -config flag.\n");
			Runner.printUsage(); System.exit(0);
		}
	}
}
