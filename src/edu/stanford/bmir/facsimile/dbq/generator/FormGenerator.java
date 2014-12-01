package edu.stanford.bmir.facsimile.dbq.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;

import edu.stanford.bmir.facsimile.dbq.question.Question;
import edu.stanford.bmir.facsimile.dbq.question.QuestionSection;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormGenerator {
	private List<QuestionSection> questionSections;
	private boolean verbose;
	
	
	/**
	 * Constructor
	 * @param questionSections	List of questions to populate the form
	 * @param verbose	true for verbose mode
	 */
	public FormGenerator(List<QuestionSection> questionSections, boolean verbose) {
		this.questionSections = questionSections;
		this.verbose = verbose;
	}
	
	
	/**
	 * Constructor
	 * @param questions	List of questions to populate the form
	 */
	public FormGenerator(List<QuestionSection> questions) {
		this(questions, false);
	}
	
	
	/**
	 * Generate the HTML form
	 * @param f	Output file
	 * @param title	Title of the HTML webpage
	 * @return Document representing an HTML form
	 * @throws IOException 	IO error
	 */
	public Document generateHTMLForm(File f, String title) throws IOException {
		if(verbose) System.out.print("Generating HTML form... ");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write("<html>\n<head>\n<title>" + title + "</title>\n</head>\n<body>\n");
		bw.write("<h1>DBQ Form</h1><br>\n<form>\n");
		for(int i = 0; i < questionSections.size(); i++) {
			QuestionSection s = questionSections.get(i);
			bw.write("<hr>\n<h2>Section " + (i+1) + ": " + s.getSectionHeader() + "</h2>\n");
			for(Question q : s.getSectionQuestions()) { 
				bw.write("<p>" + (q.getQuestionNumber()+1) + ") " + q.getQuestionText() + "<br>\n");
				writeOutQuestion(bw, q);
				bw.write("</p>\n");
			}
		}
		bw.write("<hr><input type=\"submit\" value=\"Submit\">\n");
		bw.write("</form>\n</body>\n</html>\n");
		bw.close();
		if(verbose) System.out.println("done");
		return null;
	}
	
	
	/**
	 * Write to the given writer the details of the question
	 * @param bw	Buffered writer
	 * @param q	Question instance
	 * @throws IOException	IO error
	 */
	private void writeOutQuestion(BufferedWriter bw, Question q) throws IOException {
		switch(q.getQuestionType()) {
		case CHECKBOX:
			for(String opt : q.getQuestionOptions())
				bw.write("<input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=\"q" + (q.getQuestionNumber()+1) 
						+ "\" value=\"" + opt.toLowerCase() + "\"/>" + opt.toLowerCase() + "\n");
			break;
		case DROPDOWN:
//			bw.write("<input list=\"q" + (q.getQuestionNumber()+1) + "\"/>\n");
			bw.write("<select id=\"q" + (q.getQuestionNumber()+1) + "\">\n");
			for(String opt : q.getQuestionOptions())
				bw.write("<option value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</option>\n");
			bw.write("</select>\n");
			break;
		case RADIO:
			for(String opt : q.getQuestionOptions())
				bw.write("<input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=\"q" + (q.getQuestionNumber()+1) 
						+ "\" value=\"" + opt + "\"/>" + opt + "\n");
			break;
		case TEXTFIELD:
			bw.write("<input type=\"text\" size=\"200\"/>\n");
			break;
		case COMBO:
			break;
		default:
			break;
		}
	}
}
