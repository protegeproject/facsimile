package edu.stanford.bmir.facsimile.dbq.question;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class Question {
	private int questionNumber;
	private String questionText, questionFocus;
	private QuestionType questionType;
	
	/**
	 * Constructor
	 * @param questionNumber	Number of the question
	 * @param questionText	Title (text) of the question
	 * @param questionFocus	Focus of the question
	 * @param questionType	Type of question, i.e., HTML form element type
	 */
	public Question(int questionNumber, String questionText, String questionFocus, QuestionType questionType) {
		this.questionNumber = questionNumber;
		this.questionText = questionText;
		this.questionFocus = questionFocus;
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
	 * HTML form element types
	 */
	public enum QuestionType {
		TEXTFIELD, CHECKBOX, DROPDOWN, RADIO, COMBO;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
