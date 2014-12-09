package edu.stanford.bmir.facsimile.dbq.generator;

import java.io.IOException;
import java.util.List;

import edu.stanford.bmir.facsimile.dbq.question.Question;
import edu.stanford.bmir.facsimile.dbq.question.QuestionSection;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormGenerator {
	private List<QuestionSection> questionSections;
	
	
	/**
	 * Constructor
	 * @param questionSections	List of questions to populate the form
	 */
	public FormGenerator(List<QuestionSection> questionSections) {
		this.questionSections = questionSections;
	}
	
	
	/**
	 * Generate the HTML form
	 * @param title	Title of the HTML webpage
	 * @param cssClass	CSS style class to be used 
	 * @return Document representing an HTML form
	 * @throws IOException 	IO error
	 */
	public String generateHTMLForm(String title, String cssClass) throws IOException {
		System.out.print("Generating HTML form... ");
		String output = "";
		output += "<!DOCTYPE html>\n<html>\n<head>\n<title>" + title + "</title>\n<meta charset=\"utf-8\"/>\n";
		output += "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\">\n";
		output += "<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\">\n";
		output += "</head>\n<body>\n<div class=\"" + cssClass + "\">\n";
		output += "<h1>Generated Form<span>Please answer all questions and submit your answers at the end</span></h1><br>\n";
		output += "<form action=\"submit\" method=\"post\" id=\"form\">\n";
		for(int i = 0; i < questionSections.size(); i++) {
			QuestionSection s = questionSections.get(i);
			output += "<div class=\"section\"><span>" + (i+1) + "</span>" + s.getSectionHeader() + "</div>";
			String sectText = s.getSectionText();
			if(!sectText.equalsIgnoreCase(""))
				output += "<p>" + sectText + "</p>";
			output += "<br>\n";
			List<Question> questions = s.getSectionQuestions();
			for(int j = 0; j < questions.size(); j++)
				output += writeOutQuestion(questions.get(j));
			if(i<questionSections.size()-1) output += "<br><hr><br>\n";
		}
		output += "<br><br>\n<div class=\"button-section\"><input type=\"submit\" value=\"Submit\" onclick=\"this.form.submit();\"/></div>\n";
		output += "</form>\n</div>\n</body>\n</html>\n";
		System.out.println("done");
		return output;
	}
	
	
	/**
	 * Get the details of the question
	 * @param q	Question instance
	 * @return String with the HTML code for the given question
	 * @throws IOException	IO error
	 */
	private String writeOutQuestion(Question q) throws IOException {
		String output = "";
		String qName = "\"" + q.getQuestionIndividual().getIRI().toString() + "\"";
		String qNumber = q.getQuestionNumber();
		String qText = q.getQuestionText();
		
		String labelInit = "<p>" + qNumber.toUpperCase() + ") " + qText + "\n";
		if(!qText.isEmpty() || (qText.isEmpty() && q.isSubquestion())) {
			if(q.isSubquestion())
				output += "<div class=\"inner-wrap-alt\">\n";
			else
				output += "<div class=\"inner-wrap\">\n";
			switch(q.getQuestionType()) {
			case CHECKBOX:
				output += labelInit + "<br><br>\n";
				for(String opt : q.getQuestionOptions())
					output += "<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
							+ " value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</label>\n";
				break;
			case DROPDOWN:
				output += labelInit + "<br><br>\n" + "<select name=" + qName + ">\n";
				for(String opt : q.getQuestionOptions())
					output += "<option value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</option>\n";
				output += "</select>\n";
				break;
			case RADIO:
				output += labelInit + "<br><br>\n";
				for(String opt : q.getQuestionOptions())
					output += "<label><input type=\"" + q.getQuestionType().toString().toLowerCase() + "\" name=" + qName 
							+ " value=\"" + opt + "\">" + opt + "</label>\n";
				break;
			case TEXTAREA:
				output += labelInit + "<br><br>\n" + "<textarea name=" + qName + "></textarea>\n";
				break;
			case TEXT:
				output += labelInit + "<br><br>\n" + "<input type=\"text\" name=" + qName + "/>\n";
				break;
			case NONE:
				output += labelInit;
				break;
			case COMBO:
				output += labelInit + "<br><br>\n";
				break;
			default:
				break;
			}
			output += "</p>\n</div>\n";
		}
		else
			output += "<div class=\"question-holder\">" + labelInit + "</p>\n</div>";
		return output;
	}
	
	
	/**
	 * Get the name identifier of a given question. The identifier will be of the form: s1q2 for question 2 of section 1
	 * @param q	Question instance
	 * @return String representation of the question identifier
	 */
	@SuppressWarnings("unused")
	private String getQuestionName(Question q) {
		return "\"s" + q.getSectionNumber() + "q" + q.getQuestionNumber() + "\"";
	}
}
