package edu.stanford.bmir.facsimile.dbq.form.elements;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormElement {
	private String number, text, focus;
	private int sectionNumber;
	private ElementType type;
	
	
	/**
	 * Constructor
	 * @param number	Form element number
	 * @param sectionNumber	Section number
	 * @param text	Text to be shown on this form element 
	 * @param focus	Focus of the element
	 */
	public FormElement(String number, int sectionNumber, String text, String focus, ElementType type) {
		this.number = number;
		this.sectionNumber = sectionNumber;
		this.text = text;
		this.focus = focus;
		this.type = type;
	}
	
	
	/**
	 * Get the element number as established by the configuration file, or by its parse order
	 * @return String representing the element number
	 */
	public String getElementNumber() {
		return number;
	}
	
	
	/**
	 * Get the section number this element appears in
	 * @return Integer representing the section number
	 */
	public int getSectionNumber() {
		return sectionNumber;
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
