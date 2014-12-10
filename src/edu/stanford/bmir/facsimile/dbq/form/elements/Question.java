package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.io.Serializable;
import java.util.List;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Question extends FormDataElement implements Serializable {
	private static final long serialVersionUID = 6525002902613268136L;
	private OWLNamedIndividual ind;
	private QuestionType questionType;
	private List<String> options;
	private boolean subquestion;
	
	
	/**
	 * Constructor
	 * @param ind	Question individual
	 * @param questionNumber	Number of the question
	 * @param sectionNumber	Number of the section this question appears
	 * @param questionText	Title (text) of the question
	 * @param questionFocus	Focus of the question
	 * @param questionType	Type of question, i.e., HTML form element type
	 * @param options	List of options, i.e., possible answers to the question
	 * @param subquestion	true if question has a parent question, false otherwise
	 */
	public Question(OWLNamedIndividual ind, String questionNumber, int sectionNumber, String questionText, String questionFocus, 
			QuestionType questionType, List<String> options, boolean subquestion) {
		super(questionNumber, sectionNumber, questionText, questionFocus);
		this.ind = ind;
		this.questionType = questionType;
		this.options = options;
		this.subquestion = subquestion;
	}
	
	
	/**
	 * Get the OWL individual that represents this question
	 * @return OWL individual
	 */
	public OWLNamedIndividual getQuestionIndividual() {
		return ind;
	}
	

	/**
	 * Get the HTML form question type given by an element of the QuestionType enumeration 
	 * @return QuestionType representing the type of HTML form element
	 */
	public QuestionType getQuestionType() {
		return questionType;
	}
	
	
	/**
	 * Get the list of possible answers to this question
	 * @return List of options
	 */
	public List<String> getQuestionOptions() {
		return options;
	}
	
	
	/**
	 * Check if question is a subquestion (ie has a parent question)
	 * @return true if question is a subquestion, false otherwise
	 */
	public boolean isSubquestion() {
		return subquestion;
	}
	
	
	/**
	 * 
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * HTML form element types
	 */
	public enum QuestionType {
		TEXT, TEXTAREA, CHECKBOX, DROPDOWN, RADIO, COMBO, NONE;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
