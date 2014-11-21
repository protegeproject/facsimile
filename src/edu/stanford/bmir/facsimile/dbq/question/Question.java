package edu.stanford.bmir.facsimile.dbq.question;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class Question {
	private int questionNumber;
	private String questionTitle;
	private QuestionType questionType;
	
	/**
	 * Constructor
	 * @param questionNumber	Number of the question
	 * @param questionTitle	Title (text) of the question
	 * @param questionType	Type of question, i.e., HTML form element type
	 */
	public Question(int questionNumber, String questionTitle, QuestionType questionType) {
		this.questionNumber = questionNumber;
		this.questionTitle = questionTitle;
		this.questionType = questionType;
	}
	
	
	/**
	 * Get the question number as established by the configuration file, or by its parse order
	 * @return Integer representing the question number
	 */
	public int getQuestionNumber() {
		return questionNumber;
	}
	
	
	/**
	 * Get the title of the question
	 * @return String representing the question title
	 */
	public String getQuestionTitle() {
		return questionTitle;
	}
	
	
	/**
	 * Get the HTML form question type given by an element of the QuestionType enumeration 
	 * @return QuestionType representing the type of HTML form element
	 */
	public QuestionType getQuestionType() {
		return questionType;
	}
	
	
	/**
	 * HTML form element types
	 */
	public enum QuestionType {
		TEXTFIELD, CHECKBOX, DROPDOWN, RADIO, COMBO;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
