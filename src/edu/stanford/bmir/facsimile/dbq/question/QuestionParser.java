package edu.stanford.bmir.facsimile.dbq.question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
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
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionParser {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private OWLReasoner reasoner;
	private Configuration conf;
	private boolean verbose;
	private Map<OWLClassExpression,QuestionType> questionTypes;
	private OWLDataProperty textDataProperty, dataValueProperty;
	private OWLObjectProperty valueObjectProperty, focusObjectProperty;
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology document
	 * @param conf	Configuration file
	 * @param verbose	true for verbose mode
	 */
	public QuestionParser(OWLOntology ont, Configuration conf, boolean verbose) {
		this.ont = ont;
		this.conf = conf;
		this.verbose = verbose;
		man = ont.getOWLOntologyManager();
		df = man.getOWLDataFactory();
		reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
		questionTypes = initQuestionTypes();
		textDataProperty = df.getOWLDataProperty(conf.getQuestionTextPropertyBinding());
		valueObjectProperty = df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding());
		focusObjectProperty = df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding());
		dataValueProperty = df.getOWLDataProperty(conf.getQuestionDataValuePropertyBinding());
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
		
		OWLClass questionClass = df.getOWLClass(conf.getQuestionInputClass());
		Set<OWLNamedIndividual> instances = reasoner.getInstances(questionClass, false).getFlattened();
		
		for(OWLNamedIndividual i : instances) {
			if(i.getIRI().getShortForm().contains(type))
				map.put(i, ont.getReferencingAxioms(i, Imports.INCLUDED));
		}
		if(verbose) System.out.println("done");
		return map;
	}
	
	
	/**
	 * Parse the questions in the ontology according to the order in the configuration file
	 * @param map	Map of questions (individuals) to the set of axioms that they occur in
	 * @return List of questions
	 */
	private List<QuestionSection> parseSections(Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		List<QuestionSection> qSections = new ArrayList<QuestionSection>();
		Map<IRI,List<List<IRI>>> sections = conf.getSectionMap(); 
		int counter = 1;
		for(IRI section : sections.keySet()) { // foreach section
			List<List<IRI>> qList = sections.get(section);
			List<Question> questions = new ArrayList<Question>();
			for(int i = 0; i < qList.size(); i++) {
				List<IRI> subquestions = qList.get(i);
				for(int j = 0; j < subquestions.size(); j++) {
					OWLNamedIndividual ind = df.getOWLNamedIndividual(subquestions.get(j));
					if(verbose) System.out.println("   Processing question: " + ind.getIRI().getShortForm());
					String qNumber = "" + alphabet[i];
					if(subquestions.size()>1)
						qNumber += "" + (j+1);
					Question q = getQuestionDetails(qNumber, counter, ind, map.get(ind));
					if(verbose) printQuestionInfo(q);
					questions.add(q);
				}
			}
			qSections.add(new QuestionSection(getSectionHeader(section), questions));
			counter++;
		}
		return qSections;
	}
	
	
	/**
	 * Get the section header for a given section IRI
	 * @param iri	IRI of the section
	 * @return Header of the section
	 */
	private String getSectionHeader(IRI iri) {
		String header = "";
		OWLNamedIndividual ind = df.getOWLNamedIndividual(iri);
		OWLDataProperty dp = df.getOWLDataProperty(conf.getSectionHeaderPropertyBinding());		
		for(OWLAxiom ax : ont.getReferencingAxioms(ind)) {
			if(ax instanceof OWLDataPropertyAssertionAxiom && ax.containsEntityInSignature(dp))
				header = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
		}
		return header;
	}
	
	
	/**
	 * Given a question, gather its details: text, focus, type, and possible answers
	 * @param qNr	Question number
	 * @param sectionNr	Section number
	 * @param ind	Individual representing a question
	 * @param axioms	 Set of axioms where the individual is mentioned 
	 * @return Question instance
	 */
	private Question getQuestionDetails(String qNr, int sectionNr, OWLNamedIndividual ind, Set<OWLAxiom> axioms) {
		String qText = "", qFocus = ""; QuestionOptions qOpts = null;
		for(OWLAxiom ax : axioms) {
			if(ax.isLogicalAxiom()) {
				if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION) && qText.isEmpty())
					qText = getQuestionText(ax, textDataProperty);
				if(ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION) && qFocus.isEmpty())
					qFocus = getQuestionFocus(ax, focusObjectProperty);
				if(ax.isOfType(AxiomType.CLASS_ASSERTION) && (qOpts == null || qOpts.getQuestionType() == null))
					qOpts = getQuestionOptions(ind.getIRI(), ax, valueObjectProperty);
			}
		}
		if(qOpts.getQuestionType() == null) {
			qOpts.setQuestionType(QuestionType.TEXTAREA);
			System.out.println("\t!! Question type not defined in ontology or configuration file. "
					+ "Defaulting to text field !!");
		}
		return new Question(qNr, sectionNr, qText, qFocus, qOpts.getQuestionType(), qOpts.getOptions());
	}
	
	
	/**
	 * Get the type of question, i.e., what the HTML form output will be
	 * @param questionIri	IRI of the question individual
	 * @param ax	Class assertion axiom
	 * @param valueObjectProperty	Object property that represents the input of the question
	 * @return Type of question
	 */
	private QuestionOptions getQuestionOptions(IRI questionIri, OWLAxiom ax, OWLObjectProperty valueObjectProperty) {
		OWLClassExpression ce = ((OWLClassAssertionAxiom)ax).getClassExpression();
		List<String> opts = new ArrayList<String>();
		QuestionType qType = null;
		if(conf.hasDefinedType(questionIri)) {
			qType = conf.getQuestionType(questionIri);
			opts = getOptions(ce);
		}
		else if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
			if(((OWLObjectSomeValuesFrom)ce).getProperty().equals(valueObjectProperty)) {
				OWLClassExpression filler = ((OWLObjectSomeValuesFrom)ce).getFiller();
				if(questionTypes.containsKey(filler)) // string or boolean values
					qType = questionTypes.get(filler);
				else if(filler instanceof OWLObjectOneOf) { /* || ce subclassof (ce' | ce' is instanceof owlobjectoneof) */ 
					qType = QuestionType.DROPDOWN;
					opts = getOptionsFromEnumeration((OWLObjectOneOf)filler);
				}
			}
		}
		if(qType != null && qType.equals(QuestionType.RADIO)) 
			opts.addAll(Arrays.asList("YES","NO"));
		
		return new QuestionOptions(questionIri, qType, opts);
	}
	
	
	/**
	 * Get the answer options from a given class expression 
	 * @param ce	OWL class expression
	 * @return List of options represented by the given class expression
	 */
	private List<String> getOptions(OWLClassExpression ce) {
		List<String> opts = new ArrayList<String>();
		if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
			if(((OWLObjectSomeValuesFrom)ce).getProperty().equals(valueObjectProperty)) {
				OWLClassExpression filler = ((OWLObjectSomeValuesFrom)ce).getFiller();
				if(filler instanceof OWLObjectOneOf)
					opts = getOptionsFromEnumeration((OWLObjectOneOf)filler);
			}
		}
		return opts;
	}
	
	
	/**
	 * Get the list of options contained in an enumeration 
	 * @param optsEnum	Class expression
	 * @return List of answer options
	 */
	private List<String> getOptionsFromEnumeration(OWLObjectOneOf optsEnum) {
		List<String> list = new ArrayList<String>();
		for(OWLNamedIndividual ind : optsEnum.getIndividualsInSignature()) {
			Set<OWLAxiom> usg = ont.getReferencingAxioms(ind, Imports.INCLUDED);
			for(OWLAxiom ax : usg) {
				if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
					if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(dataValueProperty)) {
						OWLLiteral lit = ((OWLDataPropertyAssertionAxiom)ax).getObject();
						list.add(StringUtils.leftPad(lit.getLiteral(), 6, '0')); // add leading zeroes for proper sorting
					}
				}
				else if(ax.isOfType(AxiomType.CLASS_ASSERTION)) {
					OWLClassExpression ce = ((OWLClassAssertionAxiom)ax).getClassExpression();
					if(ce instanceof OWLDataSomeValuesFrom) {
						OWLDataRange r = ((OWLDataSomeValuesFrom)ce).getFiller();
						OWLDatatypeRestriction res = (OWLDatatypeRestriction)r;
						for(OWLFacetRestriction fr : res.getFacetRestrictions())
							list.add(fr.getFacet().getSymbolicForm() + fr.getFacetValue().getLiteral());
					}
				}
			}
		}
		return sortOptionList(list);
	}
	
	
	/**
	 * Sort a given list of options
	 * @param list	List of options in the order gathered
	 * @return Sorted list of options
	 */
	private List<String> sortOptionList(List<String> list ) {
		Collections.sort(list);
		for(int i = 0; i<list.size(); i++) {
			String opt = list.get(i);
			opt = opt.replaceFirst("^0+(?!$)", ""); // remove leading zeroes
			list.remove(i); list.add(i, opt);
		}
		return list;
	}
	
	
	/**
	 * Get the focus of the question, i.e., the desired input w.r.t. the modeling of the ontology
	 * @param ax	Object property assertion axiom
	 * @param focus	Object property that represents the focus of the question
	 * @return String describing the focus of the question
	 */
	private String getQuestionFocus(OWLAxiom ax, OWLObjectProperty focus) {
		String qFocus = "";
		if(((OWLObjectPropertyAssertionAxiom)ax).getProperty().equals(focus)) {
			OWLIndividual ind = ((OWLObjectPropertyAssertionAxiom)ax).getObject();
			if(ind.isNamed())
				qFocus = ((OWLNamedIndividual)ind).getIRI().getShortForm();
			else
				qFocus = ind.toString();
		}
		return qFocus;
	}
	
	
	/**
	 * Get the main text of the question
	 * @param ax	Data property assertion axioms
	 * @param text	Data property that represents the text (or title) of a text
	 * @return Text of the question
	 */
	private String getQuestionText(OWLAxiom ax, OWLDataProperty text) {
		String qText = "";
		if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(text))
			qText = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
		return qText;
	}
	
	
	/**
	 * Get the list of sections, and questions of a specific type, instantiated in the given ontology, 
	 * where type is a string that is matched against the individuals' names 
	 * @param type	Type of question (i.e., IRI name fragment)
	 * @return List of question
	 */
	public List<QuestionSection> getSections(String type) {
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = collectQuestionAxioms(type); 
		return parseSections(map);
	}
	
	
	/**
	 * Get the list of all sections and questions instantiated in the given ontology
	 * @return List of question sections
	 */
	public List<QuestionSection> getAllSections() {
		return getSections("");
	}

	
	/**
	 * Retrieve question type bindings from the configuration file 
	 * @return Map of OWL class expressions to what HTML form question type they should be  
	 */
	private Map<OWLClassExpression,QuestionType> initQuestionTypes() {
		Map<OWLClassExpression,QuestionType> qTypes = new HashMap<OWLClassExpression,QuestionType>();
		OWLClassExpression text = df.getOWLClass(conf.getTextInputBinding());
		OWLClassExpression radio = df.getOWLClass(conf.getRadioInputBinding());
		qTypes.put(text, QuestionType.TEXTAREA);
		qTypes.put(radio, QuestionType.RADIO);
		return qTypes;
	}
	
	
	/**
	 * Print details about the given question
	 * @param q	Question
	 */
	private void printQuestionInfo(Question q) {
		System.out.println("\tQuestion text: " + q.getQuestionText());
		System.out.println("\tQuestion focus: " + q.getQuestionFocus());
		System.out.println("\tQuestion type: " + q.getQuestionType());
		System.out.print("\tQuestion options: ");
		if(!q.getQuestionType().equals(QuestionType.TEXTAREA)) {
			for(String opt : q.getQuestionOptions())
				System.out.print(opt + " ");
		}
		else 
			System.out.print("none (text field)");
		if(verbose) System.out.println();
	}
}
