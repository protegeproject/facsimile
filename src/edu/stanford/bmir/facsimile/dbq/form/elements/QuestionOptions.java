package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.util.List;

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
	private List<String> options;
	
	
	/**
	 * Constructor
	 * @param question	IRI of question individual
	 * @param type	Type of question from the QuestionType enumeration
	 * @param options	List of answer options
	 */
	public QuestionOptions(IRI question, ElementType type, List<String> options) {
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
	public List<String> getOptions() {
		return options;
	}
}
