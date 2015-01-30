package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.util.List;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class QuestionList {
	private List<IRI> questions;
	private QuestionListType type;
	private int repeat;
	
	
	/**
	 * Constructor
	 * @param questions	List of question IRIs
	 */
	public QuestionList(List<IRI> questions) {
		this.questions = questions;
		type = QuestionListType.NORMAL;
		repeat = 0;
	}
	
	
	/**
	 * Get the list of question IRIs in this question list
	 * @return List of IRIs
	 */
	public List<IRI> getQuestions() {
		return questions;
	}
		
	
	/**
	 * Get the type of question list
	 * @return Type of question list
	 */
	public QuestionListType getType() {
		return type;
	}
	
	
	/**
	 * Get the number of repetitions of this question list
	 * @return Number of repetitions
	 */
	public int getRepetitions() {
		return repeat;
	}
	
	
	/**
	 * Set the number of times this question list should be displayed
	 * @param reps	Number of repetitions
	 */
	public void setRepetitions(int reps) {
		repeat = reps;
	}
	
	
	/**
	 * Set the type of question list
	 * @param type	Type of question list
	 */
	public void setType(QuestionListType type) {
		this.type = type;
	}
	
	
	/**
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * Question list types
	 */
	public enum QuestionListType {
		NORMAL, INLINE, REPEATED, INLINEREPEATED;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}