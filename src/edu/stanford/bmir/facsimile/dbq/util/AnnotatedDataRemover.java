package edu.stanford.bmir.facsimile.dbq.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class AnnotatedDataRemover {
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private OWLReasoner reasoner;
	private OWLOntology ont;

	private OWLObjectProperty hasAnswer, isDerivedFrom, isComponentOf, hasComponent;
	private final String datamodel = "http://purl.org/facsimile/datamodel#",
			cfa = "http://purl.org/facsimile/cfa#";
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 */
	public AnnotatedDataRemover(OWLOntology ont) {
		this.ont = ont;
		man = ont.getOWLOntologyManager();
		df = man.getOWLDataFactory();
		reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
		
		// properties
		hasAnswer = df.getOWLObjectProperty(IRI.create(datamodel + "hasAnswer"));
		isComponentOf = df.getOWLObjectProperty(IRI.create(cfa + "isComponentOf"));
		hasComponent = df.getOWLObjectProperty(IRI.create(cfa + "hasComponent"));
		isDerivedFrom = df.getOWLObjectProperty(IRI.create(datamodel + "isDerivedFrom"));
	}
	
	
	/**
	 * Shift object property assertions from instances of AnnotatedData to instances
	 * @return
	 */
	public OWLOntology shift() {
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>(), toAdd = new HashSet<OWLAxiom>();
		Set<OWLNamedIndividual> inds = getInstancesOf(datamodel + "AnnotatedData");
		System.out.println(inds.size() + " instances of AnnotatedData");
		for(OWLNamedIndividual ind : inds) {
			System.out.println("\nChecking " + ind.getIRI().getShortForm());
			Set<OWLAxiom> usg = ont.getReferencingAxioms(ind);
			
			OWLNamedIndividual formElement = getFormElementInstance(usg); // get instance of Observation or SubjectInfo or EvaluatorInfo
			
			for(OWLAxiom axiom : usg) {
				boolean remove = false;
				if(axiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
					OWLObjectPropertyAssertionAxiom ax = (OWLObjectPropertyAssertionAxiom) axiom; 
					OWLObjectProperty prop = ax.getProperty().asOWLObjectProperty();
				
					if(((OWLObjectPropertyAssertionAxiom)axiom).getProperty().equals(hasAnswer))
						remove = true;
					
					// get instance of data this instance is a component of
					if(prop.equals(isComponentOf)) {
						OWLNamedIndividual component = ax.getObject().asOWLNamedIndividual();
						System.out.println("   isComponentOf: " + component.getIRI().getShortForm());
						toAdd.add(df.getOWLObjectPropertyAssertionAxiom(prop, formElement, component));
						remove = true;
					}
					
					// get form element this instance of data is derived from
					if(prop.equals(isDerivedFrom)) {
						OWLNamedIndividual derivedFrom = ax.getObject().asOWLNamedIndividual();
						System.out.println("   isDerivedFrom: " + derivedFrom.getIRI().getShortForm());
						toAdd.add(df.getOWLObjectPropertyAssertionAxiom(prop, formElement, derivedFrom));
						remove = true;
					}
					
					if(prop.equals(hasComponent)) {
						OWLNamedIndividual componentOf = ax.getSubject().asOWLNamedIndividual();
						if(ax.getObject().asOWLNamedIndividual().equals(ind)) {
							System.out.println("   hasComponent.this: " + componentOf.getIRI().getShortForm());
							toAdd.add(df.getOWLObjectPropertyAssertionAxiom(prop, componentOf, ind));
							remove = true;
						}
					}
				}
				else {
					remove = true;
				}
				if(remove) toRemove.add(axiom);
			}
		}
		performChanges(toAdd, true);
		performChanges(toRemove, false);
		return ont;
	}
	
	
	/**
	 * Fetch the form element instance which the instance of AnnotatedData "answers"
	 * @param usage	Usage of the data instance
	 * @return Form element OWL instance
	 */
	private OWLNamedIndividual getFormElementInstance(Set<OWLAxiom> usage)  {
		OWLNamedIndividual observation = null;
		for(OWLAxiom axiom : usage)
			if(axiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) 
				if(((OWLObjectPropertyAssertionAxiom)axiom).getProperty().equals(hasAnswer)) 
					observation = ((OWLObjectPropertyAssertionAxiom)axiom).getObject().asOWLNamedIndividual();
		return observation;
	}
	
	
	/**
	 * Carry out modifications
	 * @param changes	Set of axiom axioms
	 * @param add	true if adding the axioms, false if deleting
	 */
	private void performChanges(Set<OWLAxiom> changes, boolean add) {
		for(OWLAxiom ax : changes) {
			if(add)
				ont.getOWLOntologyManager().addAxiom(ont, ax);
			else
				ont.getOWLOntologyManager().removeAxiom(ont, ax);
		}
	}
	
	
	/**
	 * Get instances of the given entity
	 * @param name	Entity IRI as a string
	 * @return Set of instances of given entity
	 */
	public Set<OWLNamedIndividual> getInstancesOf(String name) {
		return reasoner.getInstances(df.getOWLClass(IRI.create(name)), false).getFlattened();
	}
	
	
	public OWLOntology removeImports() {
		for(OWLImportsDeclaration decl : ont.getImportsDeclarations()) {
			ont.getOWLOntologyManager().applyChange(new RemoveImport(ont, decl));
		}
		return ont;
	}
	
	
	/**
	 * main
	 * @param args	arguments
	 * @throws OWLOntologyStorageException	Ontology storage exception
	 */
	public static void main(String[] args) throws OWLOntologyStorageException {
		String outputDir = args[0];
		if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
		
		for(int i = 1; i < args.length; i++) {
			File f = new File(args[i]);
			String name = f.getName(), parentDir = f.getParentFile().getName();
			if(!parentDir.endsWith(File.separator)) parentDir += File.separator;
			
			OWLOntology ont = OntologyUtils.loadDBQOntology(args[i]);
			AnnotatedDataRemover remover = new AnnotatedDataRemover(ont);
			
			OWLOntology modified = remover.shift();
			modified.saveOntology(IRI.create("file:" + outputDir + parentDir + name));
			
			OWLOntology no_imports = remover.removeImports();
			no_imports.saveOntology(new NTriplesDocumentFormat(), IRI.create("file:" + outputDir + parentDir + name.replace(".owl", ".nt")));
		}
	}
}
