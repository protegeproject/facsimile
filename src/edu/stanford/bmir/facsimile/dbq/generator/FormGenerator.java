package edu.stanford.bmir.facsimile.dbq.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;

/**
 * @author Rafael S. Goncalves <br/>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br/>
 * School of Medicine, Stanford University <br/>
 */
public class FormGenerator {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private Configuration conf;
	private boolean verbose;
	
	
	public FormGenerator(OWLOntology ont, Configuration conf, boolean verbose) {
		this.ont = ont;
		this.conf = conf;
		this.man = ont.getOWLOntologyManager();
		this.df = man.getOWLDataFactory();
		this.verbose = verbose;
	}
	
	
	/**
	 * Extract questions (individuals) and their corresponding axioms from the ontology
	 * @param type	Specific type of question desired (if any). The given string is matched against the individual name 
	 * as it is in the given ontology
	 * @return Map of questions (represented as individuals) to the sets of axioms associated with each question 
	 */
	private Map<OWLNamedIndividual,Set<OWLAxiom>> parseQuestions(String type) {
		if(verbose) System.out.print("Parsing questions... ");
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = new HashMap<OWLNamedIndividual,Set<OWLAxiom>>();
		
		OWLClass questionClass = df.getOWLClass(conf.getQuestionClass());
		StructuralReasoner reasoner = new StructuralReasoner(ont, new SimpleConfiguration(), BufferingMode.BUFFERING);
		Set<OWLNamedIndividual> instances = reasoner.getInstances(questionClass, false).getFlattened();
		
		for(OWLNamedIndividual i : instances) {
			if(i.getIRI().getShortForm().contains(type))
				map.put(i, ont.getReferencingAxioms(i, Imports.INCLUDED));
		}
		if(verbose) System.out.println("done");
		return map;
	}
	
	
	
	public void printQuestions() {
		Map<OWLNamedIndividual,Set<OWLAxiom>> map = parseQuestions("");
		
		for(OWLNamedIndividual i : map.keySet()) {
			System.out.println("Processing question: " + i.getIRI().getShortForm());
			Set<OWLAxiom> axioms = map.get(i);
			for(OWLAxiom ax : axioms) {
				if(ax.isLogicalAxiom()) {
					System.out.println("\t" + ax);
				}
			}
		}
	}
	
}
