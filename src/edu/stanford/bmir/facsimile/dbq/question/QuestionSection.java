package edu.stanford.bmir.facsimile.dbq.question;

import java.util.List;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionSection {
	private String header;
	private List<IRI> questions;
	
	
	/**
	 * Constructor
	 * @param header	Section header/title
	 * @param questions	List of questions (IRIs)
	 */
	public QuestionSection(String header, List<IRI> questions) {
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
	public List<IRI> getSectionQuestions() {
		return questions;
	}
	
	
	/**
	 * Check if this section contains a specified question
	 * @param i	IRI of the question individual
	 * @return true if this section contains the given question, false otherwise
	 */
	public boolean containsQuestion(IRI i) {
		if(questions.contains(i))
			return true;
		else 
			return false;
	}
}
