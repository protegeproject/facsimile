package edu.stanford.bmir.facsimile.dbq.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private List<IRI> questionList, sectionList;
	private String ontPath, outPath, title;
	private Map<IRI,String> imports;
	private boolean verbose;
	private Map<IRI,QuestionType> questionTypes;
	
	/**
	 * Constructor
	 * @param file	XML document file
	 * @param verbose	true for verbose mode
	 */
	public Configuration(File file, boolean verbose) {
		this.verbose = verbose;
		doc = loadConfigurationFile(file);
		questionTypes = new HashMap<IRI,QuestionType>();
		imports = new HashMap<IRI,String>();
		questionList = getQuestions();
		sectionList = getSections();
		gatherOntologyFiles();
		gatherOutputInformation();
	}
	
	
	/**
	 * Constructor 
	 * @param file	XML document file
	 */
	public Configuration(File file) {
		this(file, false);
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
	 * Gather a list of sections specified in the configuration file
	 * @return List of sections' IRIs
	 */
	private List<IRI> getSections() {
		List<IRI> list = new ArrayList<IRI>();
		NodeList nl = doc.getElementsByTagName("section");
		for(int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			for(int j = 0; j < n.getChildNodes().getLength(); j++) {
				Node child = n.getChildNodes().item(j);
				if(child.getNodeName().equalsIgnoreCase("iri"))
					list.add(IRI.create(child.getTextContent()));
			}
		}
		return list;
	}
	
	
	/**
	 * Gather a list questions as they are ordered in the configuration file
	 * @return List of questions' IRIs
	 */
	private List<IRI> getQuestions() {
		List<IRI> list = new ArrayList<IRI>();
		if(verbose) System.out.println("Checking configuration file for question order... ");
		NodeList nl = doc.getElementsByTagName("question");
		for(int i = 0; i < nl.getLength(); i++) {
			Node curNode = nl.item(i);
			IRI iri = IRI.create(getIRI(curNode));
			list.add(iri);
			if(verbose) System.out.print("\tQuestion " + (i+1) + ": " + iri);
			
			if(curNode.hasAttributes())
				checkQuestionType(iri, curNode);
			if(verbose) System.out.println();
		}
		return list;
	}
	
	
	/**
	 * Get the IRI of the given node in the configuration file
	 * @param curNode	Current node
	 * @return String representation of the IRI
	 */
	private String getIRI(Node curNode) {
		NodeList children = curNode.getChildNodes();
		String iri = "";
		for(int j = 0; j < children.getLength(); j++) {
			if(children.item(j).getNodeName().equalsIgnoreCase("iri"))
				iri = children.item(j).getTextContent();
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
			for(int i = 0; i<QuestionType.values().length; i++) {
				if(type.equalsIgnoreCase(QuestionType.values()[i].toString()))
					qType = QuestionType.values()[i];
			}
			if(verbose) System.out.print(" (type: " + qType + ")");
			questionTypes.put(iri, qType);
		}
	}
	
	
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
	 * Check if the question map contains a given question (represented by its IRI)
	 * @param i	IRI of individual representing a question
	 * @return true if configuration file specifies this question, false otherwise
	 */
	public boolean containsQuestion(IRI i) {
		if(questionList.contains(i))
			return true;
		else
			return false;
	}
	
	
	/**
	 * Get the question number for a given question (represented by its IRI)
	 * @param i	IRI of individual representing a question
	 * @return Question number as an integer
	 */
	public Integer getQuestionNumber(IRI i) {
		if(containsQuestion(i))
			return questionList.indexOf(i)+1;
		else
			return 0;
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
	 * Get the OWL class IRI which represents the type of questions, i.e., questions are instances of this class
	 * @return OWL class IRI
	 */
	public IRI getQuestionClass() {
		return IRI.create((doc.getElementById("class").getTextContent()));
	}
	
	
	/**
	 * Get the OWL class IRI which represents the type of output class desired, i.e., 
	 * each form input will become an instance of this class
	 * @return OWL class IRI
	 */
	public IRI getOutputQuestionClass() {
		return IRI.create((doc.getElementById("outclass").getTextContent()));
	}
	
	
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
	
	
	/**
	 * Get the list of questions (sorted by configuration file parsing order)
	 * @return List of ordered questions
	 */
	public List<IRI> getQuestionOrder() {
		return questionList;
	}
	
	
	/**
	 * Get the map of question IRIs to their respective type as defined in the configuration file
	 * @return Map of IRIs to question types
	 */
	public Map<IRI,QuestionType> getQuestionTypes() {
		return questionTypes;
	}
	
	
	/**
	 * Get file path to main ontology input
	 * @return File path
	 */
	public String getOntologyPath() {
		return ontPath;
	}
	
	
	/**
	 * Get the map of imported ontologies' IRIs and file paths
	 * @return Map of IRIs and file paths of imported ontologies
	 */
	public Map<IRI,String> getImportsMap() {
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
	 * Get the list of sections specified in the configuration file
	 * @return List of sections' IRIs
	 */
	public List<IRI> getSectionsList() {
		return sectionList;
	}
}
