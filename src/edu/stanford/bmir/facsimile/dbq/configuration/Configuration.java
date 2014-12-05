package edu.stanford.bmir.facsimile.dbq.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.model.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.bmir.facsimile.dbq.question.Question.QuestionType;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Configuration {
	private Document doc;
	private String ontPath, outPath, title, cssStyle;
	private Map<IRI,String> imports;
	private Map<IRI,QuestionType> questionTypes;
	private Map<IRI,List<List<IRI>>> sections;
	private File file;
	private boolean verbose;
	
	
	/**
	 * Constructor
	 * @param file	XML document file
	 * @param verbose	true for verbose mode
	 */
	public Configuration(File file, boolean verbose) {
		this.file = file;
		this.verbose = verbose;
	}
	
	
	/**
	 * Constructor 
	 * @param file	XML document file
	 */
	public Configuration(File file) {
		this(file, false);
	}
	
	
	/**
	 * Load configuration file and initialize data structures
	 */
	public void loadConfiguration() {
		doc = loadConfigurationFile(file);
		questionTypes = new HashMap<IRI,QuestionType>();
		imports = new HashMap<IRI,String>();
		sections = getSections();
		gatherOntologyFiles();
		gatherOutputInformation();
	}
	
	
	/**
	 * Load XML configuration file 
	 * @param f	File
	 * @return XML document instance 
	 */
	private Document loadConfigurationFile(File f) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(f);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		return doc;
	}
	
	
	/**
	 * Retrieve output information
	 */
	private void gatherOutputInformation() {
		NodeList nl = doc.getElementsByTagName("output");
		for(int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if(n.hasChildNodes() && n.getChildNodes().getLength() > 0) {
				for(int j = 0; j < n.getChildNodes().getLength(); j++) {
					Node c = n.getChildNodes().item(j);
					if(c.getNodeName().equalsIgnoreCase("file")) {
						outPath = c.getTextContent();
						if(c.hasAttributes() && c.getAttributes().getNamedItem("title") != null)
							title = c.getAttributes().getNamedItem("title").getTextContent();
					}
					if(c.getNodeName().equalsIgnoreCase("cssstyle"))
						cssStyle = c.getTextContent();
				}
			}
		}
	}
	
	
	/**
	 * Retrieve ontology files (including imports)
	 */
	private void gatherOntologyFiles() {
		NodeList nl = doc.getElementsByTagName("ontology");
		for(int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if(n.getParentNode().getNodeName().equalsIgnoreCase("imports")) {
				if(n.hasAttributes()) {
					String iri = n.getAttributes().getNamedItem("iri").getTextContent();
					if(iri != null)
						imports.put(IRI.create(iri), n.getTextContent());
				}
			}
			else if(n.getParentNode().getNodeName().equalsIgnoreCase("input"))
				ontPath = n.getTextContent();
		}
	}
	
	
	/**
	 * Gather a map of sections specified in the configuration file and their questions
	 * @return Map of sections' IRIs and their respective questions
	 */
	private Map<IRI,List<List<IRI>>> getSections() {
		Map<IRI,List<List<IRI>>> sections = new TreeMap<IRI,List<List<IRI>>>();
		NodeList nl = doc.getElementsByTagName("section");
		for(int i = 0; i < nl.getLength(); i++) {
			NodeList children = nl.item(i).getChildNodes();
			List<List<IRI>> questions = null; IRI section = null;
			for(int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				if(child.getNodeName().equalsIgnoreCase("iri")) {
					section = IRI.create(child.getTextContent());
					if(verbose) System.out.println("   Section: " + section);
				}
				if(child.getNodeName().equalsIgnoreCase("questionlist"))
					questions = getQuestions(child);
			}
			// TODO
//			Node s = nl.item(i).getAttributes().getNamedItem("id");
//			if(s != null && s.getTextContent().equalsIgnoreCase("init"))
//				section = IRI.create("");
			
			if(section != null && questions != null && !questions.isEmpty())
				sections.put(section, questions);
		}
		return sections;
	}
	
	
	/**
	 * Gather the list of questions in a given (questionlist) node
	 * @param n	Questionlist node
	 * @return List of question IRIs
	 */
	private List<List<IRI>> getQuestions(Node n) {
		List<List<IRI>> questions = new ArrayList<List<IRI>>();
		NodeList nl = n.getChildNodes(); // <question>'s
		for(int i = 0; i < nl.getLength(); i++) {
			NodeList children = nl.item(i).getChildNodes(); // <iri>, <subquestions>'s
			List<IRI> subquestions = new ArrayList<IRI>();
			for(int j = 0; j < children.getLength(); j++) {
				Node curNode = children.item(j);
				if(children.item(j).getNodeName().equalsIgnoreCase("iri")) {
					IRI iri = getQuestionIRI(curNode, false);
					if(iri != null) subquestions.add(0,iri);
				}
				if(children.item(j).getNodeName().equalsIgnoreCase("question")) {
					IRI iri = getQuestionIRI(curNode, true);
					if(iri != null) subquestions.add(iri);
				}
			}
			if(!subquestions.isEmpty())
				questions.add(subquestions);
		}
		return questions;
	}
	
	
	/**
	 * Get IRI of question at the given node
	 * @param node	Current node
	 * @return IRI of question in given node
	 */
	private IRI getQuestionIRI(Node node, boolean subquestion) {
		String iriTxt = node.getTextContent();
		IRI iri = null;
		if(!iriTxt.equals("")) {
			iri = IRI.create(iriTxt);
			if(iri != null && verbose) {
				if(!subquestion)
					System.out.print("\tQuestion: " + iriTxt);
				else
					System.out.print("\t   Subquestion: " + iriTxt);
			}
			if(subquestion && node.hasAttributes())
				checkQuestionType(iri, node);
			if(!subquestion && node.getParentNode().hasAttributes())
				checkQuestionType(iri, node.getParentNode());
			if(iri != null && verbose) System.out.println();
		}
		return iri;
	}

	
	/**
	 * Get the type of question given as an attribute
	 * @param iri	IRI of the question individual
	 * @param curNode	Current node being checked
	 */
	private void checkQuestionType(IRI iri, Node curNode) {
		Node n = curNode.getAttributes().getNamedItem("type");
		if(n != null) {
			QuestionType qType = null;
			String type = n.getNodeValue();
			for(int i = 0; i < QuestionType.values().length; i++) {
				if(type.equalsIgnoreCase(QuestionType.values()[i].toString()))
					qType = QuestionType.values()[i];
			}
			if(verbose) System.out.print(" (type: " + qType + ")");
			questionTypes.put(iri, qType);
		}
	}
	
	
	/*	QUESTION TYPE	*/
	
	
	/**
	 * Check if the configuration file contains a question type for the given question
	 * @param i	IRI of the question
	 * @return true if question type is defined in the configuration file, false otherwise
	 */
	public boolean hasDefinedType(IRI i) {
		if(questionTypes.containsKey(i))
			return true;
		else
			return false;
	}
	
	
	/**
	 * Get the question type for the given question
	 * @param i	IRI of individual representing a question
	 * @return Question type
	 */
	public QuestionType getQuestionType(IRI i) {
		return questionTypes.get(i);
	}
	
	
	/**
	 * Get the map of question IRIs to their respective type as defined in the configuration file
	 * @return Map of IRIs to question types
	 */
	public Map<IRI,QuestionType> getQuestionTypes() {
		return questionTypes;
	}
	
	
	/*	SECTION - QUESTION MAP	*/
	
	
	/**
	 * Get the map of sections and their corresponding questions specified in the configuration file
	 * @return Map of sections' IRIs to the questions they contain 
	 */
	public Map<IRI,List<List<IRI>>> getSectionMap() {
		return sections;
	}
	
	
	/*	INPUT AND OUTPUT	*/
	
	
	/**
	 * Get file path to main ontology input
	 * @return File path
	 */
	public String getInputOntologyPath() {
		return ontPath;
	}
	
	
	/**
	 * Get the map of imported ontologies' IRIs and file paths
	 * @return Map of IRIs and file paths of imported ontologies
	 */
	public Map<IRI,String> getInputImportsMap() {
		return imports;
	}
	
	
	/**
	 * Get the file path of the output file
	 * @return String with the file path of the output file
	 */
	public String getOutputFilePath() {
		return outPath;
	}

	
	/**
	 * Get the title of the (HTML) output file
	 * @return String describing the title of the output file
	 */
	public String getOutputFileTitle() {
		return title;
	}
	
	
	/**
	 * Get the CSS style class to be used in the form output
	 * @return String specifying the output CSS class
	 */
	public String getCSSStyleClass() {
		return cssStyle;
	}

	
	/*	CLASS BINDINGS	*/
	
	
	/**
	 * Get the OWL class IRI which represents the type of questions, i.e., questions are instances of this class
	 * @return OWL class IRI
	 */
	public IRI getQuestionInputClass() {
		return IRI.create((doc.getElementById("class").getTextContent()));
	}
	
	
	/**
	 * Get the OWL class IRI which represents the type of output class desired, i.e., 
	 * each form input will become an instance of this class
	 * @return OWL class IRI
	 */
	public IRI getQuestionOutputClass() {
		return IRI.create((doc.getElementById("outclass").getTextContent()));
	}
	
	
	/**
	 * Get the OWL class IRI which represents the type of sections, i.e., sections are instances of this class
	 * @return OWL class IRI
	 */
	public IRI getSectionInputClass() {
		return IRI.create(doc.getElementById("sectionclass").getTextContent());
	}
	
	
	/*	PROPERTY BINDINGS	*/
	
	
	/**
	 * Get the property IRI which represents the questions' text
	 * @return OWL data property IRI
	 */
	public IRI getQuestionTextPropertyBinding() {
		return IRI.create((doc.getElementById("text").getTextContent()));
	}
	

	/**
	 * Get the property IRI which represents the questions' focus
	 * @return OWL data property IRI
	 */
	public IRI getQuestionFocusPropertyBinding() {
		return IRI.create((doc.getElementById("focus").getTextContent()));
	}
	
	
	/**
	 * Get the property IRI which represents the questions' possible value(s)
	 * @return OWL object property IRI
	 */
	public IRI getQuestionValuePropertyBinding() {
		return IRI.create((doc.getElementById("value").getTextContent()));
	}
	
	
	/**
	 * Get the data property IRI which is used to represent the numeric value of a question's option
	 * @return OWL data property IRI
	 */
	public IRI getQuestionDataValuePropertyBinding() {
		return IRI.create(doc.getElementById("datavalue").getTextContent());
	}
	
	
	/**
	 * Get the OWL data property IRI that gives a heading value to a section instance
	 * @return OWL data property IRI
	 */
	public IRI getSectionHeaderPropertyBinding() {
		return IRI.create(doc.getElementById("sectionheading").getTextContent());
	}
	
	
	/**
	 * Get the OWL data property IRI that is used to specify the text at the beginning of a section
	 * @return OWL data property IRI
	 */
	public IRI getSectionTextPropertyBinding() {
		return IRI.create(doc.getElementById("sectiontext").getTextContent());
	}
	
	
	/**
	 * Get the OWL data property IRI that gives a sorting label to a section instance
	 * @return OWL data property IRI
	 */
	public IRI getSectionSortingLabelPropertyBinding() {
		return IRI.create(doc.getElementById("sectionsortinglabel").getTextContent());
	}
	
	
	/**
	 * Get the OWL object property IRI for 'hasQuestion'
	 * @return OWL object property IRI
	 */
	public IRI getSectionHasQuestionPropertyBinding() {
		return IRI.create(doc.getElementById("sectionquestion").getTextContent());
	}
	
	
	/*	QUESTION INPUT TYPES (HTML FORM)	*/
	
	
	/**
	 * Get the OWL class IRI which represents an HTML radio input element
	 * @return OWL class IRI
	 */
	public IRI getRadioInputBinding() {
		return IRI.create(doc.getElementById("radio").getTextContent());
	}
	
	
	/**
	 * Get the OWL class IRI which represents an HTML text field input element
	 * @return OWL class IRI
	 */
	public IRI getTextInputBinding() {
		return IRI.create(doc.getElementById("textfield").getTextContent());
	}
	
	
	/**
	 * Get the OWL class expression IRI which represents an HTML checkbox input element
	 * @return OWL class expression IRI
	 */
	public IRI getCheckboxInputBinding() {
		return IRI.create(doc.getElementById("checkbox").getTextContent());
	}
	
	
	/**
	 * Get the OWL class expression IRI which represents an HTML dropdown input element
	 * @return OWL class expression IRI
	 */
	public IRI getDropdownInputBinding() {
		return IRI.create(doc.getElementById("dropdown").getTextContent());
	}
	
	
	/**
	 * Get the OWL class expression IRI which represents an HTML combo box input element
	 * @return OWL class expression IRI
	 */
	public IRI getComboInputBinding() {
		return IRI.create(doc.getElementById("combo").getTextContent());
	}
}
