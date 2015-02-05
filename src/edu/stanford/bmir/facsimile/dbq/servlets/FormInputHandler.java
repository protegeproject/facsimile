package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
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
import org.semanticweb.owlapi.rdf.model.RDFGraph;
import org.semanticweb.owlapi.rdf.model.RDFTranslator;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import edu.stanford.bmir.facsimile.dbq.Runner;
import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElementList;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section.SectionType;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormInputHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<Section> sections;
	private Date date;
	private String uuid, dateStr, dateShortStr;
	private Map<String,FormElement> eIri;
	private Map<String,String> eTextMap, eFocusMap;
	private Map<String,SectionType> eSectionType;
	private Map<String,Map<String,String>> eOptions;
	private Configuration conf;
	private Map<IRI,IRI> aliases;
	private OWLOntology inputOnt;

    
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
	 * Sets the uuid according to the session-stored value, if still handling the same session. If a new session
	 * is identified, then a new uuid value is set. The date gets updated regardless. Because the files only use
	 * the short date (no time), a user can keep working on the same form so long as the browser is not refreshed,
	 * and the file will be getting overwritten (to avoid multiple entries for the same form submission, e.g., going 
	 * back and fixing an answer)
	 * @param session	Http session
	 */
	private void sortIdentifiers(HttpSession session) {
		if(session.getAttribute("uuid") != null)
			uuid = (String)session.getAttribute("uuid");
		else
			uuid = getID();
		dateStr = getDate();
		dateShortStr = getShortDate(date);
	}
	
	
	/**
	 * Process input from the form
	 * @param request	Html request
	 * @param response	Html response
	 */
	@SuppressWarnings("unchecked")
	private void processInput(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		sortIdentifiers(session);
		aliases = (Map<IRI, IRI>) session.getAttribute("aliases");
		try {
			session.setAttribute("uuid", uuid);
			session.setAttribute("date", dateStr);
			PrintWriter pw = response.getWriter();
			System.out.print("\nParsing form input... ");
			createElementMaps(session);
			
			File outDir = new File("output"); outDir.mkdirs();
			String outName = "output/" + dateShortStr + "-form-" + uuid;
			
			// CSV file
			String csv = getCSVFile(request.getParameterNames(), request);
			session.setAttribute(uuid + "-csv", csv);
			serialize(csv, outName + ".csv");

			// OWL ontology
			OWLOntology ont = getOntology(request.getParameterNames(), request);
			IRI ont_import = (IRI) session.getAttribute("iri");
			AddImport imp = new AddImport(ont, ont.getOWLOntologyManager().getOWLDataFactory().getOWLImportsDeclaration(ont_import));
			ont.getOWLOntologyManager().applyChange(imp);
			session.setAttribute(uuid + "-owl", ont);
			serialize(ont, outName + ".owl");

			// RDF triple dump
			RDFTranslator trans = new RDFTranslator(ont.getOWLOntologyManager(), ont, true);
			RDFGraph graph = trans.getGraph();
			for(OWLAxiom ax : ont.getAxioms())
				ax.accept(trans);
			session.setAttribute(uuid + "-rdf", graph);
			serialize(graph, outName + ".rdf");
			
			printOutputPage(pw);
			pw.close();
			System.out.println("done\n  Submission UUID: " + uuid + "\n  Submission date: " + dateStr + "\nfinished");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Serialize the given string 
	 * @param output	String to be output
	 * @param path	File path (incl. filename and extension)	
	 */
	private void serialize(String output, String path) {
		try {
			FileUtils.writeStringToFile(new File(path), output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Serialize an ontology
	 * @param ont	OWL ontology
	 * @param path	File path (incl. filename and extension)
	 */
	private void serialize(OWLOntology ont, String path) {
		try {
			ont.getOWLOntologyManager().saveOntology(ont, new FileDocumentTarget(new File(path)));
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Serialize a dump of RDF triples in the given graph
	 * @param graph	RDF graph
	 * @param path	File path (incl. filename and extension)
	 */
	private void serialize(RDFGraph graph, String path) {
		try {
			graph.dumpTriples(new FileWriter(new File(path)));
		} catch (IOException e) {
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
		Map<IRI,Set<OWLNamedIndividual>> answerMap = new HashMap<IRI,Set<OWLNamedIndividual>>();
		Map<FormElementList,String> eleListMap = new HashMap<FormElementList,String>();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();		
		OWLNamedIndividual initInfo = null, finalInfo = null, formDataInd = df.getOWLNamedIndividual(IRI.create("formdata-" + uuid)); 
		OWLOntology ont = createOntology(man, df, formDataInd);
		while(paramNames.hasMoreElements()) {
			String qIriAlias = (String)paramNames.nextElement(), qIri = qIriAlias; 	// element iri: Question individual IRI, or InformationElement property IRI
			if(aliases.containsKey(IRI.create(qIriAlias)))
				qIri = aliases.get(IRI.create(qIriAlias)).toString();
			String[] params = request.getParameterValues(qIriAlias);	// answer(s)
			String qFocus = eFocusMap.get(qIri);				// element focus
			SectionType type = eSectionType.get(qIri);			// section type
			 
			OWLNamedIndividual dataInd = df.getOWLNamedIndividual(IRI.create(getName(type, qIriAlias, "-data-") + uuid));
			addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getOutputClass()), dataInd));	// { data : AnnotatedData }
			addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), formDataInd, dataInd));	// { formDataInd hasComponent data }
			
			processGroupData(qIri, qIriAlias, eleListMap, dataInd, type, answerMap, df);
			
			OWLNamedIndividual answerInd = df.getOWLNamedIndividual(IRI.create(getName(type, qIriAlias, "-obs-") + uuid)); // answerInd is instance of one of (Observation | PatientInformation | PhysicianInformation)				
			if(type.equals(SectionType.QUESTION_SECTION))
				addQuestionSectionAxioms(ont, man, df, answerInd, dataInd, qFocus, qIri);
			else { 
				if(type.equals(SectionType.PATIENT_SECTION)) {
					addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getInitialSectionClassBinding()), answerInd));	// { answer : PatientInformation }
					if(initInfo == null) initInfo = answerInd;
				}
				else if(type.equals(SectionType.PHYSICIAN_SECTION)) {
					addPhysicianSectionAxioms(ont, man, df, answerInd);
					if(finalInfo == null) finalInfo = answerInd;
				}
				addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsAnswerToPropertyBinding()), dataInd, df.getOWLNamedIndividual(IRI.create(qFocus))));	// { data isResponseTo P*InformationDataElement }
			}
			addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasAnswerPropertyBinding()), dataInd, answerInd));	// { data hasAnswer answer }
			processAnswerData(params, qIri, qIriAlias, type, ont, answerInd, initInfo, finalInfo);
		}
		addDataRelations(answerMap, ont);
		return ont;
	}
	
	
	
	private void processGroupData(String qIri, String qIriAlias, Map<FormElementList,String> eleListMap, OWLNamedIndividual dataInd, SectionType type, Map<IRI,Set<OWLNamedIndividual>> answerMap, OWLDataFactory df) {
		FormElementList eleList = eIri.get(qIri).getFormElementList();
		if(eleList.isRepeating() || eleListMap.containsKey(eleList) || qIri != qIriAlias) {
			System.out.println("qIriAlias: " + qIriAlias);
			String s = qIriAlias.substring(qIriAlias.lastIndexOf("-rep-"), qIriAlias.length());
			System.out.println("\tsuffix: " + s);
			String dataGroupStr = getName(type, qIri, s + "-group-data-");
//			if(eleListMap.containsKey(eleList))
//				dataGroupStr = eleListMap.get(eleList);
//			else {
//				dataGroupStr = getName(type, qIri, "-group-data-" + uuid);
//				eleListMap.put(eleList, dataGroupStr);
//			}
			addAnswer(dataGroupStr, dataInd, answerMap);
			addAnswer(qIri, df.getOWLNamedIndividual(IRI.create(dataGroupStr)), answerMap);
		}
		else addAnswer(qIri, dataInd, answerMap);
	}
	
	
	/**
	 * Add an answer to the given answers map
	 * @param qIri	Question IRI
	 * @param ind	Data individual
	 * @param map	Map of answers
	 */
	private void addAnswer(String qIri, OWLNamedIndividual ind, Map<IRI,Set<OWLNamedIndividual>> map) {
		IRI i = IRI.create(qIri);
		Set<OWLNamedIndividual> inds = null;
		if(map.containsKey(i)) {
			inds = map.get(i);
			inds.add(ind);
		}
		else {
			inds = new HashSet<OWLNamedIndividual>();
			inds.add(ind);
		}
		map.put(i, inds);
	}
	
	
	/**
	 * Add question section axioms
	 * @param ont	OWL ontology
	 * @param man	OWL ontology manager
	 * @param df	OWL data factory
	 * @param answerInd	Answer individual
	 * @param dataInd	Data individual
	 * @param qFocus	Question focus
	 * @param qIri	Question IRI
	 */
	private void addQuestionSectionAxioms(OWLOntology ont, OWLOntologyManager man, OWLDataFactory df, OWLNamedIndividual answerInd, OWLNamedIndividual dataInd, String qFocus, String qIri) {
		// { answer : Observation }
		addAxiom(ont, df.getOWLClassAssertionAxiom(
				df.getOWLClass(conf.getQuestionSectionClassBinding()), answerInd));
		
		// { answer hasFocus focus }
		if(!qFocus.equals(""))
			addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(
					df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding()), answerInd, df.getOWLNamedIndividual(IRI.create(qFocus))));
		
		// { data isResponseTo question }
		addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(
				df.getOWLObjectProperty(conf.getIsAnswerToPropertyBinding()), dataInd, df.getOWLNamedIndividual(IRI.create(qIri))));
	}
	
	
	/**
	 * Add physician section axioms
	 * @param ont	OWL ontology
	 * @param man	OWL ontology manager
	 * @param df	OWL data factory
	 * @param answerInd	Answer individual
	 */
	private void addPhysicianSectionAxioms(OWLOntology ont, OWLOntologyManager man, OWLDataFactory df, OWLNamedIndividual answerInd) {
		// { answer : PhysicianInformation }
		addAxiom(ont, df.getOWLClassAssertionAxiom(
				df.getOWLClass(conf.getFinalSectionClassBinding()), answerInd));
		
		// { physicianCert : PhysicianCertification }
		OWLNamedIndividual physicianCertInd = df.getOWLNamedIndividual(IRI.create("certification-" + uuid));
		addAxiom(ont, df.getOWLClassAssertionAxiom(
				df.getOWLClass(conf.getPhysicianCertificationClassBinding()), physicianCertInd));
		
		// { physicianCert hasComponent answer }
		addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(
				df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), physicianCertInd, answerInd));
		
		// { physicianCert hasDate date }
		addAxiom(ont, df.getOWLDataPropertyAssertionAxiom(
				df.getOWLDataProperty(conf.getHasDatePropertyBinding()), physicianCertInd, df.getOWLLiteral(dateStr, OWL2Datatype.XSD_DATE_TIME)));
	}
	
	
	/**
	 * Create output ontology and add basic form-data axioms
	 * @param man	OWL ontology manager
	 * @param df	OWL data factory
	 * @param formDataInd	Form data individual
	 * @return OWL ontology
	 */
	private OWLOntology createOntology(OWLOntologyManager man, OWLDataFactory df, OWLNamedIndividual formDataInd) {
		OWLOntology ont = null;
		try { 
			ont = man.createOntology(getOntologyIRI());
		} catch (OWLOntologyCreationException e) { 
			e.printStackTrace();
		}
		addCommentAnnotations(ont);
		addAxiom(ont, df.getOWLClassAssertionAxiom(
				df.getOWLClass(conf.getFormDataClassBinding()), formDataInd));	// { formDataInd : FormData }
		addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(
				df.getOWLObjectProperty(conf.getHasFormPropertyBinding()), formDataInd, df.getOWLNamedIndividual(conf.getFormIndividualIRI()))); // { formDataInd hasForm form }
		return ont;
	}
	
	
	/**
	 * Add 'hasComponent' data relations
	 * @param map	Map of question IRIs to their -data- individuals
	 * @param ont	Output ontology
	 */
	private void addDataRelations(Map<IRI,Set<OWLNamedIndividual>> map, OWLOntology ont) {
		OWLOntologyManager man = ont.getOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		for(IRI iri : map.keySet()) {
			FormElement element = eIri.get(iri.toString());
			Set<OWLNamedIndividual> answers = map.get(iri);
			for(OWLNamedIndividual answer : answers) {
				if(element != null) { 
					for(IRI parentIri : element.getParentElements()) {
						Set<OWLNamedIndividual> parents = map.get(parentIri);
						for(OWLNamedIndividual parentDataInd : parents) {
							if(parentDataInd != null) {
								addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), parentDataInd, answer));
								addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsComponentOfPropertyBinding()), answer, parentDataInd));
							}
						}
					}
				} else {
					OWLNamedIndividual group = df.getOWLNamedIndividual(iri);
					addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasComponentPropertyBinding()), group, answer));
					addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsComponentOfPropertyBinding()), answer, group));
				}
			}
		}
	}
	
	
	/**
	 * Process answers to a given question
	 * @param params	Form values
	 * @param qIri	Question IRI
	 * @param qIriAlias	Question IRI alias (where applicable)
	 * @param type	Section type
	 * @param ont	OWL ontology
	 * @param answerInd	Answer individual
	 * @param initInfo	Individual representing patient information
	 * @param finalInfo	Individual representing physician information
	 */
	private void processAnswerData(String[] params, String qIri, String qIriAlias, SectionType type, OWLOntology ont, OWLNamedIndividual answerInd, OWLNamedIndividual initInfo, OWLNamedIndividual finalInfo) {
		OWLOntologyManager man = ont.getOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
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
					valInd = df.getOWLNamedIndividual(IRI.create(qIriAlias + "-val-" + uuid));
					addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getDataElementValueClassBinding()), valInd));	// { val : DataElementValue }
				}
				addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding()), answerInd, valInd));	// { answer hasValue val }

				if(!inputOnt.containsEntityInSignature(IRI.create(aIri), Imports.INCLUDED) && !aIri.isEmpty())
					addAxiom(ont, df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), valInd.getIRI(), df.getOWLLiteral(aIri)));	// { rdfs:label(val) }
			}
			else {
				if(answerInd == null)
					if(type.equals(SectionType.PATIENT_SECTION))
						answerInd = initInfo;
					else if(type.equals(SectionType.PHYSICIAN_SECTION))
						answerInd = finalInfo;
				addAxiom(ont, df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(IRI.create(qIri)), answerInd, aIri));	// { answer hasX val } where X is given by the question IRI
			}
		}
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
	 * @param ont	OWL ontology
	 * @param axiom	OWL axiom
	 */
	private void addAxiom(OWLOntology ont, OWLAxiom axiom) {
		OWLOntologyManager man = ont.getOWLOntologyManager();
		man.applyChange(new AddAxiom(ont, axiom));
	}
	
	
	/**
	 * Get the CSV file output
	 * @param paramNames	Enumeration of parameters
	 * @param request	Http request
	 * @return String containing the output CSV file
	 */
	private String getCSVFile(Enumeration<String> paramNames, HttpServletRequest request) {
		String csv = "question IRI,answer IRI (where applicable),question text,answer text,question focus,parent question\n"; 
		while(paramNames.hasMoreElements()) {
			String qIriAlias = (String)paramNames.nextElement(), qIri = qIriAlias; // element iri
			if(aliases.containsKey(IRI.create(qIriAlias)))
				qIri = aliases.get(IRI.create(qIriAlias)).toString();
			String[] params = request.getParameterValues(qIriAlias);
			String qFocus = eFocusMap.get(qIri);	// element focus
			String qText = eTextMap.get(qIri);	// element text
			if(qText != null) {
				qText = qText.replaceAll(",", ";");
				qText = qText.replaceAll("\n", "");
			}
			csv += addAnswer(params, qIri, qIriAlias, qText, qFocus);
		}
		return csv;
	}
	
	
	/**
	 * Add question and answer details to a csv
	 * @param params	Answers
	 * @param qIri	Question IRI
	 * @param qIriAlias	Question IRI alias (if any)
	 * @param qText	Question text
	 * @param qFocus	Question focus class IRI
	 * @return String CSV entry / entries
	 */
	private String addAnswer(String[] params, String qIri, String qIriAlias, String qText, String qFocus) {
		String csv = "";
		for(int i = 0; i < params.length; i++) {
			String answer = params[i];
			Map<String,String> aMap = eOptions.get(qIri);
			String aIri = "";
			if(aMap != null)
				for(String s : aMap.keySet())
					if(aMap.get(s).equalsIgnoreCase(answer)) {
						aIri = s; break;
					}
			if(answer.contains(","))
				answer = answer.replaceAll(",", "");
			if(aIri.equalsIgnoreCase(""))
				aIri = answer;
			
			FormElement ele = eIri.get(qIri);
			IRI parentIri = ele.getParentElement();
			String parentIriStr = "";
			if(parentIri != null) {
				parentIriStr = parentIri.toString();
				if(aliases.containsValue(parentIri))
					for(IRI iri : aliases.keySet())
						if(aliases.get(iri).equals(parentIri)) {
							parentIriStr = iri.toString();
							break;
						}
			}
			csv += (qIriAlias.equals(qIri) ? qIri : qIriAlias) + "," + aIri + "," + qText + "," + answer + "," + qFocus + "," + parentIriStr + "\n";
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
		date = new Date();
		String dateString = dateFormat.format(date);
		return dateString;
	}

	
	/**
	 * Get current date: short format
	 * @param d	Date
	 * @return String containing current date
	 */
	private String getShortDate(Date d) {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String dateString = dateFormat.format(d);
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
		pw.append("<input type=\"submit\" value=\"CSV\" name=\"filetype\">&nbsp;");
		pw.append("<input type=\"submit\" value=\"RDF\" name=\"filetype\">&nbsp;");
		pw.append("<input type=\"submit\" value=\"OWL\" name=\"filetype\">&nbsp;\n");
		pw.append("</div>\n</form>\n</div>\n</body>\n</html>");
	}
	
	
	/**
	 * Gather the details of each form element
	 * @param session	Http session
	 */
	@SuppressWarnings("unchecked")
	private void createElementMaps(HttpSession session) {
		eIri = new HashMap<String,FormElement>();
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
				eIri.put(qIri, ele);
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
		
		return IRI.create("http://purl.org/facsimile/" + formName + "_" + dateShortStr + "_" + uuid);
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
				df.getOWLLiteral("form data submitted on: " + dateStr));
		
		ont.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ont, ann));
		ont.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ont, ann2));
	}
}