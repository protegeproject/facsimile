package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.w3c.dom.Node;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement.ElementType;
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
	private Set<IRI> processed;
	private Map<IRI,IRI> aliases;
	
	
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
		processed = new HashSet<IRI>();
		aliases = new HashMap<IRI,IRI>();
	}
	
	
	/**
	 * Generate the HTML form
	 * @param title	Title of the HTML webpage
	 * @param cssClass	CSS style class to be used 
	 * @return Document representing an HTML form
	 */
	public String generateHTMLForm(String title, String cssClass) {
		System.out.print("Generating HTML form... ");
		String output = generateHTMLTopPart(title, cssClass);
		int skip = 0;
		for(int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			boolean numbered = s.isSectionNumbered();
			if(numbered)
				output += "<div class=\"section\"><span>" + (i+1-skip) + "</span>" + s.getSectionHeader() + "</div>";
			else {
				output += "<div class=\"section\">" + s.getSectionHeader() + "</div>"; skip++;
			}
			output += getSectionText(s);
			
			List<FormElement> elements = s.getSectionElements();
			List<IRI> invisibleElements = new ArrayList<IRI>();
			int startRepIndex = 0, repeat = 0;
			for(int j = 0; j < elements.size(); j++) {
				String onchange = "";
				FormElement element = elements.get(j);
				IRI eleIri = element.getEntityIRI(), trigger = null;
				List<IRI> superquestions = element.getSuperquestions();
				// JavaScript onChange event handling
				for(int k = 0; k < superquestions.size(); k++) 
					if(posTriggers.containsKey(superquestions.get(k)))
						invisibleElements.add(eleIri);
				if(posTriggers.containsKey(eleIri)) {
					trigger = posTriggers.get(eleIri);
					invisibleElements.addAll(element.getSubquestions());
					List<FormElement> descendants = element.getDescendants(elements);
					onchange = getOnChangeEvent(posTriggers, element, true, descendants);
				}
				else if(negTriggers.containsKey(eleIri)) {
					trigger = negTriggers.get(eleIri);
					onchange = getOnChangeEvent(negTriggers, element, false, null);
				}
				
				if(isFirstElementInQuestionList(eleIri)) { // check if this is the first question of an inline-questionList element
					int indent = 0; startRepIndex = j; 
					if(repeat == 0) repeat = getNumberRepetitionsForQuestionList(eleIri);
					if(element instanceof Question) indent = ((Question)element).getLevel()*50;
					output += "<div class=\"table-container\"" + (indent > 0 ? " style=\"margin-left:" + indent + "px;\"" : "") + ">\n";
					output += "<div class=\"table\">\n<div class=\"table-row\">\n";
				}
				
				output += writeElement(element, onchange, trigger, numbered, (invisibleElements.contains(eleIri) ? true : false));
				
				if(isLastElementInQuestionList(eleIri)) { // check if this is the last question of an inline-questionList element
					output += "</div>\n</div>\n</div>\n";
					if(repeat > 1) {
						j = startRepIndex-1;
						repeat--;
					}
				}
				processed.add(eleIri);
			}
			if(i < sections.size()-1) output += "<br><hr><br>\n";
		}
		output += "<br><br>\n<div class=\"button-section\">\n<input type=\"submit\" value=\"Submit\"/>\n</div>\n";
		output += "</form>\n</div>\n</body>\n</html>\n";
		System.out.println("done");
		return output;
	}
	
	
	/**
	 * Get the number of repetitions specified for a question list
	 * @param iri	IRI of first element in inline-questionlist
	 * @return Number of repetitions
	 */
	private Integer getNumberRepetitionsForQuestionList(IRI iri) {
		int output = 0;
		Map<Node, List<IRI>> typesMap = config.getQuestionListTypesMap();
		loopNodes:
			for(Node n : typesMap.keySet()) {
				List<IRI> list = typesMap.get(n);
				if(list.get(0).equals(iri)) {
					output = config.getQuestionListRepeatMap().get(n);
					break loopNodes;
				}
			}
		return output;
	}
	
	
	/**
	 * Check if given IRI is the first question IRI in an inline-questionList element
	 * @param iri	Question IRI
	 * @return true if question is the first of an inline-questionList element, false otherwise
	 */
	private boolean isFirstElementInQuestionList(IRI iri) {
		boolean firstEle = false;
		Map<Node, List<IRI>> typesMap = config.getQuestionListTypesMap();
		loopNodes:
			for(Node n : typesMap.keySet()) {
				List<IRI> list = typesMap.get(n);
				if(list.get(0).equals(iri)) {
					firstEle = true;
					break loopNodes;
				}
			}
		return firstEle;
	}
	
	
	/**
	 * Check if given IRI is the last question IRI in an inline-questionList element
	 * @param iri	Question IRI
	 * @return true if question is the last of an inline-questionList element, false otherwise 
	 */
	private boolean isLastElementInQuestionList(IRI iri) {
		boolean lastEle = false;
		Map<Node, List<IRI>> typesMap = config.getQuestionListTypesMap();
		loopNodes:
			for(Node n : typesMap.keySet()) {
				List<IRI> list = typesMap.get(n);
				if(list.get(list.size()-1).equals(iri)) {
					lastEle = true;
					break loopNodes;
				}
			}
		return lastEle;
	}
	
	
	/**
	 * Get section text
	 * @param s	Section object
	 * @return String containing section text to be displayed
	 */
	private String getSectionText(Section s) {
		String output = "";
		String sectText = s.getSectionText();
		sectText = sectText.replaceAll("\n", "<br>");
		if(!sectText.equalsIgnoreCase(""))
			output += "<p>" + sectText + "</p><br>\n";
		else
			output += "<br>\n";
		return output;
	}
	
	
	/**
	 * Generate the top portion of the output HTML webpage, up until the beginning of the form element itself
	 * @param title	Webpage title
	 * @param cssClass	CSS class
	 * @return String containing top part of the output HTML page, down to beginning of form
	 */
	private String generateHTMLTopPart(String title, String cssClass) {
		String output = "<!DOCTYPE html>\n<html>\n<head>\n<title>" + title + "</title>\n<meta charset=\"utf-8\"/>\n";
		output += "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\"/>\n";
		output += "<link rel=\"icon\" type=\"image/png\" href=\"style/favicon.ico\"/>\n";
		output += "<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\"/>\n";
		output += "<script type=\"text/javascript\" src=\"js/script.js\"></script>\n";
		output += "</head>\n<body>\n<div class=\"" + cssClass + "\">\n";
		output += "<h1>" + config.getOutputFileTitle() + "<span>Please answer all questions and submit your answers at the end</span></h1><br>\n";
		output += "<form action=\"submit\" method=\"post\" id=\"form\">\n";
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
		IRI eleIri = e.getEntityIRI();
		if(processed.contains(eleIri)) eleIri = getAlternateIRI(e);
		String qNumber = "", qName = eleIri.toString(), qNameShort = eleIri.getShortForm();
		StringBuilder output = new StringBuilder();
		if(sectionNumbered && e.isElementNumbered()) 
			qNumber = e.getElementNumber() + ") ";
		String qText = e.getText(); qText = qText.replaceAll("\n", "<br>");
		String labelInit = "<p>" + qNumber.toUpperCase() + qText + (e.isRequired() ? " <sup>*</sup>" : "") + (!qNumber.equals("") || !qText.equals("") ? "</p>\n" : "");
		
		if(!qText.isEmpty() || (qText.isEmpty() && (e instanceof Question && ((Question)e).isSubquestion()))) {
			String cssClass = "inner-wrap";
			if(e.getType().equals(ElementType.NONE)) cssClass = "question-holder";
			
			if(e instanceof Question && ((Question)e).getLevel()>0) {
				int indent = ((Question)e).getLevel()*50;
				output.append("<div class=\"" + cssClass + "\" style=\"margin-left:" + indent + "px;" + ((qNumber.equals("") && qText.equals("")) ? "padding-bottom:10px;" : "") 
						+ (hidden? "display:none;" : "") + "\" id=\"" + qNameShort + "\"" + (!onchange.isEmpty() ? onchange : "") + ">\n");
			}
			else
				output.append("<div class=\"" + cssClass + "\" id=\"" + qNameShort + "\"" + (hidden ? " style=\"display:none;\"" : "") + (!onchange.isEmpty() ? onchange : "") + ">\n");
			
			if(!qNumber.equals("") || !qText.equals("")) output.append(labelInit);
			
			appendElementHTMLCode(output, e, qName, qNameShort, trigger);
			output.append("</div>\n");
		}
		else
			output.append("<div class=\"question-holder\">\n" + labelInit + "</div>\n");
		return output.toString();
	}
	
	
	/**
	 * Get an alternate IRI for a form element which has already been displayed
	 * @param e	Form element
	 * @return Element IRI
	 */
	private IRI getAlternateIRI(FormElement e) {
		String draft = e.getEntityIRI().toString() + "-rep-1";
		if(aliases.containsKey(IRI.create(draft))) {
			String suffix = draft.substring(draft.lastIndexOf("-rep-"), draft.length());
			int nr = Integer.parseInt(suffix.substring(suffix.lastIndexOf("-")+1, suffix.length())) + 1;
			draft = draft.replace(suffix, "-rep-" + nr);
		}
		aliases.put(IRI.create(draft), e.getEntityIRI());
		return IRI.create(draft);
	}
	
	
	/**
	 * Get the HTML code for the given element
	 * @param output	Output string builder
	 * @param e	Element instance
	 * @param qName	Question name (IRI string)
	 * @param qNameShort	Question name shortened to IRI fragment without namespace
	 * @param trigger	IRI of the question trigger
	 */
	private void appendElementHTMLCode(StringBuilder output, FormElement e, String qName, String qNameShort, IRI trigger) {
		switch(e.getType()) {
		case CHECKBOX:
			getCheckboxHTMLCode(output, e, qName, qNameShort, trigger, true); 
			break;
		case CHECKBOXHORIZONTAL:
			getCheckboxHTMLCode(output, e, qName, qNameShort, trigger, false); 
			break;
		case DROPDOWN:
			getDropdownHTMLCode(output, e, qName, qNameShort, trigger);
			break;
		case RADIO:
			getRadioHTMLCode(output, e, qName, qNameShort, trigger);
			break;
		case TEXTAREA:
			output.append("<textarea name=\"" + qName + "\"" + (e.isRequired() ? " required" : "") + "></textarea>\n"); 
			break;
		case TEXT:
			output.append("<input type=\"text\" name=\"" + qName + "\"" + (e.isRequired() ? " required" : "") + "/>\n"); 
			break;
		case NONE: break;
		default: break;
		}
	}
	
	
	/**
	 * Get the HTML code for the given radio element
	 * @param output	Output string builder
	 * @param e	Element instance
	 * @param qName	Question name (IRI string)
	 * @param qNameShort	Question name shortened to IRI fragment without namespace
	 * @param trigger	IRI of the question trigger
	 */
	private void getRadioHTMLCode(StringBuilder output, FormElement e, String qName, String qNameShort, IRI trigger) {
		if(e instanceof Question) {
			QuestionOptions opts = ((Question)e).getQuestionOptions();
			List<String> optionList = opts.getOptionsValues();
			if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
				optionList = sortList(optionList, optionsOrder.get(((Question)e).getEntityIRI()), qName);
			for(int i = 0; i < optionList.size(); i++) {
				String opt = optionList.get(i);
				String qId = qNameShort + "-" + i;
				if(trigger != null && opt.equalsIgnoreCase(opts.getOptionsMap().get(trigger.toString())))
					output.replace(output.indexOf(triggerString), output.indexOf(triggerString)+triggerString.length(), qId);
				output.append("<div class=\"option\"><label><input type=\"" + e.getType().toString().toLowerCase() + "\" name=\"" + qName + "\" id=\"" + qId
						+ "\" value=\"" + opt + "\"" + (e.isRequired() ? " required" : "") + "/>" + opt + "</label></div>\n");
			}
		}
	}
	
	
	/**
	 * Get the HTML code for the given dropdown element
	 * @param output	Output string builder
	 * @param e	Element instance
	 * @param qName	Question name (IRI string)
	 * @param qNameShort	Question name shortened to IRI fragment without namespace
	 * @param trigger	IRI of the question trigger
	 */
	private void getDropdownHTMLCode(StringBuilder output, FormElement e, String qName, String qNameShort, IRI trigger) {
		output.append("<select name=\"" + qName + "\">\n");
		if(e instanceof Question) {
			List<String> list = ((Question)e).getQuestionOptions().getOptionsValues();
			if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
				list = sortList(list, optionsOrder.get(((Question)e).getEntityIRI()), qName);
			output.append("<option value=\"\" selected>&nbsp;</option>\n");
			for(int i = 0; i < list.size(); i++) {
				String opt = list.get(i);
				output.append("<option value=\"" + opt + "\">" + opt + "</option>\n");
			}
		}
		output.append("</select>\n");
	}
	
	
	/**
	 * Get the HTML code for the given checkbox element
	 * @param output	Output string builder
	 * @param e	Element instance
	 * @param qName	Question name (IRI string)
	 * @param qNameShort	Question name shortened to IRI fragment without namespace
	 * @param trigger	IRI of the question trigger
	 * @param vertical	true if checkbox should be displayed vertically, false otherwise
	 */
	private void getCheckboxHTMLCode(StringBuilder output, FormElement e, String qName, String qNameShort, IRI trigger, boolean vertical) {
		if(e instanceof Question) {
			QuestionOptions opts = ((Question)e).getQuestionOptions();
			List<String> optionList = opts.getOptionsValues();
			if(optionsOrder.containsKey(((Question)e).getEntityIRI()))
				optionList = sortList(optionList, optionsOrder.get(((Question)e).getEntityIRI()), qName);
			for(int i = 0; i < optionList.size(); i++) {
				String opt = optionList.get(i);
				String qId = qNameShort + "-" + i;
				if(trigger != null && opt.equalsIgnoreCase(opts.getOptionsMap().get(trigger.toString())))
					output.replace(output.indexOf(triggerString), output.indexOf(triggerString)+triggerString.length(), qId);
				output.append("<div class=\"option\"><label><input type=\"checkbox\" name=\"" + qName + "\" id=\"" + qId + "\" value=\"" + opt.toLowerCase() + "\"" 
					+ (e.isRequired() ? " required" : "") + "/>" + opt + "</label></div>" + (vertical && i<(optionList.size()-1) ? "<br>\n" : "\n"));
			}
		}
	}
	
	
	/**
	 * Sort a given list according to the order of elements given by the second list parameter
	 * @param list	List to be ordered
	 * @param orderList	List that guides the order of elements of the first list
	 * @param qName	String for question IRI
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
					if(!onchange.endsWith(",")) onchange += ",";
					onchange += "'" + iri.getShortForm() + "'";
				}
				if(i<children.size()-1) onchange += ",";
			}
			onchange += ");\"";
		}
		return onchange;
	}
	
	
	/**
	 * Get the map of IRI aliases
	 * @return Map of IRI aliases
	 */
	public Map<IRI,IRI> getIRIAliases() {
		return aliases;
	}
}
