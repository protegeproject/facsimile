package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.util.List;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormElementList {
	private List<IRI> formElements;
	private FormElementListType type;
	private int repeat;
	private String id;
	
	
	/**
	 * Constructor
	 * @param formElements	List of form element IRIs
	 */
	public FormElementList(List<IRI> formElements, String id) {
		this.formElements = formElements;
		this.id = id;
		type = FormElementListType.NORMAL;
		repeat = 0;
	}
	
	
	/**
	 * Get the ID of this form element list
	 * @return Identifier of the question list, which follows from the sibling configuration element
	 */
	public String getID() {
		return id;
	}
	
	
	/**
	 * Get the list of form element IRIs in this form element list
	 * @return List of IRIs
	 */
	public List<IRI> getFormElements() {
		return formElements;
	}
		
	
	/**
	 * Get the type of form element list
	 * @return Type of form element list
	 */
	public FormElementListType getType() {
		return type;
	}
	
	
	/**
	 * Get the number of repetitions of this form element list
	 * @return Number of repetitions
	 */
	public int getRepetitions() {
		return repeat;
	}
	
	
	/**
	 * Set the number of times this form element list should be displayed
	 * @param reps	Number of repetitions
	 */
	public void setRepetitions(int reps) {
		repeat = reps;
	}
	
	
	/**
	 * Set the type of form element list
	 * @param type	Type of form elementform element list
	 */
	public void setType(FormElementListType type) {
		this.type = type;
	}
	
	
	/**
	 * Check if the given element is the first element of its form element list (excluding its own sub-questions)
	 * @param element	Form element 
	 * @return true if the given form element is the first element in this form element list
	 */
	public boolean isFirstElement(FormElement element) {
		boolean isFirstElement = false;
		List<IRI> ignored = element.getChildElements();
		for(int i = 0; i < formElements.size(); i++) {
			if(!ignored.contains(formElements.get(i))) {
				if(element.getIRI().equals(formElements.get(i))) {
					isFirstElement = true;
					break;
				}
				else break;
			}
		}
		return isFirstElement;
	}
	
	
	/**
	 * Check if the given element is the last element of its form element list (excluding its own sub-questions)
	 * @param element	Form element 
	 * @return true if the given form element is the last element in this form element list
	 */
	public boolean isLastElement(FormElement element) {
		boolean isLastElement = false;
		List<IRI> ignored = element.getChildElements();
		for(int i = formElements.size()-1; i >= 0; i--) {
			if(!ignored.contains(formElements.get(i))) {
				if(element.getIRI().equals(formElements.get(i))) {
					isLastElement = true;
					break;
				}
				else break;
			}
		}
		return isLastElement;
	}
	
	
	/**
	 * Get the number of form elements in this list
	 * @return Number of form elements
	 */
	public int size() {
		return formElements.size();
	}
	
	
	/**
	 * Check if this question list is a repeating type
	 * @return true if question list is repeating (inline or not), false otherwise
	 */
	public boolean isRepeating() {
		if(this.type.equals(FormElementListType.REPEATED) || this.type.equals(FormElementListType.INLINEREPEATED))
			return true;
		else
			return false;
	}
	
	
	/**
	 * Check if this form element list is an inline type
	 * @return true if this form element list is an inline type, false otherwise
	 */
	public boolean isInline() {
		if(this.type.equals(FormElementListType.INLINE) || this.type.equals(FormElementListType.INLINEREPEATED))
			return true;
		else
			return false;
	}
	
	
	/**
	 * Check if this form element list contains a specified element based on its IRI
	 * @param iri	IRI of the element
	 * @return true if this form element list contains the specified element
	 */
	public boolean contains(IRI iri) {
		boolean contains = false;
		for(IRI element : formElements)
			if(iri.equals(element)) {
				contains = true;
				break;
			}
		return contains;
	}
	
	
	/**
	 * @author Rafael S. Goncalves <br>
	 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
	 * School of Medicine, Stanford University <br>
	 * <br>
	 * Form element list types
	 */
	public enum FormElementListType {
		NORMAL, INLINE, REPEATED, INLINEREPEATED;
		
		public String toString() {
	        return name().charAt(0) + name().substring(1).toLowerCase();
	    }
	}
}