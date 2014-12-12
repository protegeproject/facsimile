package edu.stanford.bmir.facsimile.dbq.form.elements;

import java.io.Serializable;

import org.semanticweb.owlapi.model.OWLEntity;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class InformationElement extends FormElement implements Serializable {
	private static final long serialVersionUID = -1511148682869641277L;

	/**
	 * Constructor
	 * @param entity	OWL entity
	 * @param eleNr	Element number
	 * @param sectionNr	Section number
	 * @param text	Element text
	 * @param focus	Element focus
	 * @param type	Type of element
	 */
	public InformationElement(OWLEntity entity, String eleNr, int sectionNr, String text, String focus, ElementType type) {
		super(entity, eleNr, sectionNr, text, focus, type);
	}
}
