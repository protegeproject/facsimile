package edu.stanford.bmir.facsimile.dbq.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEntity;
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

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.exception.MissingOntologyEntityException;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement.ElementType;
import edu.stanford.bmir.facsimile.dbq.form.elements.InformationElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
import edu.stanford.bmir.facsimile.dbq.form.elements.QuestionOptions;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section.SectionType;
import edu.stanford.bmir.facsimile.dbq.tree.TreeNode;

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
	private Map<IRI,Boolean> sectioNumbering, questionNumbering, questionRequired;
	private OWLOntologyManager man;
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
		sectioNumbering = conf.getSectionNumberingMap();
		questionNumbering = conf.getQuestionNumberingMap();
		questionRequired = conf.getQuestionRequiredMap();
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
	 * Parse the questions in the ontology according to the order in the configuration file
	 * @return List of questions
	 */
	private List<Section> parseSections() {
		char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		List<Section> outputSections = new ArrayList<Section>();
		Map<IRI,List<TreeNode<IRI>>> sections = conf.getSectionMap(); 
		for(IRI section : sections.keySet()) { // foreach section
			if(verbose) System.out.println(" Section: " + section.getShortForm());
			OWLEntity sectionEnt = null;
			
			if(ont.containsIndividualInSignature(section, Imports.INCLUDED))
				sectionEnt = df.getOWLNamedIndividual(section);	// section (or form element) individual
			else if(ont.containsClassInSignature(section, Imports.INCLUDED))
				sectionEnt = df.getOWLClass(section);	// section (or form element) class
			else
				throw new MissingOntologyEntityException("Section form element: " + section.toString() + " does not exist in the given ontology");
			
			SectionType sectionType = conf.getSectionType(section);
			List<TreeNode<IRI>> eleList = sections.get(section);
			List<FormElement> formElements = getFormElements(section, sectionType, eleList, alphabet);
			outputSections.add(new Section(getSectionHeader(sectionEnt), getSectionText(sectionEnt), 
					formElements, sectioNumbering.get(section), sectionType));
		}
		return outputSections;
	}
	
	
	/**
	 * Get the form elements in a given section
	 * @param section	Section IRI
	 * @param sectionType	Section type
	 * @param eleList	Element list
	 * @param alphabet	Alphabet
	 * @return List of form elements
	 */
	private List<FormElement> getFormElements(IRI section, SectionType sectionType, List<TreeNode<IRI>> eleList, char[] alphabet) {
		List<FormElement> formElements = new ArrayList<FormElement>();
		int skip_main = 0;
		for(int i = 0; i < eleList.size(); i++) {
			Iterator<TreeNode<IRI>> iter = eleList.get(i).iterator();
			int skip_sub = 0, counter = 0;
			while(iter.hasNext()) {
				TreeNode<IRI> node = iter.next();
				boolean isNumbered = true;
				String questionNr = "";
				FormElement q = null;
				if(questionNumbering.containsKey(node.data) && !questionNumbering.get(node.data)) {
					if(node.getLevel()>0) skip_sub++; 
					else skip_main++;
					isNumbered = false; 
				}
				else
					questionNr += (skip_main > 0 ? (i-skip_main >= 0 ? alphabet[i-skip_main] : 0) : alphabet[i]);
				
				OWLNamedIndividual ind = df.getOWLNamedIndividual(node.data);
				if(ont.containsIndividualInSignature(node.data)) {
					if(node.getLevel() > 0 && isNumbered)
						questionNr += "" + (skip_sub > 0 ? counter-skip_sub : counter);
					q = getQuestionDetails(questionNr, i, ind, node.getLevel());
					
					if(verbose) System.out.println("    Question: " + ind.getIRI().getShortForm());
					addSubquestionList((Question)q, node);
					addSuperquestionList((Question)q, node);
					if(!node.isRoot())
						q.setParentQuestion(node.parent.data);
				}
				else if(ont.containsEntityInSignature(node.data))
					q = getInformationElement(node.data, sectionType, section, questionNr, i);
				else
					throw new MissingOntologyEntityException("Question or info form element: " + node.data.toString() + " does not exist in the given ontology");
				
				if(q != null) {
					if(verbose) printInfo(q);
					formElements.add(q);
				}
				counter++;
			}
		}
		return formElements;
	}
	
	
	/**
	 * Add subquestion list
	 * @param q	Question instance
	 * @param node	Treenode
	 */
	private void addSubquestionList(Question q, TreeNode<IRI> node) {
		Iterator<TreeNode<IRI>> iter = node.iterator();
		String entIri = q.getEntity().getIRI().toString();
		Set<IRI> ignored = new HashSet<IRI>();
		while(iter.hasNext()) {
			TreeNode<IRI> child = iter.next();
			if(!child.data.toString().equalsIgnoreCase(entIri) && !ignored.contains(child.data)) {
				if(conf.getSubquestionPositiveTriggers().containsKey(child.data) || conf.getSubquestionNegativeTriggers().containsKey(child.data))
					addNode(ignored, child.children);
				if(!q.getSubquestions().contains(child.data))
					q.addSubquestion(child.data);
			}
		}
	}
	
	
	/**
	 * Add superquestion list
	 * @param q	Question instance
	 * @param node	Treenode
	 */
	private void addSuperquestionList(Question q, TreeNode<IRI> node) {
		TreeNode<IRI> parent = node.parent;
		if(parent != null) {
			q.addSuperquestions(parent.data);
			addSuperquestionList(q, parent);
		}
	}
	
	
	/**
	 * Add all nodes in a node list to a set of (ignored question) IRIs 
	 * @param set	Set of IRIs
	 * @param nodeList	Node list
	 */
	private void addNode(Set<IRI> set, List<TreeNode<IRI>> nodeList) {
		for(TreeNode<IRI> treeNode : nodeList) {
			Iterator<TreeNode<IRI>> iter = treeNode.iterator();
			while(iter.hasNext()) {
				TreeNode<IRI> node = iter.next();
				set.add(node.data);
			}
		}
	}
	
	
	/**
	 * Create an instance of an information element
	 * @param eleIri	Element IRI
	 * @param sectionType	Section type
	 * @param sectionIri	Section IRI
	 * @param eleNr	Element number
	 * @param sectionNr	Section number
	 * @return Instance of InformationElement
	 */
	private InformationElement getInformationElement(IRI eleIri, SectionType sectionType, IRI sectionIri, String eleNr, int sectionNr) {
		String eleTxt = "";
		OWLDataProperty eleDP = df.getOWLDataProperty(eleIri);
		if(verbose) System.out.println("    Information element: " + eleIri.getShortForm());

		Set<OWLAnnotationAssertionAxiom> axs = ont.getAnnotationAssertionAxioms(eleDP.getIRI());
		for(OWLAnnotationAssertionAxiom annAx : axs)
			if(annAx.getProperty().isComment())
				eleTxt = annAx.getValue().asLiteral().get().getLiteral();
		
		ElementType type = null;
		if(conf.hasDefinedType(eleIri))
			type = conf.getQuestionType(eleIri);
		else
			type = ElementType.TEXTAREA;
		return new InformationElement(eleDP, eleNr, sectionNr, eleTxt, sectionIri.toString(), type, questionRequired.get(eleIri));
	}
	
	
	/**
	 * Get the section header for a given section IRI
	 * @param ent	Section entity
	 * @return Header of the section
	 */
	private String getSectionHeader(OWLEntity ent) {
		String header = "";
		OWLDataProperty dp = df.getOWLDataProperty(conf.getSectionHeaderPropertyBinding());		
		for(OWLAxiom ax : ont.getReferencingAxioms(ent)) {
			if(ax instanceof OWLDataPropertyAssertionAxiom && ax.containsEntityInSignature(dp))
				header = ((OWLDataPropertyAssertionAxiom)ax).getObject().getLiteral();
		}
		return header;
	}
	
	
	/**
	 * Get the text that appears at the beginning of a given section 
	 * @param ent	Section entity
	 * @return Text of a section
	 */
	private String getSectionText(OWLEntity ent) {
		String text = "";
		OWLDataProperty dp = df.getOWLDataProperty(conf.getSectionTextPropertyBinding());
		for(OWLAxiom ax : ont.getReferencingAxioms(ent)) {
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
	 * @param indentLevel	Indentation level for sub-questions 
	 * @return Question instance
	 */
	private Question getQuestionDetails(String qNr, int sectionNr, OWLNamedIndividual ind, int indentLevel) {
		String qText = "", qFocus = ""; QuestionOptions qOpts = null;
		for(OWLAxiom ax : ont.getReferencingAxioms(ind)) {
			if(ax.isLogicalAxiom()) {
				if(ax.isOfType(AxiomType.DATA_PROPERTY_ASSERTION) && qText.isEmpty())
					qText = getQuestionText(ax, textDataProperty);
				if(ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION) && qFocus.isEmpty())
					qFocus = getQuestionFocus(ax, focusObjectProperty);
				if(ax.isOfType(AxiomType.CLASS_ASSERTION) && (qOpts == null || qOpts.getQuestionType() == null) && 
						((OWLClassAssertionAxiom)ax).getClassExpression().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
					qOpts = getQuestionOptions(ind.getIRI(), ax, valueObjectProperty);
			}
		}
		if(qOpts == null) {
			if(conf.hasDefinedType(ind.getIRI()))
				qOpts = new QuestionOptions(ind.getIRI(), conf.getQuestionType(ind.getIRI()), new HashMap<String,String>());
			else
				qOpts = new QuestionOptions(ind.getIRI(), ElementType.TEXTAREA, new HashMap<String,String>());
		}
		if(qOpts.getQuestionType() == null) {
			qOpts.setQuestionType(ElementType.TEXTAREA);
			System.err.println("\n! Warning: Type for question " + ind.getIRI().toString() + " is not defined in the ontology or configuration file. Defaulting to text area.");
		}
		return new Question(ind, qNr, sectionNr, qText, qFocus, qOpts.getQuestionType(), qOpts, indentLevel, questionRequired.get(ind.getIRI()));
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
			opts.put(conf.getBooleanTrueValueBinding().toString(), "Yes");
			opts.put(conf.getBooleanFalseValueBinding().toString(), "No");
		}
		questionOptions.put(questionIri.toString(), opts);
		return new QuestionOptions(questionIri, qType, opts);
	}
	
	
	/**
	 * Get the answer options from a given class expression 
	 * @param ce	OWL class expression
	 * @return List of options represented by the given class expression
	 */
	private Map<String,String> getOptions(OWLClassExpression ce) {
		Map<String,String> opts = new HashMap<String,String>();
		if(((OWLObjectSomeValuesFrom)ce).getProperty().equals(valueObjectProperty)) {
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)ce).getFiller();
			if(filler instanceof OWLObjectOneOf)
				opts = getOptionsFromEnumeration((OWLObjectOneOf)filler);
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
			usg.addAll(ont.getAnnotationAssertionAxioms(ind.getIRI()));
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
				else if(ax.isOfType(AxiomType.ANNOTATION_ASSERTION)) {
					OWLAnnotationAssertionAxiom ann_ax = (OWLAnnotationAssertionAxiom)ax;
					if(ann_ax.getProperty().equals(df.getRDFSLabel())) 
						opts.put(ind.getIRI().toString(), ann_ax.getValue().asLiteral().get().getLiteral().toString());
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
        for(Entry<String,String> entry : list) {
        	String value = entry.getValue().replaceFirst("^0+(?!$)", "");
        	newMap.put(entry.getKey(), (value.startsWith("/") ? value = "0" + value : value)); // remove leading zeroes
        }
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
	public List<Section> getAllSections() {
		System.out.print("Parsing questions and sections... ");
		if(verbose) System.out.println();
		List<Section> list = parseSections();
		System.out.println("done");
		return list;
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
		if(!e.getText().isEmpty()) System.out.println("\tText: " + e.getText());
		if(e.getFocus() != null && !e.getFocus().isEmpty()) System.out.println("\tFocus: " + e.getFocus());
		System.out.println("\tType: " + e.getType());
		if(e instanceof Question) {
			Question question = (Question)e;
			System.out.print("\tOptions: ");
			if(question.getType().equals(ElementType.NONE))
				System.out.print("none");
			else if(!question.getType().equals(ElementType.TEXTAREA) && !question.getType().equals(ElementType.TEXT)) {
				for(String opt : question.getQuestionOptions().getOptionsValues())
					System.out.print(opt + " ");
			}
			else 
				System.out.print("none (text input)");
			System.out.println();
		}
	}
}
