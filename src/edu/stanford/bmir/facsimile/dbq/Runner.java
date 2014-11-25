package edu.stanford.bmir.facsimile.dbq;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.xml.sax.SAXException;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.generator.FormGenerator;
import edu.stanford.bmir.facsimile.dbq.question.Question;
import edu.stanford.bmir.facsimile.dbq.question.QuestionParser;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Runner {

	public static void main(String[] args) throws OWLOntologyCreationException, ParserConfigurationException, SAXException, IOException {
		String sep = File.separator;
		boolean verbose = true;
		
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
		
		File f = new File(args[0]); // ontology file
		String folderPath = f.getParentFile().getAbsolutePath() + sep; // path of root folder where ontology (and its imports) are located 
		
		// IRI mappers for imported ontologies
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/datamodel"), 
				IRI.create("file:" + folderPath + "datamodel.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/cfa"), 
				IRI.create("file:" + folderPath + "ides_cfa.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://who.int/icf"), 
				IRI.create("file:" + folderPath + "icf_simplified_2013.11.22.owl")));
		
		System.out.print("Loading ontology... ");
		OWLOntology ont = man.loadOntologyFromOntologyDocument(new FileDocumentSource(f), config);
		System.out.println("done");
		
		Configuration conf = new Configuration(new File(args[1]), verbose);
		
		QuestionParser gen = new QuestionParser(ont, conf, verbose);
		List<Question> questions = gen.getQuestions("_Back_");
		
		FormGenerator form = new FormGenerator(questions, verbose);
		form.generateHTMLForm(new File("/Users/rgoncalves/Documents/workspace/facsimile/test/index.html"), "DBQ Form");
	}
}
