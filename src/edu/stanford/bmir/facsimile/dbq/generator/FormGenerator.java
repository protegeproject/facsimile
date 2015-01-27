package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
import edu.stanford.bmir.facsimile.dbq.form.elements.QuestionOptions;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormGenerator {
	private List<Section> sections;
	private Map<IRI,IRI> posTriggers, negTriggers;
	private Map<IRI,List<String>> optionsOrder;
	private final String triggerString;
	private Configuration config;
	
	
	/**
	 * Constructor
	 * @param sections	List of sections to populate the form
	 * @param config	Configuration
	 */
	public FormGenerator(List<Section> sections, Configuration config) {
		this.sections = sections;
		this.config = config;
		triggerString = "xtriggerx";
		posTriggers = config.getSubquestionPositiveTriggers();
		negTriggers = config.getSubquestionNegativeTriggers();
		optionsOrder = config.getOptionsOrderMap();
	}
	
	
	/**
	 * Generate the HTML form
	 * @param title	Title of the HTML webpage
	 * @param cssClass	CSS style class to be used 
	 * @return Document representing an HTML form
	 */
	public String generateHTMLForm(String title, String cssClass) {
		System.out.print("Generating HTML form... ");
		String output = "";
		output += "<!DOCTYPE html>\n<html>\n<head>\n<title>" + title + "</title>\n<meta charset=\"utf-8\"/>\n";
		output += "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\"/>\n";
		output += "<link rel=\"icon\" type=\"image/png\" href=\"style/favicon.ico\"/>\n";
		output += "<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\"/>\n";
		output += "<script type=\"text/javascript\" src=\"js/script.js\"></script>\n";
		output += "</head>\n<body>\n<div class=\"" + cssClass + "\">\n";
		output += "<h1>" + config.getOutputFileTitle() + "<span>Please answer all questions and submit your answers at the end</span></h1><br>\n";
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
			List<IRI> invisibleElements = new ArrayList<IRI>();
			for(int j = 0; j < elements.size(); j++) {
				FormElement element = elements.get(j);
				List<IRI> superquestions = element.getSuperquestions();
				for(int k = 0; k < superquestions.size(); k++) 
					if(posTriggers.containsKey(superquestions.get(k)))
						invisibleElements.add(element.getEntityIRI());

				String onchange = "";
				IRI trigger = null;
				if(posTriggers.containsKey(element.getEntityIRI())) {
					trigger = posTriggers.get(element.getEntityIRI());
					invisibleElements.addAll(element.getSubquestions());
					List<FormElement> descendants = element.getDescendants(elements);
					onchange = getOnChangeEvent(posTriggers, element, true, descendants);
				}
				else if(negTriggers.containsKey(element.getEntityIRI())) {
					trigger = negTriggers.get(element.getEntityIRI());
					onchange = getOnChangeEvent(negTriggers, element, false, null);
				}
				output += writeElement(element, onchange, trigger, numbered, (invisibleElements.contains(element.getEntityIRI()) ? true : false));
			}
			if(i<sections.size()-1) output += "<br><hr><br>\n";
		}
		output += "<br><br>\n<div class=\"button-section\">\n<input type=\"submit\" value=\"Submit\"/>\n</div>\n";
		output += "</form>\n</div>\n</body>\n</html>\n";
		System.out.println("done");
		return output;
	}
	
	
	/**
	 * Get the details of the element
	 * @param e	Element instance
	 * @param onchange	JavaScript onChange event
	 * @param trigger	IRI of the subquestion show/hide trigger
	 * @param sectionNumbered	true if all elements should be numbered, false otherwise
	 * @param hidden	true if question should be hidden by default, false otherwise
	 * @return String with the HTML code for the given element
	 */
	private String writeElement(FormElement e, String onchange, IRI trigger, boolean sectionNumbered, boolean hidden) {
		String output = "";
		String qName = e.getEntity().getIRI().toString();
		String qNameShort = e.getEntity().getIRI().getShortForm();
		String qNumber = "";
		if(sectionNumbered && e.isElementNumbered()) qNumber = e.getElementNumber() + ") ";
		String qText = e.getText();
		qText = qText.replaceAll("\n", "<br>");
		String labelInit = "<p>" + qNumber.toUpperCase() + qText + (e.isRequired() ? " <sup>*</sup>" : "") + (!qNumber.equals("") || !qText.equals("") ? "</p>\n" : "");
		
		if(!qText.isEmpty() || (qText.isEmpty() && (e instanceof Question && ((Question)e).isSubquestion()))) {
			if(e instanceof Question && ((Question)e).getLevel()>0) {
				int indent = ((Question)e).getLevel()*50;
				output += "<div class=\"inner-wrap\" style=\"margin-left:" + indent + "px;" + ((qNumber.equals("") && qText.equals("")) ? "padding-bottom:10px;" : "") 
						+ (hidden? "display:none;" : "") + "\" id=\"" + e.getEntity().getIRI().getShortForm() + "\"" + (!onchange.isEmpty() ? onchange : "") + ">\n";
			}
			else
				output += "<div class=\"inner-wrap\" id=\"" + e.getEntity().getIRI().getShortForm() + "\"" + (hidden ? " style=\"display:none;\"" : "") + (!onchange.isEmpty() ? onchange : "") + ">\n";
			
			if(!qNumber.equals("") || !qText.equals(""))
				output += labelInit;
			
			switch(e.getType()) {
			case CHECKBOX:
				if(e instanceof Question) {
					QuestionOptions opts = ((Question)e).getQuestionOptions();
					List<String> optionList = opts.getOptionsValues();
					if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
						optionList = sortList(optionList, optionsOrder.get(((Question)e).getEntityIRI()), qName);
					for(int i = 0; i < optionList.size(); i++) {
						String opt = optionList.get(i);
						String qId = qNameShort + "-" + i;
						if(trigger != null && opt.equalsIgnoreCase(opts.getOptionsMap().get(trigger.toString())))
							output = output.replace(triggerString, qId);
						output += "<div class=\"option\"><label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=\"" + qName + "\" id=\"" + qId + "\" value=\"" + opt.toLowerCase() + "\"" 
							+ (e.isRequired() ? " required" : "") + "/>" + opt + "</label></div>" + (i<(optionList.size()-1) ? "<br>\n" : "\n");
					}
				}
				break;
			case CHECKBOXHORIZONTAL:
				if(e instanceof Question) {
					QuestionOptions opts = ((Question)e).getQuestionOptions();
					List<String> optionList = opts.getOptionsValues();
					if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
						optionList = sortList(optionList, optionsOrder.get(((Question)e).getEntityIRI()), qName);
					for(int i = 0; i < optionList.size(); i++) {
						String opt = optionList.get(i);
						String qId = qNameShort + "-" + i;
						if(trigger != null && opt.equalsIgnoreCase(opts.getOptionsMap().get(trigger.toString())))
							output = output.replace(triggerString, qId);
						output += "<div class=\"option\"><label><input type=\"checkbox\" name=\"" + qName + "\" id=\"" + qId + "\" value=\"" + opt.toLowerCase() + "\"" 
							+ (e.isRequired() ? " required" : "") + "/>" + opt + "</label></div>\n";
					}
				}
				break;
			case DROPDOWN:
				output += "<select name=\"" + qName + "\">\n";
				if(e instanceof Question) {
					List<String> list = ((Question)e).getQuestionOptions().getOptionsValues();
					if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
						list = sortList(list, optionsOrder.get(((Question)e).getEntityIRI()), qName);
					output += "<option value=\"\" selected>&nbsp;</option>\n";
					for(int i = 0; i < list.size(); i++) {
						String opt = list.get(i);
						output += "<option value=\"" + opt + "\">" + opt + "</option>\n";
					}
				}
				output += "</select>\n";
				break;
			case RADIO:
				if(e instanceof Question) {
					QuestionOptions opts = ((Question)e).getQuestionOptions();
					List<String> optionList = opts.getOptionsValues();
					if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
						optionList = sortList(optionList, optionsOrder.get(((Question)e).getEntityIRI()), qName);
					for(int i = 0; i < optionList.size(); i++) {
						String opt = optionList.get(i);
						String qId = qNameShort + "-" + i;
						if(trigger != null && opt.equalsIgnoreCase(opts.getOptionsMap().get(trigger.toString())))
							output = output.replace(triggerString, qId);
						output += "<label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=\"" + qName + "\" id=\"" + qId
								+ "\" value=\"" + opt + "\"" + (e.isRequired() ? " required" : "") + "/>" + opt + "</label>\n";
					}
				}
				break;
			case TEXTAREA:
				output += "<textarea name=\"" + qName + "\"" + (e.isRequired() ? " required" : "") + "></textarea>\n"; break;
			case TEXT:
				output += "<input type=\"text\" name=\"" + qName + "\"" + (e.isRequired() ? " required" : "") + "/>\n"; break;
			case NONE:
				break;
			default:
				break;
			}
			output += "</div>\n";
		}
		else
			output += "<div class=\"question-holder\">" + labelInit + "</p></div>\n";
		return output;
	}
	
	
	/**
	 * Sort a given list according to the order of elements given by the second list parameter
	 * @param list	List to be ordered
	 * @param orderList	List that guides the order of elements of the first list
	 * @return List sorted according to the order given by another list
	 */
	private List<String> sortList(List<String> list, List<String> orderList, String qName) {
		List<String> output = new ArrayList<String>();
		LinkedList<Integer> freeIndexes = new LinkedList<Integer>();
		boolean wildcharUsed = false;
		for(int i = 0; i < orderList.size(); i++) {
			if(orderList.get(i).equals("*")) {
				System.err.println("\n! Warning: Not all options' order specified for question " + qName + " ('*' wildcard character used)");
				wildcharUsed = true;
				int diff = list.size()-orderList.size()+1;
				for(int j = 0; j < diff; j++) {
					output.add("");
					freeIndexes.add(output.size()-1);
				}
			}
			else {
				Integer nr = Integer.parseInt(orderList.get(i));
				if(nr <= list.size() && list.get(nr-1) != null)
					output.add(list.get(nr-1));
			}
			
		}
		/* If not all members of the 1st list are specified in the 2nd list,
		 * just add them to the end of the (partially-ordered) list  
		 */
		if(output.size() < list.size() || wildcharUsed) {
			for(int i = 0; i < list.size(); i++) {
				String str = list.get(i);
				if(!output.contains(str)) {
					if(wildcharUsed && !freeIndexes.isEmpty()) {
						int index = freeIndexes.poll();
						output.set(index, str);
					}
					else
						output.add(str);
				}
			}
		}
		return output;
	}
	
	
	/**
	 * Get the JavaScript onChange event for the given form element
	 * @param map	Map of questions to trigger IRIs
	 * @param e	Form element
	 * @param pos	true if given triggers are positive triggers (i.e., showSubquestionsForAnswer="...")
	 * @param descendants	List of descendant form elements
	 * @return String containing the onChange event 
	 */
	private String getOnChangeEvent(Map<IRI,IRI> map, FormElement e, boolean pos, List<FormElement> descendants) {
		String onchange = "";
		List<IRI> children = e.getSubquestions();
		if(map.containsKey(e.getEntityIRI())) {
			if(pos)
				onchange += " onchange=\"showSubquestions('" + triggerString + "',";
			else
				onchange += " onchange=\"hideSubquestions('" + triggerString + "',";
			
			for(int i = 0; i < children.size(); i++) {
				IRI c = children.get(i);
				onchange += "'" + c.getShortForm() + "'";
				List<IRI> extraChildren = new ArrayList<IRI>();
				if(pos && descendants != null)
					if(negTriggers.containsKey(c))
						for(FormElement ele : descendants)
							if(ele.getEntityIRI().equals(c)) 
								extraChildren.addAll(ele.getSubquestions());
							
				for(IRI iri : extraChildren) {
					if(!onchange.endsWith(",")) 
						onchange += ",";
					onchange += "'" + iri.getShortForm() + "'";
				}
				if(i<children.size()-1)
					onchange += ",";
			}
			onchange += ");\"";
		}
		return onchange;
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
