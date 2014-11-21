package edu.stanford.bmir.facsimile.dbq.configuration;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.model.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class Configuration {
	private Document doc;
	private BiMap<Integer,IRI> questions;
	private BiMap<IRI,Integer> questionsInv;
	
	/**
	 * Constructor
	 * @param file	XML document file
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Configuration(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		this.doc = db.parse(file);
		questions = getQuestionOrdering();
		questionsInv = questions.inverse();
	}
	
	
	/**
	 * Get a map which represents the ordering of questions according to the configuration file
	 * @return Map of questions to the individual IRI they stand for   
	 */
	private BiMap<Integer,IRI> getQuestionOrdering() {
		System.out.println("Checking configuration file for question order... ");
		BiMap<Integer,IRI> qs = HashBiMap.create();
		NodeList nl = doc.getElementsByTagName("Question");
		for(int i = 0; i < nl.getLength(); i++) {
			NodeList children = nl.item(i).getChildNodes();
			String iri = ""; int nr = 0;
			for(int j = 0; j < children.getLength(); j++) {
				String nodename = children.item(j).getNodeName();
				if(nodename.equalsIgnoreCase("number"))
					nr = Integer.parseInt(children.item(j).getTextContent());
				
				if(nodename.equalsIgnoreCase("iri"))
					iri = children.item(j).getTextContent();
			}
			qs.put(nr, IRI.create(iri));
			System.out.println("\tQuestion " + nr + ": " + iri);
		}
		return qs;
	}
	
	
	/**
	 * Check if the question map contains a given question (represented by its IRI)
	 * @param i	IRI of individual representing a question
	 * @return true if configuration file specifies this question, false otherwise
	 */
	public boolean containsQuestion(IRI i) {
		if(questions.containsValue(i))
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
			return questionsInv.get(i);
		else
			return 0;
	}
	
	
	/**
	 * Get the property IRI which represents the questions' text
	 * @return OWL data property IRI
	 */
	public IRI getQuestionTextPropertyBinding() {
		return IRI.create((doc.getElementById("text").getTextContent()));
	}
	

	//TODO
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
}
