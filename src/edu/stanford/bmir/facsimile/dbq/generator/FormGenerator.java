package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.question.Question;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class FormGenerator {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private Configuration conf;
	private boolean verbose;
	
	
	/**
	 * Constructor
	 * @param ont
	 * @param conf
	 * @param verbose
	 */
	public FormGenerator(OWLOntology ont, Configuration conf, boolean verbose) {
		this.ont = ont;
		this.conf = conf;
		this.man = ont.getOWLOntologyManager();
		this.df = man.getOWLDataFactory();
		this.verbose = verbose;
	}
	
	
	/**
	 * Extract questions (individuals) and their corresponding axioms from the ontology
	 * @param type	Specific type of question desired (if any). The given string is matched against the individual name 
	 * as it is in the given ontology
	 * @return Map of questions (represented as individuals) to the sets of axioms associated with each question 
	 */
	private Set<Question> collectQuestions(String type) {
		if(verbose) System.out.print("Parsing questions... ");
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = new HashMap<OWLNamedIndividual,Set<OWLAxiom>>();
		
		OWLClass questionClass = df.getOWLClass(conf.getQuestionClass());
		StructuralReasoner reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
		Set<OWLNamedIndividual> instances = reasoner.getInstances(questionClass, false).getFlattened();
		
		for(OWLNamedIndividual i : instances) {
			if(i.getIRI().getShortForm().contains(type))
				map.put(i, ont.getReferencingAxioms(i, Imports.INCLUDED));
		}
		if(verbose) System.out.println("done");
		return parseQuestions(map);
	}
	
	
	
	private Set<Question> parseQuestions(Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		Set<Question> questions = new HashSet<Question>();
		OWLDataProperty text = df.getOWLDataProperty(conf.getQuestionTextPropertyBinding());
		OWLObjectProperty value = df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding());
		
		int counter = 0;
		for(OWLNamedIndividual i : map.keySet()) {
			counter++;
			System.out.println("Processing question " + counter + ": " + i.getIRI().getShortForm());
			String qText = "";
			Set<OWLAxiom> axioms = map.get(i);
			for(OWLAxiom ax : axioms) {
				if(ax.isLogicalAxiom()) {
					if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
						if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(text)) {
							qText = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
							System.out.println("\tQuestion text: " + qText);
						}
					}
					if(ax.isOfType(AxiomType.CLASS_ASSERTION)) {
						OWLClassExpression ce = ((OWLClassAssertionAxiom)ax).getClassExpression();
						if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
							if(((OWLObjectSomeValuesFrom)ce).getProperty().equals(value)) {
								OWLClassExpression filler = ((OWLObjectSomeValuesFrom)ce).getFiller();
								
								System.out.println("\tQuestion type: " + filler);
							}
						}
					}
				}
			}
		}
		return questions;
	}
	
	
	
	public void printQuestions(String type) {
		Set<Question> questions = collectQuestions(type);
	}
	
	
	public void printAllQuestions() {
		printQuestions("");
	}
	
}
