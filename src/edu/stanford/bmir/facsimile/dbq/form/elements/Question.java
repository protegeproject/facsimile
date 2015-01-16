package edu.stanford.bmir.facsimile.dbq.form.elements;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Question extends FormElement {
	private static final long serialVersionUID = 1L;
	private QuestionOptions options;
	private int indentLevel;
	
	
	/**
	 * Constructor
	 * @param ind	Question individual
	 * @param questionNumber	Number of the question
	 * @param sectionNumber	Number of the section this question appears
	 * @param questionText	Title (text) of the question
	 * @param questionFocus	Focus of the question
	 * @param questionType	Type of question, i.e., HTML form element type
	 * @param options	List of options, i.e., possible answers to the question
	 * @param indentLevel	Indentation level of question
	 * @param required	Whether the form element is requires input
	 */
	public Question(OWLNamedIndividual ind, String questionNumber, int sectionNumber, String questionText, String questionFocus, 
			ElementType questionType, QuestionOptions options, int indentLevel, boolean required) {
		super(ind, questionNumber, sectionNumber, questionText, questionFocus, questionType, required);
		this.options = options;
		this.indentLevel = indentLevel;
	}
		
	
	/**
	 * Get the question options
	 * @return QuestionOptions instance
	 */
	public QuestionOptions getQuestionOptions() {
		return options;
	}
	
	
	/**
	 * Check if question is a subquestion (ie has a parent question)
	 * @return true if question is a subquestion, false otherwise
	 */
	public boolean isSubquestion() {
		if(indentLevel > 0)
			return true;
		else
			return false;
	}
	
	
	/**
	 * Get the indentation level of this question
	 * @return Level of indentation
	 */
	public int getLevel() {
		return indentLevel;
	}
}
