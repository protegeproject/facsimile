package edu.stanford.bmir.facsimile.dbq.generator;

import java.io.IOException;
import java.util.List;

import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement.ElementType;

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
		output += "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\"/>\n";
		output += "<link rel=\"icon\" type=\"image/png\" href=\"style/favicon.ico\"/>\n";
		output += "<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\"/>\n";
		output += "<script type=\"text/javascript\" src=\"js/script.js\"></script>\n";
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
			sectText = sectText.replaceAll("\n", "<br>");
			if(!sectText.equalsIgnoreCase(""))
				output += "<p>" + sectText + "</p><br>\n";
			else
				output += "<br>\n";
			
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
	 * @param sectionNumbered	true if all elements should be numbered, false otherwise
	 * @return String with the HTML code for the given element
	 * @throws IOException	IO error
	 */
	private String writeElement(FormElement e, boolean sectionNumbered) throws IOException {
		String output = "";
		String qName = "\"" + e.getEntity().getIRI().toString() + "\"";
		String qNumber = "";
		if(sectionNumbered && e.isElementNumbered()) qNumber = e.getElementNumber() + ") ";
		String qText = e.getText();
		qText = qText.replaceAll("\n", "<br>");
		String labelInit = "<p>" + qNumber.toUpperCase() + qText;
		
		if(!qText.isEmpty() || (qText.isEmpty() && (e instanceof Question && ((Question)e).isSubquestion()))) {
			if(e instanceof Question && ((Question)e).getLevel()>0) {
				int indent = ((Question)e).getLevel()*50;
				output += "<div class=\"inner-wrap\" style=\"margin-left:" + indent + "px;" + 
				((qNumber.equals("") && qText.equals("")) ? "padding-bottom:10px;" : "") + "\" id=\"" + e.getEntity().getIRI().getShortForm() + "\">\n";
			}
			else
				output += "<div class=\"inner-wrap\" id=\"" + e.getEntity().getIRI().getShortForm() + "\">\n";
			
			if(!qNumber.equals("") || !qText.equals("")) {
				output += labelInit;
				if(!e.getType().equals(ElementType.NONE))
					output += "<br><br>\n";
			}
			
			switch(e.getType()) {
			case CHECKBOX:
				if(e instanceof Question) {
					List<String> list = ((Question)e).getQuestionOptions();
					for(int i = 0; i < list.size(); i++) {
						String opt = list.get(i);
						output += "<label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=" + qName 
						+ " value=\"" + opt.toLowerCase() + "\">" + opt + "</label>" + (i<(list.size()-1) ? "<br>\n" : "\n");
					}
				}
				break;
			case CHECKBOXHORIZONTAL:
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<label><input type=\"checkbox\" name=" + qName + " value=\"" + opt.toLowerCase() + "\">" + opt + "</label>\n";
				break;
			case DROPDOWN:
				output += "<select name=" + qName + ">\n";
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<option value=\"" + opt + "\">" + opt + "</option>\n";
				output += "</select>\n";
				break;
			case RADIO:
				if(e instanceof Question)
					for(String opt : ((Question)e).getQuestionOptions())
						output += "<label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=" + qName 
						+ " value=\"" + opt + "\">" + opt + "</label>\n";
				break;
			case TEXTAREA:
				output += "<textarea name=" + qName + "></textarea>\n";
				break;
			case TEXT:
				output += "<input type=\"text\" name=" + qName + "/>\n";
				break;
			case NONE:
				break;
			default:
				break;
			}
			if(!qNumber.equals("") || !qText.equals(""))
				output += "</p>\n";
			output += "</div>\n";
		}
		else
			output += "<div class=\"question-holder\">" + labelInit + "</p></div>\n";
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
