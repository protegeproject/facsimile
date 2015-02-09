package edu.stanford.bmir.facsimile.dbq.utils;

import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.rdf.model.RDFGraph;
import org.semanticweb.owlapi.rdf.model.RDFTranslator;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class OWL2RDFTranslator {

	/**
	 * Constructor
	 */
	public OWL2RDFTranslator() { }
		
	
	/**
	 * Given an ontology, return a string representing its translation into a (N-triples format) triple dump
	 * @param ont	OWL ontology
	 * @return String containing the triples
	 */
	public String translate(OWLOntology ont) {
		RDFTranslator trans = new RDFTranslator(ont.getOWLOntologyManager(), ont, true);
		RDFGraph graph = trans.getGraph();
		for(OWLAxiom ax : ont.getAxioms())
			ax.accept(trans);
		return translate(graph);
	}
	
	
	/**
	 * Given an RDF graph, append each triple to a string and finally return that
	 * @param graph	RDF graph
	 * @return String containing the triples in the given graph
	 */
	public String translate(RDFGraph graph) {
		String output = "";
		for(RDFTriple triple : graph.getAllTriples()) {
			output += triple.getSubject() + " " + triple.getPredicate() + " " + (triple.getObject().isLiteral() ? "\"" + triple.getObject() + "\"" : triple.getObject()) + ".\n";
		}
		return output;
	}
}
