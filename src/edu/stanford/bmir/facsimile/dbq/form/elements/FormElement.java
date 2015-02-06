package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
	private List<IRI> children, parents;
	private boolean required;
	private FormElementList formElementList;
	private Set<FormElementList> formElementLists;
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
	public boolean isNumbered() {
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
	
	
	public boolean hasChildren() {
		if(children.isEmpty())
			return false;
		else
			return true;
	}
	
	
	public boolean hasParents() {
		if(parents.isEmpty())
			return false;
		else
			return true;
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
	public IRI getIRI() {
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
	 * Get the list of sub-elements' IRIs
	 * @return List of IRIs
	 */
	public List<IRI> getChildElements() {
		if(children == null) children = new ArrayList<IRI>();
		return children;
	}
	
	
	/**
	 * Get the list of super-elements' IRIs
	 * @return List of IRIs
	 */
	public List<IRI> getParentElements() {
		if(parents == null) parents = new ArrayList<IRI>();
		return parents;
	}
	
	
	/**
	 * Get direct parent element IRI
	 * @return IRI
	 */
	public IRI getParentElement() {
		return parent;
	}
	
	
	/**
	 * Get the surrounding question or info list
	 * @return Form element list instance
	 */
	public FormElementList getFormElementList() {
		return formElementList;
	}
	
	
	/**
	 * Get the list of all sub-elements of this question within a given list
	 * @param elements	List of elements within which the search for descendants will occur
	 * @return List of form elements which are sub-elements of this question
	 */
	public List<FormElement> getDescendants(List<FormElement> elements) {
		List<FormElement> output = new ArrayList<FormElement>();
		for(FormElement e : elements)
			if(e.getParentElements().contains(getIRI()))
				output.add(e);
		return output;
	}
	
	
	/**
	 * Add child element to children list
	 * @param iri	Element IRI
	 */
	public void addSubElement(IRI iri) {
		if(children == null)
			children = new ArrayList<IRI>();
		children.add(iri);
	}
	
	
	/**
	 * Add parent element IRI to parent elements list
	 * @param iri	Element IRI
	 */
	public void addSuperElement(IRI iri) {
		if(parents == null)
			parents = new ArrayList<IRI>();
		parents.add(iri);
	}
	
	
	/**
	 * Set the direct parent element IRI
	 * @param iri	IRI
	 */
	public void setParentElement(IRI iri) {
		parent = iri;
	}
	
	
	/**
	 * Set the directly surrounding form element list of this question
	 * @param formElementList	Form element list instance
	 */
	public void setFormElementList(FormElementList formElementList) {
		this.formElementList = formElementList; 
	}
	
	
	/**
	 * Set all surrounding form element lists
	 * @param formElementLists	Set of form element lists
	 */
	public void setFormElementLists(Set<FormElementList> formElementLists) {
		if(this.formElementLists == null) this.formElementLists = formElementLists;
		else this.formElementLists.addAll(formElementLists);
	}
	
	
	/**
	 * Get the set of all form element lists this form element appears in
	 * @return Set of form element lists this element appears in
	 */
	public Set<FormElementList> getFormElementLists() {
		return formElementLists;
	}
	
	
	/**
	 * Check if this form element is involved in a repeating form element list
	 * @return true if this element is involved in a repeating form element list, false otherwise
	 */
	public boolean isInRepeatingElementList() {
		boolean isRepeating = false;
		for(FormElementList list : formElementLists)
			if(list.isRepeating() && list.contains(getIRI())) {
				isRepeating = true;
				break;
			}
		return isRepeating;
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
