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
			bw.write("<div class=\"section\"><span>" + (i+1) + "</span>" + s.getSectionHeader() + "</div>");
			String sectText = s.getSectionText();
			if(!sectText.equalsIgnoreCase(""))
				bw.write("<p>" + sectText + "</p>");
			bw.write("<br>\n");
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
		String qNumber = q.getQuestionNumber();
		String qText = q.getQuestionText();
		String labelInit = "<p>" + qNumber.toUpperCase() + ") " + qText + "\n";
		if(!qText.isEmpty() || (qText.isEmpty() && q.isSubquestion())) {
			if(q.isSubquestion())
				bw.write("<div class=\"inner-wrap-alt\">\n");
			else
				bw.write("<div class=\"inner-wrap\">\n");
			switch(q.getQuestionType()) {
			case CHECKBOX:
				bw.write(labelInit + "<br><br>\n");
				for(String opt : q.getQuestionOptions())
					bw.write("<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
							+ " value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</label>\n");
				break;
			case DROPDOWN:
				bw.write(labelInit + "<br><br>\n" + "<select name=" + qName + ">\n");
				for(String opt : q.getQuestionOptions())
					bw.write("<option value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</option>\n");
				bw.write("</select>\n");
				break;
			case RADIO:
				bw.write(labelInit + "<br><br>\n");
				for(String opt : q.getQuestionOptions())
					bw.write("<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
							+ " value=\"" + opt + "\">" + opt + "</label>\n");
				break;
			case TEXTAREA:
				bw.write(labelInit + "<br><br>\n" + "<textarea name=" + qName + "></textarea>\n");
				break;
			case TEXT:
				bw.write(labelInit + "<br><br>\n" + "<input type=\"text\" name=" + qName + "/>\n");
				break;
			case NONE:
				bw.write(labelInit);
				break;
			case COMBO:
				bw.write(labelInit + "<br><br>\n");
				break;
			default:
				break;
			}
			bw.write("</p>\n</div>\n");
		}
		else
			bw.write("<div class=\"question-holder\">" + labelInit + "</p>\n</div>");
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
