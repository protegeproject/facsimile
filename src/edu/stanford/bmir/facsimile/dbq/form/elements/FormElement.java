package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormElement implements Serializable {
	private static final long serialVersionUID = 1L;
	private OWLEntity entity;
	private String eleNr, text, focus;
	private int sectionNr;
	private ElementType type;
	private List<IRI> subquestions, superquestions;
	private boolean required;
	private FormElementList questionList;
	private IRI parent;
	
	
	/**
	 * Constructor
	 * @param entity	OWL entity
	 * @param eleNr	Form element number
	 * @param sectionNr	Section number
	 * @param text	Text to be shown on this form element 
	 * @param focus	Focus of the element
	 * @param type	Element type
	 * @param required	Whether the form element is requires input
	 */
	public FormElement(OWLEntity entity, String eleNr, int sectionNr, String text, String focus, ElementType type, boolean required) {
		this.entity = entity;
		this.eleNr = eleNr;
		this.sectionNr = sectionNr;
		this.text = text;
		this.focus = focus;
		this.type = type;
		this.required = required;
	}
	
	
	/**
	 * Check whether an element is numbered or not
	 * @return true if element is numbered, false otherwise
	 */
	public boolean isElementNumbered() {
		if(eleNr.equals(""))
			return false;
		else
			return true;
	}
	
	
	/**
	 * Check if this form element is required
	 * @return true if element is required, false otherwise
	 */
	public boolean isRequired() {
		return required;
	}
	
	
	/**
	 * Get the OWL entity that represents this form element 
	 * @return OWL entity
	 */
	public OWLEntity getEntity() {
		return entity;
	}
	
	
	/**
	 * Get IRI of OWL entity representing this form element 
	 * @return IRI
	 */
	public IRI getEntityIRI() {
		return entity.getIRI();
	}
	
	
	/**
	 * Get the element number as established by the configuration file, or by its parse order
	 * @return String representing the element number
	 */
	public String getElementNumber() {
		return eleNr;
	}
	
	
	/**
	 * Get the section number this element appears in
	 * @return Integer representing the section number
	 */
	public int getSectionNumber() {
		return sectionNr;
	}
	
	
	/**
	 * Get the text content of the form element
	 * @return String containing the text content
	 */
	public String getText() {
		return text;
	}
	
	
	/**
	 * Get the focus (output type) of the form element
	 * @return String describing the focus of form element
	 */
	public String getFocus() {
		return focus;
	}
	
	
	/**
	 * Get the HTML form element type given by an element of the ElementType enumeration 
	 * @return ElementType representing the type of HTML form element
	 */
	public ElementType getType() {
		return type;
	}
	
	
	/**
	 * Get the list of subquestions' IRIs
	 * @return List of IRIs
	 */
	public List<IRI> getSubquestions() {
		if(subquestions == null) subquestions = new ArrayList<IRI>();
		return subquestions;
	}
	
	
	/**
	 * Get the list of superquestions' IRIs
	 * @return List of IRIs
	 */
	public List<IRI> getSuperquestions() {
		if(superquestions == null) superquestions = new ArrayList<IRI>();
		return superquestions;
	}
	
	
	/**
	 * Get parent question IRI
	 * @return IRI
	 */
	public IRI getParentQuestion() {
		return parent;
	}
	
	
	/**
	 * Get the surrounding question list
	 * @return Question list instance
	 */
	public FormElementList getQuestionList() {
		return questionList;
	}
	
	
	/**
	 * Get the list of all subquestions of this question within a given list
	 * @param elements	List of elements within which the search for descendants will occur
	 * @return List of form elements which are subquestions of this question
	 */
	public List<FormElement> getDescendants(List<FormElement> elements) {
		List<FormElement> output = new ArrayList<FormElement>();
		for(FormElement e : elements)
			if(e.getSuperquestions().contains(getEntityIRI()))
				output.add(e);
		return output;
	}
	
	
	/**
	 * Add subquestion to subquestions list
	 * @param iri	Subquestion IRI
	 */
	public void addSubquestion(IRI iri) {
		if(subquestions == null)
			subquestions = new ArrayList<IRI>();
		subquestions.add(iri);
	}
	
	
	/**
	 * Add superquestion to superquestion list
	 * @param iri	Superquestion IRI
	 */
	public void addSuperquestion(IRI iri) {
		if(superquestions == null)
			superquestions = new ArrayList<IRI>();
		superquestions.add(iri);
	}
	
	
	/**
	 * Set the parent question IRI
	 * @param iri	IRI
	 */
	public void setParentQuestion(IRI iri) {
		parent = iri;
	}
	
	
	/**
	 * Set the surrounding questionlist of this question
	 * @param questionList	Questionlist instance
	 */
	public void setQuestionList(FormElementList questionList) {
		this.questionList = questionList; 
	}
	
	
	/**
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * HTML form element types
	 */
	public enum ElementType {
		TEXT, TEXTAREA, CHECKBOX, CHECKBOXHORIZONTAL, DROPDOWN, RADIO, NONE;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
