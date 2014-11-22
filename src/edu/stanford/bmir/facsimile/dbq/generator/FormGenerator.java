package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.List;

import org.w3c.dom.Document;

import edu.stanford.bmir.facsimile.dbq.question.Question;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class FormGenerator {
	private List<Question> questions;
	private boolean verbose;
	
	
	/**
	 * Constructor
	 * @param questions	List of questions to populate the form
	 * @param verbose	true for verbose mode
	 */
	public FormGenerator(List<Question> questions, boolean verbose) {
		this.questions = questions;
		this.verbose = verbose;
	}
	
	
	/**
	 * Constructor
	 * @param questions	List of questions to populate the form
	 */
	public FormGenerator(List<Question> questions) {
		this(questions, false);
	}
	
	
	/**
	 * Generate the HTML form
	 * @return Document representing an HTML form
	 */
	public Document generateHTMLForm() {
		if(verbose) System.out.println("Generating HTML form... ");
		
		// TODO
		
		return null;
	}
	
}
