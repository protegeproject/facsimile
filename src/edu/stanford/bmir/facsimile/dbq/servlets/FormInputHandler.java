package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.rdf.model.RDFTranslator;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import edu.stanford.bmir.facsimile.dbq.Runner;
import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section.SectionType;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
@WebServlet("/FormInputHandler")
public class FormInputHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<String> outputOptions;
	private List<Section> sections;
	private final String uuid, date, dateShort;
	private Map<String,String> eTextMap, eFocusMap;
	private Map<String,SectionType> eSectionType;
	private Map<String,Map<String,String>> eOptions;
	private Configuration conf;
	private OWLOntology inputOnt;

	
    /**
     * Constructor
     */
    public FormInputHandler() {
    	outputOptions = new ArrayList<String>(3);
    	uuid = getID();
    	date = getDate();
    	dateShort = getDateShort();
    }

    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processInput(request, response);
	}

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processInput(request, response);
	}

	
	/**
	 * Process input from the form
	 * @param request	Html request
	 * @param response	Html response
	 */
	private void processInput(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		try {
			session.setAttribute("uuid", uuid);
			session.setAttribute("date", date);
			PrintWriter pw = response.getWriter();
			System.out.print("\nParsing form input... ");
			createElementMaps(request);
			
			File outDir = new File("output"); outDir.mkdirs();
			String outName = "output/" + dateShort + "-form-" + uuid;
			
			// CSV file
			if(session.getAttribute(uuid + "-csv") == null) {
				String csv = getCSVFile(request.getParameterNames(), request);
				session.setAttribute(uuid + "-csv", csv);
				outputOptions.add("csv");

				// Serialize CSV
				FileUtils.writeStringToFile(new File(outName + ".csv"), csv);
			}
			// OWL & RDF file
			if(session.getAttribute(uuid + "-owl") == null) {
				OWLOntology ont = getOntology(request.getParameterNames(), request);
				IRI ont_import = (IRI) session.getAttribute("iri");
				AddImport imp = new AddImport(ont, ont.getOWLOntologyManager().getOWLDataFactory().getOWLImportsDeclaration(ont_import));
				ont.getOWLOntologyManager().applyChange(imp);
				session.setAttribute(uuid + "-owl", ont);
				outputOptions.add("rdf"); outputOptions.add("owl");
				
				// RDF triple dump
				RDFTranslator trans = new RDFTranslator(ont.getOWLOntologyManager(), ont, true);
				for(OWLAxiom ax : ont.getAxioms())
	                ax.accept(trans);
				session.setAttribute(uuid + "-rdf", trans.getGraph());
				
				// Serialize files
				trans.getGraph().dumpTriples(new FileWriter(new File(outName + ".xml")));
				ont.getOWLOntologyManager().saveOntology(ont, new FileDocumentTarget(new File(outName + ".owl")));
			}
			
			printOutputPage(pw);
			pw.close();
			System.out.println("done\n  Submission UUID: " + uuid + "\n  Submission date: " + date + "\nfinished");
		} catch (IOException | OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get the output ontology
	 * @param paramNames	Enumeration of form parameters
	 * @param request	Http request
	 * @return OWL ontology
	 */
	private OWLOntology getOntology(Enumeration<String> paramNames, HttpServletRequest request) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		OWLOntology ont = null;
		try { ont = man.createOntology(getOntologyIRI()); } 
		catch (OWLOntologyCreationException e) { e.printStackTrace(); }
		addCommentAnnotations(ont);
		
		OWLNamedIndividual initInfo = null, finalInfo = null; 
		OWLNamedIndividual formDataInd = df.getOWLNamedIndividual(IRI.create("formdata-" + uuid)); 
		addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getFormDataClassBinding()), formDataInd));	// { formDataInd : FormData }
		addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasFormPropertyBinding()), formDataInd, df.getOWLNamedIndividual(conf.getFormIndividualIRI()))); // { formDataInd hasForm form }
		
		while(paramNames.hasMoreElements()) {
			String qIri = (String)paramNames.nextElement(); 	// element iri: Question individual IRI, or InformationElement property IRI
			String[] params = request.getParameterValues(qIri);	// answer(s)
			String qFocus = eFocusMap.get(qIri);				// element focus
			SectionType type = eSectionType.get(qIri);			// section type

			OWLNamedIndividual dataInd = null, answerInd = null; // answerInd is instance of one of (Observation | PatientInformation | PhysicianInformation)
			if((type.equals(SectionType.PATIENT_SECTION) && initInfo == null) || (type.equals(SectionType.PHYSICIAN_SECTION) && finalInfo == null) || type.equals(SectionType.QUESTION_SECTION)) {
				dataInd = df.getOWLNamedIndividual(IRI.create(getName(type, qIri, "-data-") + uuid));
				addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getOutputClass()), dataInd));	// { data : AnnotatedData }
				addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), formDataInd, dataInd));	// { formDataInd hasComponent data }
				
				answerInd = df.getOWLNamedIndividual(IRI.create(getName(type, qIri, "-obs-") + uuid));				
				if(type.equals(SectionType.QUESTION_SECTION)) {
					addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getQuestionSectionClassBinding()), answerInd));	// { answer : Observation }
					if(!qFocus.equals(""))
						addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding()), answerInd, df.getOWLNamedIndividual(IRI.create(qFocus))));	// { answer hasFocus focus }
					addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsAnswerToPropertyBinding()), dataInd, df.getOWLNamedIndividual(IRI.create(qIri))));		// { data isResponseTo question }
				}
				else { 
					if(type.equals(SectionType.PATIENT_SECTION)) {
						addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getInitialSectionClassBinding()), answerInd));	// { answer : PatientInformation }
						initInfo = answerInd;
					}
					else if(type.equals(SectionType.PHYSICIAN_SECTION)) {
						addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getFinalSectionClassBinding()), answerInd));	// { answer : PhysicianInformation }
						
						OWLNamedIndividual physicianCertInd = df.getOWLNamedIndividual(IRI.create("certification-" + uuid));
						addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getPhysicianCertificationClassBinding()), physicianCertInd));	// { physicianCert : PhysicianCertification }
						addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), physicianCertInd, answerInd));	// { physicianCert hasComponent answer }
						addAxiom(man, ont, df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(conf.getHasDatePropertyBinding()), physicianCertInd, df.getOWLLiteral(date, OWL2Datatype.XSD_DATE_TIME)));	// { physicianCert hasDate date }
						
						finalInfo = answerInd;
					}
					addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsAnswerToPropertyBinding()), dataInd, df.getOWLNamedIndividual(IRI.create(qFocus))));	// { data isResponseTo P*InformationDataElement }
				}
				addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasAnswerPropertyBinding()), dataInd, answerInd));	// { data hasAnswer answer }
			}

			// handle multiple answers to single question
			for(int i = 0; i < params.length; i++) {
				Map<String,String> aMap = eOptions.get(qIri);
				String aIri = "";  
				if(aMap != null)
					for(String s : aMap.keySet())
						if(aMap.get(s).equalsIgnoreCase(params[i])) {
							aIri = s; break;
						}
				if(aIri.equalsIgnoreCase(""))
					aIri = params[i];
				
				if(type.equals(SectionType.QUESTION_SECTION)) {
					OWLNamedIndividual valInd = null;
					if(inputOnt.containsEntityInSignature(IRI.create(aIri), Imports.INCLUDED))
						valInd = df.getOWLNamedIndividual(IRI.create(aIri));
					else {
						valInd = df.getOWLNamedIndividual(IRI.create(qIri + "-val-" + uuid));
						addAxiom(man, ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getDataElementValueClassBinding()), valInd));	// { val : DataElementValue }
					}
					addAxiom(man, ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding()), answerInd, valInd));	// { answer hasValue val }
					
					if(!inputOnt.containsEntityInSignature(IRI.create(aIri), Imports.INCLUDED) && !aIri.isEmpty())
						addAxiom(man, ont, df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), valInd.getIRI(), df.getOWLLiteral(aIri)));	// { rdfs:label(val) }
				}
				else {
					if(answerInd == null) {
						if(type.equals(SectionType.PATIENT_SECTION))
							answerInd = initInfo;
						else if(type.equals(SectionType.PHYSICIAN_SECTION))
							answerInd = finalInfo;
					}
					addAxiom(man, ont, df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(IRI.create(qIri)), answerInd, aIri));	// { answer hasX val } where X is given by the question IRI
				}
			}
		}
		return ont;
	}
	
	
	/**
	 * Get an instance name based on the type of section
	 * @param type	Section type
	 * @param qIri	Question IRI
	 * @param mid	Middle identifier
	 * @return String with the instance name
	 */
	private String getName(SectionType type, String qIri, String mid) {
		String output = qIri;
		if(type.equals(SectionType.QUESTION_SECTION))
			output = qIri + mid;
		else if(type.equals(SectionType.PATIENT_SECTION))
			output = "patient" + mid;
		else if(type.equals(SectionType.PHYSICIAN_SECTION))
			output = "physician" + mid;
		return output;
	}
	
	
	/**
	 * Add given axiom to specified ontology
	 * @param man	OWL ontology manager
	 * @param ont	OWL ontology
	 * @param axiom	OWL axiom
	 */
	private void addAxiom(OWLOntologyManager man, OWLOntology ont, OWLAxiom axiom) {
		man.applyChange(new AddAxiom(ont, axiom));
	}
	
	
	/**
	 * Get the CSV file output
	 * @param paramNames	Enumeration of parameters
	 * @param request	Http request
	 * @return String containing the output CSV file
	 */
	private String getCSVFile(Enumeration<String> paramNames, HttpServletRequest request) {
		String csv = "question IRI,answer IRI (where applicable),question text,answer text,question focus\n"; 
		while(paramNames.hasMoreElements()) {
			String qIri = (String)paramNames.nextElement(); // element iri
			String[] params = request.getParameterValues(qIri);
			String qFocus = eFocusMap.get(qIri);	// element focus
			String qText = eTextMap.get(qIri);	// element text
			qText = qText.replaceAll(",", ";");
			qText = qText.replaceAll("\n", "");
			csv += addAnswer(params, qIri, qText, qFocus);
		}
		return csv;
	}
	
	
	/**
	 * Add question and answer details to a csv
	 * @param params	Answers
	 * @param qIri	Question IRI
	 * @param qText	Question text
	 * @param qFocus	Question focus class IRI
	 * @return String CSV entry / entries
	 */
	private String addAnswer(String[] params, String qIri, String qText, String qFocus) {
		String csv = "";
		for(int i = 0; i < params.length; i++) {
			String answer = params[i];
			if(!answer.isEmpty()) {
				Map<String,String> aMap = eOptions.get(qIri);
				String aIri = "";
				if(aMap != null) {
					for(String s : aMap.keySet())
						if(aMap.get(s).equalsIgnoreCase(answer)) {
							aIri = s; break;
						}
				}
				if(answer.contains(","))
					answer = answer.replaceAll(",", "");
				if(aIri.equalsIgnoreCase(""))
					aIri = answer;
				csv += qIri + "," + aIri + "," + qText + "," + answer + "," + qFocus + "\n";
			}
		}
		return csv;
	}
	
	
	/**
	 * Generate and retrieve a random UUID
	 * @return String representation of the generated UUID
	 */
	private String getID() {
		UUID u = UUID.randomUUID();
		String uuid = u.toString();
		return uuid;
	}
	
	
	/**
	 * Get current date and time in the format specified for xsd:dateTime: "yyyy-MM-ddTHH:mm:ssXXX", where "T" denotes the start of the time, 
	 * and 3 "X" gives the timezone in ISO 8601 3-letter format (e.g., +08:00)
	 * @return String containing current date and time
	 */
	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		String dateString = dateFormat.format(new Date());
		return dateString;
	}

	
	/**
	 * Get current date: short format
	 * @return String containing current date
	 */
	private String getDateShort() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String dateString = dateFormat.format(new Date());
		return dateString;
	}

	
	/**
	 * Print a final page acknowledging receipt of input data 
	 * @param pw	Print writer
	 */
	private void printOutputPage(PrintWriter pw) {
		pw.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Form Generator</title>\n<meta charset=\"utf-8\"/>\n");
		pw.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\">\n");
		pw.append("<link rel=\"icon\" type=\"image/png\" href=\"style/favicon.ico\"/>");
		pw.append("<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\">\n");
		pw.append("</head>\n<body>\n<div class=\"bmir-style\">\n");
		pw.append("<h1>Thank you</h1>\n");
		pw.append("<p>Your input has been successfully received. You can keep a copy of your answers in any of the formats below.</p><br>\n");
		pw.append("<form action=\"file\" method=\"post\">\n");
		pw.append("<div class=\"button-section\">\n");
		for(String type : outputOptions)
			pw.append("<input type=\"submit\" value=\"" + type.toUpperCase() + "\" name=\"filetype\">&nbsp;");
		pw.append("</div>\n</form>\n</div>\n</body>\n</html>");
	}
	
	
	/**
	 * Gather the details of each form element
	 * @param request	Http request
	 */
	@SuppressWarnings("unchecked")
	private void createElementMaps(HttpServletRequest request) {
		HttpSession session = request.getSession();
		eTextMap = new HashMap<String,String>();
		eFocusMap = new HashMap<String,String>();
		eSectionType = new HashMap<String,SectionType>();
		eOptions = (Map<String,Map<String,String>>) session.getAttribute("questionOptions");
		sections = (List<Section>) session.getAttribute("sectionList");
		for(Section s : sections) {
			for(FormElement ele : s.getSectionElements()) {
				String qIri = ele.getEntity().getIRI().toString();
				eTextMap.put(qIri, ele.getText());
				eFocusMap.put(qIri, ele.getFocus());
				eSectionType.put(qIri, s.getType());
			}
		}
		conf = (Configuration)session.getAttribute("configuration");
		inputOnt = (OWLOntology)session.getAttribute("ontology");
	}
	
	
	/**
	 * Generate an IRI for the output ontology
	 * @return IRI of output ontology
	 */
	private IRI getOntologyIRI() {
		String formName = conf.getOutputFileTitle();
		if(formName.contains(" "))
			formName = formName.replaceAll(" ", "_");
		
		return IRI.create("http://purl.org/facsimile/" + formName + "_" + dateShort + "_" + uuid);
	}
	
	
	/**
	 * Add comment annotations to the ontology specifying the submission date and time, and version of the tool
	 * @param ont	OWL ontology
	 */
	private void addCommentAnnotations(OWLOntology ont) {
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		
		OWLAnnotation ann = df.getOWLAnnotation(df.getRDFSComment(), 
				df.getOWLLiteral("ontology created by " + Runner.name + " v" + Runner.version));
		OWLAnnotation ann2 = df.getOWLAnnotation(df.getRDFSComment(), 
				df.getOWLLiteral("form data submitted on: " + date));
		
		ont.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ont, ann));
		ont.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ont, ann2));
	}
}