package edu.stanford.bmir.facsimile.dbq.form.elements;

import org.semanticweb.owlapi.model.OWLEntity;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormElement {
	private OWLEntity entity;
	private String eleNr, text, focus;
	private int sectionNr;
	private ElementType type;
	
	
	/**
	 * Constructor
	 * @param entity	OWL entity
	 * @param eleNr	Form element number
	 * @param sectionNr	Section number
	 * @param text	Text to be shown on this form element 
	 * @param focus	Focus of the element
	 * @param type	Element type
	 */
	public FormElement(OWLEntity entity, String eleNr, int sectionNr, String text, String focus, ElementType type) {
		this.entity = entity;
		this.eleNr = eleNr;
		this.sectionNr = sectionNr;
		this.text = text;
		this.focus = focus;
		this.type = type;
	}
	
	
	/**
	 * Get the OWL entity that represents this element 
	 * @return OWL entity
	 */
	public OWLEntity getEntity() {
		return entity;
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
	 * 
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * HTML form element types
	 */
	public enum ElementType {
		TEXT, TEXTAREA, CHECKBOX, DROPDOWN, RADIO, COMBO, NONE;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}
