package edu.stanford.bmir.facsimile.dbq.form.elements;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormDataElement {
	private String number, text, focus;
	private int sectionNumber;
	
	
	/**
	 * Constructor
	 * @param number	Form element number
	 * @param sectionNumber	Section number
	 * @param text	Text to be shown on this form element 
	 * @param focus	Focus of the element
	 */
	public FormDataElement(String number, int sectionNumber, String text, String focus) {
		this.number = number;
		this.sectionNumber = sectionNumber;
		this.text = text;
		this.focus = focus;
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
}
