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
	 * @param questionNumber
	 * @param questionTitle
	 * @param questionType
	 */
	public Question(int questionNumber, String questionTitle, QuestionType questionType) {
		this.questionNumber = questionNumber;
		this.questionTitle = questionTitle;
		this.questionType = questionType;
	}
	
	
	public int getQuestionNumber() {
		return questionNumber;
	}
	
	public String getQuestionTitle() {
		return questionTitle;
	}
	
	public QuestionType getQuestionType() {
		return questionType;
	}
	
	
	/**
	 * HTML form element types
	 */
	public enum QuestionType {
		TEXTFIELD, CHECKBOX, DROPDOWN, RADIO, COMBO
	}
}
