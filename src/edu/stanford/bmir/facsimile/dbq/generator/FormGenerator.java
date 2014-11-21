package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.Set;

import org.w3c.dom.Document;

import edu.stanford.bmir.facsimile.dbq.question.Question;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class FormGenerator {
	private Set<Question> questions;
	private boolean verbose;
	
	
	/**
	 * Constructor
	 * @param questions	Set of questions to populate the form
	 * @param verbose	true for verbose mode
	 */
	public FormGenerator(Set<Question> questions, boolean verbose) {
		this.questions = questions;
		this.verbose = verbose;
	}
	
	
	/**
	 * Constructor
	 * @param questions	Set of questions to populate the form
	 */
	public FormGenerator(Set<Question> questions) {
		this(questions, false);
	}
	
	
	/**
	 * Generate the HTML form
	 * @return Document representing an HTML form
	 */
	public Document generateHTMLForm() {
		// TODO
		return null;
	}
	
}
