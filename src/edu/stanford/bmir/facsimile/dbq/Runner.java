package edu.stanford.bmir.facsimile.dbq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.exception.MissingOntologyFileException;
import edu.stanford.bmir.facsimile.dbq.exception.OntologyFileParseException;
import edu.stanford.bmir.facsimile.dbq.form.QuestionParser;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;
import edu.stanford.bmir.facsimile.dbq.generator.FormGenerator;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Runner {
	public final static String name = "facsimile-form-generator", version = "1.0b";
	private File configFile;
	private boolean verbose, saveHtml;
	private Configuration conf;
	private List<Section> sections;
	private OWLOntology ont;
	private Map<String,Map<String,String>> questionOptions;
	private Map<IRI,IRI> aliases;
	
	
	/**
	 * Constructor
	 * @param configFile	Configuration file
	 * @param saveHtml	true if output HTML code should be saved to file, false otherwise
	 * @param verbose	Verbose mode
	 */
	public Runner(File configFile, boolean saveHtml, boolean verbose) {
		this.configFile = configFile;
		this.saveHtml = saveHtml;
		this.verbose = verbose;
	}
	
	
	/**
	 * Execute form generation procedure
	 * @return String containing HTML code for the form
	 */
	public String run() {
		System.out.print("Loading configuration file: " + configFile.getAbsolutePath() + "... ");
		if(verbose) System.out.println();
		conf = new Configuration(configFile, verbose);
		conf.parseConfigurationFile();
		System.out.println("done");
		ont = loadOntology();
		
		QuestionParser gen = new QuestionParser(ont, conf, verbose);
		sections = gen.getSections();
		questionOptions = gen.getQuestionOptions();
		
		FormGenerator formGen = new FormGenerator(sections, conf, verbose);
		String output = formGen.generateHTMLForm(conf.getOutputFileTitle(), conf.getCSSStyleClass());
		if(saveHtml) serializeOutput(output);
		aliases = formGen.getIRIAliases();
		System.out.println("finished");
		return output;
	}
	
	
	/**
	 * Generate form to a local file specified in the configuration file
	 * @param output	Output HTML code
	 */
	public void serializeOutput(String output) {		
		String outputDir = conf.getOutputFilePath();
		if(outputDir == null || outputDir.isEmpty())
			outputDir = "output" + File.separator + "form.html" + File.separator;
		System.out.println("Output: " + outputDir);
		File f = new File(outputDir);
		try {
			if(!f.exists()) {
				new File(f.getParent()).mkdirs();
				f.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(output); bw.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Load ontology specified in a configuration
	 * @return OWL ontology
	 */
	private OWLOntology loadOntology() {
		String inputFile = conf.getInputOntologyPath();
		if(inputFile == null)
			throw new MissingOntologyFileException("Could not find an input ontology element in the given configuration file");
		IRI iri = null;
		if(inputFile.contains(":")) iri = IRI.create(inputFile);
		else iri = IRI.create("file:" + inputFile);
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
		
		Map<IRI,String> map = conf.getInputImportsMap();
		for(IRI i : map.keySet()) {
			String path = map.get(i);
			if(!path.contains(":")) path = "file:" + path;
			man.getIRIMappers().add(new SimpleIRIMapper(i, IRI.create(path)));
		}
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new IRIDocumentSource(iri), config);
		} catch (OWLOntologyCreationException e) { 
			e.printStackTrace(); 
		}
		if(ont == null)
			throw new OntologyFileParseException("Could not load ontology specified in the configuration file. Make sure that all imports are well-specified, and can be properly resolved");
		System.out.println("done");
		return ont;
	}
	
	
	/**
	 * Get the ordered list of sections 
	 * @return List of sections
	 */
	public List<Section> getSections() {
		if(sections == null) run();
		return sections;
	}
	
	
	/**
	 * Get the map of question IRIs and their options 
	 * @return Map of question IRIs to their answer options
	 */
	public Map<String,Map<String,String>> getQuestionOptions() {
		if(questionOptions == null) run();  
		return questionOptions;
	}
	
	
	/**
	 * Get the configuration instance
	 * @return Configuration
	 */
	public Configuration getConfiguration() {
		return conf;
	}
	
	
	/**
	 * Get the input ontology
	 * @return OWL ontology
	 */
	public OWLOntology getOntology() {
		return ont;
	}
	
	
	/**
	 * Get the map of IRI aliases
	 * @return Map of IRI aliases
	 */
	public Map<IRI,IRI> getIRIAliases() {
		return aliases;
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
		System.out.println("	-s		save resulting HTML form file");
		System.out.println("	-v		verbose mode");
		System.out.println("	-h		print usage");
		System.out.println();
	}
	
	
	/**
	 * Main
	 * @param args	Configuration file path, verbose flag
	 * @throws IOException	IO exception
	 */
	public static void main(String[] args) throws IOException {
		boolean verbose = false, saveHtml = false; File file = null;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-config")) {
				if(++i == args.length) {
					System.err.println("\n!! Error: -config must be followed by a path to a configuration file !!\n");
					printUsage(); System.exit(1);
				}
				if(!args[i].startsWith("-"))
					file = new File(args[i].trim());
			}
			if(arg.equalsIgnoreCase("-v"))
				verbose = true;
			if(arg.equalsIgnoreCase("-s"))
				saveHtml = true;
			if(arg.equalsIgnoreCase("-h")) {
				printUsage(); System.exit(0);
			}
		}
		if(file != null)
			new Runner(file, saveHtml, verbose).run();
		else {
			System.err.println("\n!! Error: Could not load configuration file; the path to the configuration must follow the -config flag !!\n");
			printUsage(); System.exit(1);
		}
	}
}