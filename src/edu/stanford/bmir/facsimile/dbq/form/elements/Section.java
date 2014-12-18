package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.io.Serializable;
import java.util.List;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Section implements Serializable {
	private static final long serialVersionUID = 1L;
	private String header, text;
	private List<FormElement> elements;
	private boolean numbered;
	private SectionType type;
	
	
	/**
	 * Constructor
	 * @param header	Section header/title
	 * @param text	Section text
	 * @param elements	List of elements (IRIs)
	 * @param numbered	true if section is numbered, false otherwise
	 * @param type	Section type
	 */
	public Section(String header, String text, List<FormElement> elements, boolean numbered, SectionType type) {
		this.header = header;
		this.text = text;
		this.elements = elements;
		this.numbered = numbered;
		this.type = type;
	}
	
	
	/**
	 * Get the header of the section
	 * @return Section header
	 */
	public String getSectionHeader() {
		return header;
	}
	
	
	/**
	 * Get the text of the section
	 * @return Section text
	 */
	public String getSectionText() {
		return text;
	}
	
	
	/**
	 * Get the list of elements this section contains
	 * @return List of elements' IRIs contained in this section
	 */
	public List<FormElement> getSectionElements() {
		return elements;
	}
	
	
	/**
	 * Check if this section contains a specified element
	 * @param i	Element instance
	 * @return true if this section contains the given element, false otherwise
	 */
	public boolean containsElement(FormElement i) {
		if(elements.contains(i))
			return true;
		else 
			return false;
	}
	
	
	/**
	 * Check if this section is numbered
	 * @return true if section is numbered, false otherwise
	 */
	public boolean isSectionNumbered() {
		return numbered;
	}
	
	
	/**
	 * Get the type of section
	 * @return SectionType
	 */
	public SectionType getType() {
		return type;
	}
	
	
	/**
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * Section types
	 */
	public enum SectionType {
		QUESTION_SECTION, PATIENT_SECTION, PHYSICIAN_SECTION;
		
		public String toString() {
			return name().charAt(0) + name().substring(1).toLowerCase();
		}
	}
}
