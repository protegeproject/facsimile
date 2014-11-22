package edu.stanford.bmir.facsimile.dbq.question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.question.Question.QuestionType;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class QuestionParser {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private OWLReasoner reasoner;
	private Configuration conf;
	private boolean verbose;
	private Map<OWLClassExpression,QuestionType> questionTypes;
	
	/**
	 * Constructor
	 * @param ont	OWL ontology document
	 * @param conf	Configuration file
	 * @param verbose	true for verbose mode
	 */
	public QuestionParser(OWLOntology ont, Configuration conf, boolean verbose) {
		this.ont = ont;
		this.conf = conf;
		this.man = ont.getOWLOntologyManager();
		this.df = man.getOWLDataFactory();
		this.reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
		this.verbose = verbose;
		this.questionTypes = initQuestionTypes();
	}
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology document
	 * @param conf	Configuration file
	 */
	public QuestionParser(OWLOntology ont, Configuration conf) {
		this(ont, conf, false);
	}
	
	
	/**
	 * Extract questions (individuals) and their corresponding axioms from the ontology
	 * @param type	Specific type of question desired (if any). The given string is matched against the individual name 
	 * @return Map of questions to the sets of axioms associated with each of them 
	 */
	private Map<OWLNamedIndividual,Set<OWLAxiom>> collectQuestionAxioms(String type) {
		if(verbose) System.out.print("Parsing questions... ");
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = new HashMap<OWLNamedIndividual,Set<OWLAxiom>>();
		
		OWLClass questionClass = df.getOWLClass(conf.getQuestionClass());
		Set<OWLNamedIndividual> instances = reasoner.getInstances(questionClass, false).getFlattened();
		
		for(OWLNamedIndividual i : instances) {
			if(i.getIRI().getShortForm().contains(type))
				map.put(i, ont.getReferencingAxioms(i, Imports.INCLUDED));
		}
		if(verbose) System.out.println("done");
		return map;
	}
	
	
	
	private List<Question> parseQuestions(Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		List<Question> questions = new ArrayList<Question>();
		OWLDataProperty text = df.getOWLDataProperty(conf.getQuestionTextPropertyBinding());
		OWLObjectProperty value = df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding());
		OWLObjectProperty focus = df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding());
		
		for(OWLNamedIndividual i : map.keySet()) {
			if(verbose) System.out.println("Processing question: " + i.getIRI().getShortForm());
			String qText = "", qFocus = ""; QuestionType qType = null; int qNumber = 0;
			
			Set<OWLAxiom> axioms = map.get(i);
			for(OWLAxiom ax : axioms) {
				if(ax.isLogicalAxiom()) {
					if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
						if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(text)) {
							qText = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
							if(verbose) System.out.println("\tQuestion text: " + qText);
						}
					}
					if(ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
						if(((OWLObjectPropertyAssertionAxiom)ax).getProperty().equals(focus)) {
							OWLIndividual ind = ((OWLObjectPropertyAssertionAxiom)ax).getObject();
							if(ind.isNamed())
								qFocus = ((OWLNamedIndividual)ind).getIRI().getShortForm();
							else
								qFocus = ind.toString();
							
							if(verbose) System.out.println("\tQuestion focus: " + qFocus);
						}
					}
					if(ax.isOfType(AxiomType.CLASS_ASSERTION)) {
						OWLClassExpression ce = ((OWLClassAssertionAxiom)ax).getClassExpression();
						if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
							if(((OWLObjectSomeValuesFrom)ce).getProperty().equals(value)) {
								qType = findQuestionType(((OWLObjectSomeValuesFrom)ce).getFiller());
								if(verbose) System.out.println("\tQuestion type: " + qType);
							}
						}
					}
				}
			}
			
//			if(conf.containsQuestion(i.getIRI()))
//				conf.getQuestionNumber(i.getIRI())
		}
		return questions;
	}
	
	
	private QuestionType findQuestionType(OWLClassExpression ce) {
		QuestionType type = null;
		if(questionTypes.containsKey(ce))
			type = questionTypes.get(ce);
		else if(ce instanceof OWLObjectOneOf /* || ce subclassof (ce' | ce' is instanceof owlobjectoneof) */ ) //TODO
			type = QuestionType.DROPDOWN;
		return type;
	}
	
	
	
	/**
	 * Get the list of questions of a specific type instantiated in the given ontology
	 * @return List of question objects
	 */
	public List<Question> getQuestions(String type) {
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = collectQuestionAxioms(type); 
		List<Question> questions = parseQuestions(map);
		
		return questions;
	}
	
	
	/**
	 * Get the list of all questions instantiated in the given ontology
	 * @return List of question objects
	 */
	public List<Question> getAllQuestions() {
		return getQuestions("");
	}

	
	/**
	 * Retrieve question type bindings from the configuration file 
	 * @return Map of OWL class expressions to what HTML form question type they should be  
	 */
	private Map<OWLClassExpression,QuestionType> initQuestionTypes() {
		Map<OWLClassExpression,QuestionType> qTypes = new HashMap<OWLClassExpression,QuestionType>();
		OWLClassExpression text = df.getOWLClass(conf.getTextInputBinding());
		OWLClassExpression radio = df.getOWLClass(conf.getRadioInputBinding());
		qTypes.put(text, QuestionType.TEXTFIELD);
		qTypes.put(radio, QuestionType.RADIO);
		return qTypes;
	}
}
