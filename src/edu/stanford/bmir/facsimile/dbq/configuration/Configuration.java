package edu.stanford.bmir.facsimile.dbq.configuration;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.model.IRI;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class Configuration {
	private Document doc;
	
	/**
	 * Constructor
	 * @param file
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Configuration(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		this.doc = db.parse(file);
	}
	
	
	public IRI getQuestionTextPropertyBinding() {
		return IRI.create((doc.getElementById("text").getTextContent()));
	}
	

	public IRI getQuestionFocusPropertyBinding() {
		return IRI.create((doc.getElementById("focus").getTextContent()));
	}
	
	
	public IRI getQuestionValuePropertyBinding() {
		return IRI.create((doc.getElementById("value").getTextContent()));
	}
	
	
	public IRI getQuestionClass() {
		return IRI.create((doc.getElementById("class").getTextContent()));
	}
	
	public IRI getOutputQuestionClass() {
		return IRI.create((doc.getElementById("outclass").getTextContent()));
	}
}
