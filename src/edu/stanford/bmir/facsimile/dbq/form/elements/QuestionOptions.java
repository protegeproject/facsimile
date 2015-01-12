package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;

import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement.ElementType;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionOptions {
	private IRI question;
	private ElementType type; 
	private Map<String,String> options;
	
	
	/**
	 * Constructor
	 * @param question	IRI of question individual
	 * @param type	Type of question from the QuestionType enumeration
	 * @param options	Map of answer option IRIs to text values
	 */
	public QuestionOptions(IRI question, ElementType type, Map<String,String> options) {
		this.question = question;
		this.type = type;
		this.options = options;
	}
	
	
	/**
	 * Set the question type for this question
	 * @param type	Type of question
	 */
	public void setQuestionType(ElementType type) {
		this.type = type;
	}
	
	
	/**
	 * Get the IRI of this question
	 * @return IRI of question individual
	 */
	public IRI getQuestionIRI() {
		return question;
	}
	
	
	/**
	 * Get the type of question
	 * @return QuestionType
	 */
	public ElementType getQuestionType() {
		return type;
	}
	
	
	/**
	 * Get the list of answer options for this question
	 * @return List of options
	 */
	public List<String> getOptionsValues() {
		return new ArrayList<String>(options.values());
	}
	
	
	/**
	 * Get the map of question option IRIs to their values
	 * @return Map of question option IRIs to their values
	 */
	public Map<String,String> getOptionsMap() {
		return options;
	}
}
