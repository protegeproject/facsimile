package edu.stanford.bmir.facsimile.dbq.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;

import edu.stanford.bmir.facsimile.dbq.question.Question;
import edu.stanford.bmir.facsimile.dbq.question.Question.QuestionType;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
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
	 * @param f	Output file
	 * @param title	Title of the HTML webpage
	 * @return Document representing an HTML form
	 * @throws IOException 
	 */
	public Document generateHTMLForm(File f, String title) throws IOException {
		if(verbose) System.out.println("Generating HTML form... ");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write("<html><head><title>" + title + "</title></head><body>");
		bw.write("<form>");
		for(Question q : questions) {
			bw.write("<p>");
			QuestionType type = q.getQuestionType();
			switch(type) {
			case CHECKBOX: bw.write("");
				break;
			case COMBO:
				break;
			case DROPDOWN:
				break;
			case RADIO:
				break;
			case TEXTFIELD:
				break;
			default:
				break;
			}
			bw.write("</p>");
		}
		bw.write("</form></body></html>");
		bw.close();
		return null;
	}
	
}
