package edu.stanford.bmir.facsimile.dbq.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement.ElementType;
import edu.stanford.bmir.facsimile.dbq.form.elements.InformationElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
import edu.stanford.bmir.facsimile.dbq.form.elements.QuestionOptions;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionParser {
	private OWLObjectProperty valueObjectProperty, focusObjectProperty;
	private Map<OWLClassExpression,ElementType> questionTypes;
	private Map<String,Map<String,String>> questionOptions;
	private OWLDataProperty textDataProperty, dataValueProperty;
	private OWLOntologyManager man;
	private OWLReasoner reasoner;
	private OWLOntology ont;
	private OWLDataFactory df;
	private Configuration conf;
	private boolean verbose;
	
	
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
		questionOptions = new HashMap<String,Map<String,String>>();
		initQuestionTypes();
		initBindings();
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
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = new HashMap<OWLNamedIndividual,Set<OWLAxiom>>();
		OWLClass questionClass = df.getOWLClass(conf.getQuestionInputClass());
		Set<OWLNamedIndividual> instances = reasoner.getInstances(questionClass, false).getFlattened();
		for(OWLNamedIndividual i : instances) {
			if(i.getIRI().getShortForm().contains(type))
				map.put(i, ont.getReferencingAxioms(i, Imports.INCLUDED));
		}
		return map;
	}
	
	
	/**
	 * Parse the questions in the ontology according to the order in the configuration file
	 * @param map	Map of questions (individuals) to the set of axioms that they occur in
	 * @return List of questions
	 */
	private List<Section> parseSections(Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		List<Section> qSections = new ArrayList<Section>();
		Map<IRI,List<List<IRI>>> sections = conf.getSectionMap(); 
		int counter = 1;
		for(IRI section : sections.keySet()) { // foreach section
			OWLNamedIndividual sectionInd = df.getOWLNamedIndividual(section); // section (or form data element) individual
			List<List<IRI>> qList = sections.get(section);
			List<FormElement> questions = new ArrayList<FormElement>();
			for(int i = 0; i < qList.size(); i++) {
				List<IRI> subquestions = qList.get(i);
				for(int j = 0; j < subquestions.size(); j++) {
					IRI iri = subquestions.get(j);
					FormElement q = null;
					if(ont.containsIndividualInSignature(iri))
						q = getQuestion(subquestions, j, "" + alphabet[i], counter, map);
					else
						q = getInformationRequest(sectionInd, map);
					if(q != null) {
						if(verbose) printInfo(q);
						questions.add(q);
					}
				}
			}
			qSections.add(new Section(getSectionHeader(sectionInd), getSectionText(sectionInd), questions));
			counter++;
		}
		return qSections;
	}
	
	
	/**
	 * Get the question instance
	 * @param subquestions	List of (sub)questions
	 * @param j	loop index
	 * @param qNumber	Question number
	 * @param counter	Section counter
	 * @param map	Map of individuals to their usage axioms
	 * @return Question instance
	 */
	private Question getQuestion(List<IRI> subquestions, int j, String qNumber, int counter, Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		OWLNamedIndividual ind = df.getOWLNamedIndividual(subquestions.get(j));
		if(verbose) System.out.println("   Processing question: " + ind.getIRI().getShortForm());
		Question q = null;
		if(subquestions.size() > 1 && j > 0) {
			qNumber += "" + j;
			q = getQuestionDetails(qNumber, counter, ind, map.get(ind), true);
		}
		else 
			q = getQuestionDetails(qNumber, counter, ind, map.get(ind), false);
		return q;
	}
	
	
	
	private FormElement getInformationRequest(OWLNamedIndividual ind, Map<OWLNamedIndividual,Set<OWLAxiom>> map) {
		InformationElement q = null;
		if(verbose) System.out.println("   Processing information request: " + ind.getIRI().getShortForm());
		// TODO
//		q = new Question()
		return q;
	}
	
	
	/**
	 * Get the section header for a given section IRI
	 * @param ind	Section individual
	 * @return Header of the section
	 */
	private String getSectionHeader(OWLNamedIndividual ind) {
		String header = "";
		OWLDataProperty dp = df.getOWLDataProperty(conf.getSectionHeaderPropertyBinding());		
		for(OWLAxiom ax : ont.getReferencingAxioms(ind)) {
			if(ax instanceof OWLDataPropertyAssertionAxiom && ax.containsEntityInSignature(dp))
				header = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
		}
		return header;
	}
	
	
	/**
	 * Get the text that appears at the beginning of a given section 
	 * @param ind	Section individual
	 * @return Text of a section
	 */
	private String getSectionText(OWLNamedIndividual ind) {
		String text = "";
		OWLDataProperty dp = df.getOWLDataProperty(conf.getSectionTextPropertyBinding());
		for(OWLAxiom ax : ont.getReferencingAxioms(ind)) {
			if(ax instanceof OWLDataPropertyAssertionAxiom && ax.containsEntityInSignature(dp))
				text = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
		}
		return text;
	}
	
	
	/**
	 * Given a question, gather its details: text, focus, type, and possible answers
	 * @param qNr	Question number
	 * @param sectionNr	Section number
	 * @param ind	Individual representing a question
	 * @param axioms	 Set of axioms where the individual is mentioned
	 * @param subquestion	true if question has a parent question, false otherwise 
	 * @return Question instance
	 */
	private Question getQuestionDetails(String qNr, int sectionNr, OWLNamedIndividual ind, Set<OWLAxiom> axioms, boolean subquestion) {
		String qText = "", qFocus = ""; QuestionOptions qOpts = null;
//		if(axioms != null && !axioms.isEmpty()) { // TODO
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
//		}
		if(qOpts == null) 
			qOpts = new QuestionOptions(ind.getIRI(), ElementType.TEXTAREA, new ArrayList<String>());
		if(qOpts.getQuestionType() == null) {
			qOpts.setQuestionType(ElementType.TEXTAREA);
			System.out.println("\t!! Type for question: " + qNr.toUpperCase() + " (section " + sectionNr + ") not defined in ontology or configuration file. "
					+ "Defaulting to text area !!");
		}
		return new Question(ind, qNr, sectionNr, qText, qFocus, qOpts.getQuestionType(), qOpts.getOptions(), subquestion);
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
		Map<String,String> opts = new LinkedHashMap<String,String>();
		
		ElementType qType = null;
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
					qType = ElementType.DROPDOWN;
					opts = getOptionsFromEnumeration((OWLObjectOneOf)filler);
				}
			}
		}
		if(qType != null && qType.equals(ElementType.RADIO)) { 
			opts.put("YES", "YES");
			opts.put("NO", "NO");
		}
		questionOptions.put(questionIri.toString(), opts);
		return new QuestionOptions(questionIri, qType, new ArrayList<String>(opts.values()));
	}
	
	
	/**
	 * Get the answer options from a given class expression 
	 * @param ce	OWL class expression
	 * @return List of options represented by the given class expression
	 */
	private Map<String,String> getOptions(OWLClassExpression ce) {
		Map<String,String> opts = new HashMap<String,String>();
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
	 * Get the map of options' IRIs to options' text that are contained in an enumeration class expression 
	 * @param optsEnum	Class expression
	 * @return Map of answer options' IRIs to text
	 */
	private Map<String,String> getOptionsFromEnumeration(OWLObjectOneOf optsEnum) {
		Map<String,String> opts = new HashMap<String,String>();
		for(OWLNamedIndividual ind : optsEnum.getIndividualsInSignature()) {
			Set<OWLAxiom> usg = ont.getReferencingAxioms(ind, Imports.INCLUDED);
			for(OWLAxiom ax : usg) {
				if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
					if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(dataValueProperty)) {
						OWLLiteral lit = ((OWLDataPropertyAssertionAxiom)ax).getObject();
						opts.put(ind.getIRI().toString(), StringUtils.leftPad(lit.getLiteral(), 6, '0')); // add leading zeroes for proper sorting
					}
					else if(((OWLDataPropertyAssertionAxiom)ax).getProperty().equals(textDataProperty))
						opts.put(ind.getIRI().toString(), ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral());
				}
				else if(ax.isOfType(AxiomType.CLASS_ASSERTION)) {
					OWLClassExpression ce = ((OWLClassAssertionAxiom)ax).getClassExpression();
					if(ce instanceof OWLDataSomeValuesFrom) {
						if(((OWLDataSomeValuesFrom)ce).getProperty().equals(dataValueProperty)) {
							OWLDataRange r = ((OWLDataSomeValuesFrom)ce).getFiller();
							OWLDatatypeRestriction res = (OWLDatatypeRestriction)r;
							for(OWLFacetRestriction fr : res.getFacetRestrictions())
								opts.put(ind.getIRI().toString(), fr.getFacet().getSymbolicForm() + fr.getFacetValue().getLiteral());
						}
					}
				}
			}
		}
		return sortMap(opts);
	}
	
	
	/**
	 * Sort a given map according to its values
	 * @param map	Unsorted map
	 * @return Sorted map
	 */
	private Map<String,String> sortMap(Map<String,String> map) {
		Set<Entry<String,String>> set = map.entrySet();
        List<Entry<String,String>> list = new LinkedList<Entry<String,String>>(set);
        Collections.sort(list, new Comparator<Entry<String,String>>() {
            public int compare(Entry<String,String> o1, Entry<String,String> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            } 
        } );
        Map<String,String> newMap = new LinkedHashMap<String,String>();
        for(Entry<String,String> entry : list)
        	newMap.put(entry.getKey(), entry.getValue().replaceFirst("^0+(?!$)", "")); // remove leading zeroes
		return newMap;
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
				qFocus = ((OWLNamedIndividual)ind).getIRI().toString();
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
	public List<Section> getSections(String type) {
		System.out.println("Parsing questions and sections... ");
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = collectQuestionAxioms(type);
		List<Section> list = parseSections(map);
		System.out.println("done");
		return list;
	}
	
	
	/**
	 * Get the list of all sections and questions instantiated in the given ontology
	 * @return List of question sections
	 */
	public List<Section> getAllSections() {
		return getSections("");
	}
	
	
	/**
	 * Get a map of question IRIs to a map of answer IRIs to answer options 
	 * @return Map of question IRIs to their respective answer options and IRIs
	 */
	public Map<String,Map<String,String>> getQuestionOptions() {
		return questionOptions;
	}

	
	/**
	 * Initialize question type bindings from the configuration file   
	 */
	private void initQuestionTypes() {
		questionTypes = new HashMap<OWLClassExpression,ElementType>();
		OWLClassExpression text = df.getOWLClass(conf.getTextInputBinding());
		OWLClassExpression radio = df.getOWLClass(conf.getRadioInputBinding());
		questionTypes.put(text, ElementType.TEXTAREA);
		questionTypes.put(radio, ElementType.RADIO);
	}
	
	
	/**
	 * Initialize entity bindings from configuration file
	 */
	private void initBindings() {
		textDataProperty = df.getOWLDataProperty(conf.getQuestionTextPropertyBinding());
		valueObjectProperty = df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding());
		focusObjectProperty = df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding());
		dataValueProperty = df.getOWLDataProperty(conf.getQuestionDataValuePropertyBinding());
	}
	
	
	/**
	 * Print details about the given form element
	 * @param e	Form element
	 */
	private void printInfo(FormElement e) {
		System.out.println("\tText: " + e.getText());
		System.out.println("\tFocus: " + e.getFocus());
		System.out.println("\tType: " + e.getType());
		if(e instanceof Question) {
			Question question = (Question)e;
			System.out.print("\tOptions: ");
			if(question.getType().equals(ElementType.NONE))
				System.out.print("none");
			else if(!question.getType().equals(ElementType.TEXTAREA)) {
				for(String opt : question.getQuestionOptions())
					System.out.print(opt + " ");
			}
			else 
				System.out.print("none (text input)");
		}
		if(verbose) System.out.println();
	}
}
