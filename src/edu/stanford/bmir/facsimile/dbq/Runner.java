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
		String inputFile = conf.getInputOntologyPath();
		if(inputFile == null) {
			System.err.println("\n!! Error: Input ontology file path not specified in configuration file !!\n");
			System.exit(1);
		}
		File f = new File(inputFile);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
		
		System.out.print("Loading ontology: " + f.getAbsolutePath() + "... ");
		Map<IRI, String> map = conf.getInputImportsMap();
		for(IRI i : map.keySet())
			man.getIRIMappers().add(new SimpleIRIMapper(i, IRI.create("file:" + map.get(i))));
		
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new FileDocumentSource(f), config);
		} catch (OWLOntologyCreationException e) { e.printStackTrace(); }
		System.out.println("done");
		return ont;
	}
	
	
	/**
	 * Print usage message 
	 */
	private static void printUsage() {
		System.out.println(" Usage:\n\t-config [CONFIGURATION] [OPTIONS]");
		System.out.println();
		System.out.println("	[CONFIGURATION]	An XML configuration file input, output, entity bindings, and section/question ordering");
		System.out.println();
		System.out.println("	[OPTIONS]");
		System.out.println("	-v		verbose mode");
		System.out.println();
	}
	
	
	/**
	 * Main
	 * @param args	Configuration file path, verbose flag
	 * @throws IOException	IO exception
	 */
	public static void main(String[] args) throws IOException {
		boolean verbose = false; File file = null;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-config")) {
				if(++i == args.length) {
					System.err.println("\n!! Error: -config must be followed by a path to a configuration file !!\n");
					Runner.printUsage(); System.exit(1);
				}
				if(!args[i].startsWith("-"))
					file = new File(args[i].trim());
			}
			if(arg.equalsIgnoreCase("-v"))
				verbose = true;
		}
		
		OWLOntology ont = null;
		if(file != null) {
			System.out.print("Loading configuration file: " + file.getAbsolutePath() + "... ");
			if(verbose) System.out.println();
			Configuration conf = new Configuration(file, verbose);
			conf.loadConfiguration();
			System.out.println("done");
			
			ont = Runner.loadOntology(conf);
			String outputPath = conf.getOutputFilePath();
			System.out.println("Output file: " + outputPath);
			
			QuestionParser gen = new QuestionParser(ont, conf, verbose);
			FormGenerator form = new FormGenerator(gen.getSections("_Back_"));
			form.generateHTMLForm(new File(outputPath), conf.getOutputFileTitle(), conf.getCSSStyleClass());
			System.out.println("finished");
		} else {
			System.err.println("\n!! Error: Could not load configuration file; the path to the configuration must follow the -config flag !!\n");
			Runner.printUsage(); System.exit(1);
		}
	}
}