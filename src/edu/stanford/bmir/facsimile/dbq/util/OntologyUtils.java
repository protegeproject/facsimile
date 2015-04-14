package edu.stanford.bmir.facsimile.dbq.util;
import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.util.OWLEntityURIConverter;
import org.semanticweb.owlapi.util.OWLEntityURIConverterStrategy;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class OntologyUtils {
	private SimpleShortFormProvider sf;
	private OWLOntology ont;
	private String ontIri;
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 * @param ontIri	Desired ontology IRI
	 */
	public OntologyUtils(OWLOntology ont, String ontIri) {
		this.ont = ont;
		this.ontIri = ontIri;
	}
	
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 */
	public OntologyUtils(OWLOntology ont) {
		this(ont, null);
	}
	
	
	/**
	 * Get the union of the entire imports closure of an ontology. This is done by either creating a new ontology and porting all axioms
	 * to that ontology, or taking the original ontology and merging imported axioms into that one (and removing the imports statements)
	 * @param ontType	Type of root ontology 
	 * @return Union ontology
	 */
	public OWLOntology getUnionOntology(RootOntology ontType) {
		OWLOntology toProcess = null;
		switch(ontType) {
		case EMPTY_ONT:
			try { 
				if(ontIri != null)
					toProcess = OWLManager.createOWLOntologyManager().createOntology(IRI.create(ontIri));
				else
					toProcess = OWLManager.createOWLOntologyManager().createOntology();
			} catch (OWLOntologyCreationException e) { 
				e.printStackTrace(); 
			} break;
		case INPUT_ONT: toProcess = ont; break;
		}
		
		inflateOntologyWithImports(toProcess, ontType);
		
		if(ontType.equals(RootOntology.INPUT_ONT)) 
			removeImports(toProcess);
		return toProcess;
	}
	
	
	/**
	 * Get short form of a given IRI
	 * @param inputIri	IRI
	 * @return Short form of given IRI as string
	 */
	private String getShortForm(IRI inputIri) {
		String iri = inputIri.toString();
		return iri.substring(iri.indexOf("#")+1, iri.length());
	}
	
	
	/**
	 * Get Manchester syntax rendering of a given OWL object
	 * @param obj	OWL object
	 * @return String with given object rendered in Manchester syntax 
	 */
	public String getManchesterRendering(OWLObject obj) {
		if(sf == null) sf = new SimpleShortFormProvider();
		
		StringWriter wr = new StringWriter();
		ManchesterOWLSyntaxObjectRenderer render = new ManchesterOWLSyntaxObjectRenderer(wr, sf);
		obj.accept(render);
		
		String str = wr.getBuffer().toString();
		str = str.replace("<", ""); str = str.replace(">", "");
		return str;
	}
	
	
	/**
	 * Normalize entity URIs to be all under the same namespace
	 */
	@SuppressWarnings("unused")
	private void normalizeEntityURIs() {
		System.out.print("  Normalizing entity URIs... ");
		Set<OWLOntology> ontSet = new HashSet<OWLOntology>(); ontSet.add(ont);
		OWLEntityURIConverter converter = new OWLEntityURIConverter(ont.getOWLOntologyManager(), ontSet, new OWLEntityURIConverterStrategy() {
			@Override
			public IRI getConvertedIRI(OWLEntity e) {
				String entityName = getShortForm(e.getIRI());
				IRI iri = IRI.create(ontIri + "#" + entityName);
				return iri;
			}
		});
		ont.getOWLOntologyManager().applyChanges(converter.getChanges());
		System.out.println("done");
	}
	
	
	/**
	 * Remove all imports from the given ontology
	 * @param target	OWL ontology
	 */
	public void removeImports(OWLOntology target) {
		System.out.print("  Removing imports declarations... ");
		OWLOntologyManager man = ont.getOWLOntologyManager();
		for(OWLImportsDeclaration importDecl : ont.getImportsDeclarations()) {
			man.applyChange(new RemoveImport(ont, importDecl));
		}
		System.out.println("done");
	}
	
	
	/**
	 * Add all axioms of imported ontologies to the parent ontology, and remove the imports pointers
	 * @param target	OWL ontology
	 * @param ontType	Main ontology type
	 */
	public void inflateOntologyWithImports(OWLOntology target, RootOntology ontType) {
		System.out.print("  Inflating ontology with imported axioms... ");
		OWLOntologyManager man = target.getOWLOntologyManager();
		for(OWLOntology imported : ont.getImports()) {
			man.applyChanges(man.addAxioms(target, imported.getAxioms()));
		}
		if(ontType.equals(RootOntology.EMPTY_ONT)) 
			man.applyChanges(man.addAxioms(target, ont.getAxioms()));
		System.out.println("done");
	}
	
	
	/**
	 * Get ontology
	 * @return Ontology
	 */
	public OWLOntology getOntology() {
		return ont;
	}
	
	
	/**
	 * Main ontology types; either an empty ontology or take the input ontology and work from there 
	 */
	public enum RootOntology { 
		EMPTY_ONT, INPUT_ONT; 
	}

	
	/**
	 * Load DBQ ontology
	 * @param filePath	File path
	 * @return OWL ontology
	 */
	public static OWLOntology loadDBQOntology(String filePath) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/datamodel"), IRI.create("file:/Users/rgoncalves/Documents/workspace/facsimile/ontology/datamodel.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://who.int/icf"), IRI.create("file:/Users/rgoncalves/Documents/workspace/facsimile/ontology/icf_simplified_2013.11.22.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/cfa"), IRI.create("file:/Users/rgoncalves/Documents/workspace/facsimile/ontology/ides_cfa.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/dbq"), IRI.create("file:/Users/rgoncalves/Documents/workspace/facsimile/ontology/ides_dbq.owl")));
		man.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.org/facsimile/criteria"), IRI.create("file:/Users/rgoncalves/Documents/workspace/facsimile/ontology/criteria.owl")));
		
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config = config.addIgnoredImport(IRI.create("http://purl.org/facsimile/mos"));
		
		File ontFile = new File(filePath);
		System.out.print("Loading ontology: " + ontFile.getAbsolutePath() + "... ");
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new FileDocumentSource(ontFile), config);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		System.out.println("done");
		return ont;
	}
	
	
	/**
	 * main
	 * @param args	arguments: 
	 * 	arg[0]: ontology file path
	 * 	arg[1]: output file path
	 * @throws OWLOntologyCreationException	Ontology creation exception
	 * @throws OWLOntologyStorageException	Ontology storage exception
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {
		String outputPath = args[1];
		OWLOntology ont = loadDBQOntology(args[0]);
		
		System.out.println("Creating union ontology... ");
		OntologyUtils util = new OntologyUtils(ont, "http://purl.org/facsimile/main");
		OWLOntology union = util.getUnionOntology(RootOntology.EMPTY_ONT);
		
		System.out.print("done\nSaving ontology... ");
		
		RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();
		format.setPrefix("dbq", "http://purl.org/facsimile/dbq");
		union.saveOntology(format, IRI.create("file:" + outputPath));
		
		System.out.println("done");
	}
}
