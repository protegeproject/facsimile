package edu.stanford.bmir.facsimile.dbq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class PartonomyVerifier {
	private Map<OWLNamedIndividual,List<OWLNamedIndividual>> indPaths;
	private OWLObjectProperty isComponentOf;
	private OWLClass data, formData;
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private OWLOntology ont;
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 */
	public PartonomyVerifier(OWLOntology ont) {
		this.ont = ont;
		man = ont.getOWLOntologyManager();
		df = man.getOWLDataFactory();
	}
	
	
	/**
	 * Verify whether all instances of AnnotatedData in the given ontology have a path to the topmost data element: FormData
	 * @return true if all instances of AnnotatedData have a 'isComponentOf' path to their instance of FormData, false otherwise
	 */
	public boolean verify() {
		if (isComponentOf == null)
			throw new RuntimeException("IRI of 'hasComponent' object property has not been set.");
		if (data == null)
			throw new RuntimeException("IRI of 'AnnotatedData' class has not been set.");
		boolean allOk = true;
		indPaths = new HashMap<OWLNamedIndividual,List<OWLNamedIndividual>>();
		OWLReasoner reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
		Set<OWLNamedIndividual> formDataInstances = reasoner.getInstances(formData, false).getFlattened();
		Set<OWLNamedIndividual> instances = reasoner.getInstances(data, false).getFlattened();
		for(OWLNamedIndividual ind : instances) {
			System.out.print("Checking instance '" + ind.getIRI().getShortForm() + "'... ");
			List<OWLNamedIndividual> list = new ArrayList<OWLNamedIndividual>();
			list.add(ind);

			getIndividualPath(ind, list, formDataInstances);

			if(formDataInstances.contains(list.get(list.size()-1)))
				System.out.println("ok");
			else {
				allOk = false;
				System.out.println("bad\n   Path breaks at (" + list.size() + ")");
				for(int i = 0; i < list.size(); i++) {
					OWLNamedIndividual e = list.get(i);
					System.out.println("\t(" + (i+1) + ") " + e.getIRI().getShortForm());
				}
				indPaths.put(ind, list);
			}
		}
		return allOk;
	}
	
	
	/**
	 * Get the list of instances that the given individual is a component of, starting from most to least direct (that is, the last entry
	 * should be the FormData instance). 
	 * @param ind	OWL individual
	 * @param list	List of instances that the given individual is a component of
	 * @param formDataInstances	Set of FormData instances	
	 * @return Updated list of instances that the given individual is a component of
	 */
	private List<OWLNamedIndividual> getIndividualPath(OWLNamedIndividual ind, List<OWLNamedIndividual> list, Set<OWLNamedIndividual> formDataInstances) {
		Set<OWLAxiom> usage = ont.getReferencingAxioms(ind);
		for(OWLAxiom ax : usage) {
			if(ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION) &&
					((OWLObjectPropertyAssertionAxiom)ax).getSubject().equals(ind) &&
					((OWLObjectPropertyAssertionAxiom)ax).getProperty().equals(isComponentOf)) {
				
				OWLNamedIndividual obj = ((OWLObjectPropertyAssertionAxiom)ax).getObject().asOWLNamedIndividual();
				list.add(obj);
				if(!formDataInstances.contains(obj)) 
					return getIndividualPath(obj, list, formDataInstances);
				else return list;
			}
		}
		return null;
	}
	
	
	/**
	 * Set binding for isComponentOf object property
	 * @param iri	IRI of isComponentOf object property
	 */	
	public void setIsComponentOfIRI(String iri) {
		this.isComponentOf = df.getOWLObjectProperty(IRI.create(iri));
	}
	
	
	/**
	 * Set binding for AnnotatedData class
	 * @param iri	IRI of AnnotatedData class
	 */
	public void setAnnotatedDataIRI(String iri) {
		this.data = df.getOWLClass(IRI.create(iri));
	}
	
	
	/**
	 * Set binding for FormData class
	 * @param iri	IRI of FormData class
	 */
	public void setFormDataIRI(String iri) {
		this.formData = df.getOWLClass(IRI.create(iri));
	}
	
	
	/**
	 * main
	 * @param args	arguments
	 */
	public static void main(String[] args) {
		OWLOntology ont = OntologyUtils.loadDBQOntology(args[0]);
		
		PartonomyVerifier verifier = new PartonomyVerifier(ont);
		verifier.setAnnotatedDataIRI("http://purl.org/facsimile/datamodel#AnnotatedData");
		verifier.setFormDataIRI("http://purl.org/facsimile/datamodel#FormData");
		verifier.setIsComponentOfIRI("http://purl.org/facsimile/cfa#isComponentOf");
		
		verifier.verify();
	}
}