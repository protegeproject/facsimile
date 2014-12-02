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
	 * @param cssClass	CSS style class to be used 
	 * @return Document representing an HTML form
	 * @throws IOException 	IO error
	 */
	public Document generateHTMLForm(File f, String title, String cssClass) throws IOException {
		if(verbose) System.out.print("Generating HTML form... ");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write("<!DOCTYPE html>\n<html>\n<head>\n<title>" + title + "</title>\n<meta charset=\"utf-8\"/>\n");
		bw.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n");
		bw.write("<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\">\n");
		bw.write("</head>\n<body>\n<div class=\"" + cssClass + "\">\n");
		bw.write("<h1>DBQ Form<span>Please answer all questions and submit your answers at the end</span></h1><br>\n");
		bw.write("<form action=\"\" method=\"post\" id=\"form\">\n");
		for(int i = 0; i < questionSections.size(); i++) {
			QuestionSection s = questionSections.get(i);
			bw.write("<div class=\"section\"><span>" + (i+1) + "</span>" + s.getSectionHeader() + "</div><br>\n");
			List<Question> questions = s.getSectionQuestions();
			for(int j = 0; j < questions.size(); j++)
				writeOutQuestion(bw, questions.get(j));
			if(i<questionSections.size()-1) bw.write("<br><hr><br>\n");
		}
		bw.write("<br><br>\n<div class=\"button-section\"><input type=\"submit\" value=\"Submit\"/></div>\n");
		bw.write("</form>\n</div>\n</body>\n</html>\n");
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
		String qName = getQuestionName(q);
		char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		String qNumber = "" + alphabet[q.getQuestionNumber()-1];
		
		String labelInit = "<p>" + qNumber.toUpperCase() + ") " + q.getQuestionText() + "<br><br>\n";
		bw.write("<div class=\"inner-wrap\">\n");
		switch(q.getQuestionType()) {
		case CHECKBOX:
			bw.write(labelInit);
			for(String opt : q.getQuestionOptions())
				bw.write("<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
						+ " value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</label>\n");
			break;
		case DROPDOWN:
			bw.write(labelInit + "<select name=" + qName + ">\n");
			for(String opt : q.getQuestionOptions())
				bw.write("<option value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</option>\n");
			bw.write("</select>\n");
			break;
		case RADIO:
			bw.write(labelInit);
			for(String opt : q.getQuestionOptions())
				bw.write("<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
						+ " value=\"" + opt + "\">" + opt + "</label>\n");
			break;
		case TEXTAREA:
			bw.write(labelInit + "<textarea name=" + qName + "></textarea>\n");
			break;
		case TEXT:
			bw.write(labelInit + "<input type=\"text\" name=" + qName + "/>\n");
			break;
		case COMBO:
			bw.write(labelInit);
			break;
		default:
			break;
		}
		bw.write("</p>\n</div>\n");
	}
	
	
	/**
	 * Get the name identifier of a given question. The identifier will be of the form: s1q2 for question 2 of section 1
	 * @param q	Question instance
	 * @return String representation of the question identifier
	 */
	private String getQuestionName(Question q) {
		return "\"s" + q.getSectionNumber() + "q" + q.getQuestionNumber() + "\"";
	}
}
