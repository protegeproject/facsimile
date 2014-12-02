package edu.stanford.bmir.facsimile.dbq.question;

import java.util.List;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Question {
	private int questionNumber, sectionNumber;
	private String questionText, questionFocus;
	private QuestionType questionType;
	private List<String> options;
	
	
	/**
	 * Constructor
	 * @param questionNumber	Number of the question
	 * @param sectionNumber	Number of the section this question appears
	 * @param questionText	Title (text) of the question
	 * @param questionFocus	Focus of the question
	 * @param questionType	Type of question, i.e., HTML form element type
	 * @param options	List of options, i.e., possible answers to the question
	 */
	public Question(int questionNumber, int sectionNumber, String questionText, String questionFocus, 
			QuestionType questionType, List<String> options) {
		this.questionNumber = questionNumber;
		this.sectionNumber = sectionNumber; 
		this.questionText = questionText;
		this.questionFocus = questionFocus;
		this.questionType = questionType;
		this.options = options;
	}
	
	
	/**
	 * Get the question number as established by the configuration file, or by its parse order
	 * @return Integer representing the question number
	 */
	public int getQuestionNumber() {
		return questionNumber;
	}
	
	
	/**
	 * Get the section number this question appears in
	 * @return Integer representing the section number
	 */
	public int getSectionNumber() {
		return sectionNumber;
	}
	
	
	/**
	 * Get the title of the question
	 * @return String representing the question title
	 */
	public String getQuestionText() {
		return questionText;
	}
	
	
	/**
	 * Get the focus of the question (output type class)
	 * @return String representing the focus of the question
	 */
	public String getQuestionFocus() {
		return questionFocus;
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
	 * 
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * HTML form element types
	 */
	public enum QuestionType {
		TEXT, TEXTAREA, CHECKBOX, DROPDOWN, RADIO, COMBO;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
