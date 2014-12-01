package edu.stanford.bmir.facsimile.dbq.question;

import java.util.List;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionSection {
	private String header;
	private List<Question> questions;
	
	
	/**
	 * Constructor
	 * @param header	Section header/title
	 * @param questions	List of questions (IRIs)
	 */
	public QuestionSection(String header, List<Question> questions) {
		this.header = header;
		this.questions = questions;
	}
	
	
	/**
	 * Get the header of the section
	 * @return Section header
	 */
	public String getSectionHeader() {
		return header;
	}
	
	
	/**
	 * Get the list of questions this section contains
	 * @return List of questions' IRIs contained in this section
	 */
	public List<Question> getSectionQuestions() {
		return questions;
	}
	
	
	/**
	 * Check if this section contains a specified question
	 * @param i	Question instance
	 * @return true if this section contains the given question, false otherwise
	 */
	public boolean containsQuestion(Question i) {
		if(questions.contains(i))
			return true;
		else 
			return false;
	}
}
