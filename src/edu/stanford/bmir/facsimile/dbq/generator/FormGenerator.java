package edu.stanford.bmir.facsimile.dbq.generator;

import java.io.IOException;
import java.util.List;

import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormGenerator {
	private List<Section> sections;
	
	
	/**
	 * Constructor
	 * @param sections	List of sections to populate the form
	 */
	public FormGenerator(List<Section> sections) {
		this.sections = sections;
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
		int skip = 0;
		for(int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			boolean numbered = s.isSectionNumbered();
			if(numbered)
				output += "<div class=\"section\"><span>" + (i+1-skip) + "</span>" + s.getSectionHeader() + "</div>";
			else {
				output += "<div class=\"section\">" + s.getSectionHeader() + "</div>";
				skip++;
			}
			
			String sectText = s.getSectionText();
			if(!sectText.equalsIgnoreCase(""))
				output += "<p>" + sectText + "</p><br>\n";
			
			List<FormElement> elements = s.getSectionElements();
			for(int j = 0; j < elements.size(); j++)
				output += writeElement(elements.get(j), numbered);
			if(i<sections.size()-1) output += "<br><hr><br>\n";
		}
		output += "<br><br>\n<div class=\"button-section\"><input type=\"submit\" value=\"Submit\" onclick=\"this.form.submit();\"/></div>\n";
		output += "</form>\n</div>\n</body>\n</html>\n";
		System.out.println("done");
		return output;
	}
	
	
	/**
	 * Get the details of the element
	 * @param e	Element instance
	 * @param numbered	true if elements should be numbered, false otherwise
	 * @return String with the HTML code for the given element
	 * @throws IOException	IO error
	 */
	private String writeElement(FormElement e, boolean numbered) throws IOException {
		String output = "";
		String qName = "\"" + e.getEntity().getIRI().toString() + "\"";
		String qNumber = "";
		if(numbered) qNumber = e.getElementNumber() + ")";
		String qText = e.getText();
		
		String labelInit = "<p>" + qNumber.toUpperCase() + " " + qText + "\n";
		if(!qText.isEmpty() || (qText.isEmpty() && (e instanceof Question && ((Question)e).isSubquestion()))) {
			if(e instanceof Question && ((Question)e).isSubquestion())
				output += "<div class=\"inner-wrap-alt\">\n";
			else
				output += "<div class=\"inner-wrap\">\n";
			switch(e.getType()) {
			case CHECKBOX:
				output += labelInit + "<br><br>\n";
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=" + qName 
						+ " value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</label>\n";
				break;
			case DROPDOWN:
				output += labelInit + "<br><br>\n" + "<select name=" + qName + ">\n";
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<option value=\"" + opt.toLowerCase() + "\">" + opt.toLowerCase() + "</option>\n";
				output += "</select>\n";
				break;
			case RADIO:
				output += labelInit + "<br><br>\n";
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=" + qName 
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
	 * Get the name identifier of a given element. The identifier will be of the form: s1q2 for element 2 of section 1
	 * @param e	Form element instance
	 * @return String representation of the element identifier
	 */
	@SuppressWarnings("unused")
	private String getElementName(FormElement e) {
		return "\"s" + e.getSectionNumber() + "q" + e.getElementNumber() + "\"";
	}
}
