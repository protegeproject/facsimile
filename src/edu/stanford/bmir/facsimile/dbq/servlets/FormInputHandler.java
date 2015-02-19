package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
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
	private String uuid, dateStr, dateShortStr, baseNmsp, nmsp;
	private Map<String,FormElement> eIri;
	private Map<String,String> eTextMap, eFocusMap;
	private Map<String,SectionType> eSectionType;
	private Map<String,Map<String,String>> eOptions;
	private Configuration conf;
	private Map<IRI,IRI> aliases;
	private OWLOntology inputOnt;
	private Date date;

    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		RequestDispatcher view = getServletContext().getRequestDispatcher("/index.html");
		try {
			view.forward(request, response);
		} catch (ServletException e) {
			e.printStackTrace();
		}
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
		// store values in session
		session.setAttribute("uuid", uuid);
		session.setAttribute("date", dateStr);
	}
	
	
	/**
	 * Process input from the form
	 * @param request	Html request
	 * @param response	Html response
	 */
	@SuppressWarnings("unchecked")
	private void processInput(HttpServletRequest request, HttpServletResponse response) {
		System.out.print("\nParsing form input... ");
		HttpSession session = request.getSession();
		sortIdentifiers(session);
		aliases = (Map<IRI, IRI>) session.getAttribute("aliases");
		try {
			createElementMaps(session);
			PrintWriter pw = response.getWriter();
			baseNmsp = "http://purl.org/facsimile/"; 
			nmsp = inputOnt.getOntologyID().getOntologyIRI().get().toString() + "#";
			
			File outDir = new File("output"); outDir.mkdirs();
			String outName = "output/" + dateShortStr + "-form-" + uuid;
			
			// CSV file
			String csv = getCSVFile(request.getParameterNames(), request);
			session.setAttribute(uuid + "-csv", csv); serialize(csv, outName + ".csv");

			OWLOntology ont = getOntology(request.getParameterNames(), request),
					ont2 = OWLManager.createOWLOntologyManager().copyOntology(ont, OntologyCopy.DEEP);
			
			// Serialize RDF triple dump
			session.setAttribute(uuid + "-rdf", ont2);
			serialize(ont2, outName + ".nt", new NTriplesDocumentFormat());
			
			// Serialize OWL ontology with import
			IRI ont_import = (IRI) session.getAttribute("iri");
			ont.getOWLOntologyManager().applyChange(new AddImport(ont, ont.getOWLOntologyManager().getOWLDataFactory().getOWLImportsDeclaration(ont_import)));
			session.setAttribute(uuid + "-owl", ont); serialize(ont, outName + ".owl", new RDFXMLDocumentFormat());
			
			printOutputPage(pw); pw.close();
			System.out.println("done\n  Submission UUID: " + uuid + "\n  Submission date: " + dateStr + "\nfinished");
		} catch (IOException | OWLOntologyCreationException e) {
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
	 * @param format	Ontology document format
	 */
	private void serialize(OWLOntology ont, String path, OWLDocumentFormat format) {
		try {
			ont.getOWLOntologyManager().saveOntology(ont, format, new FileDocumentTarget(new File(path)));
		} catch (OWLOntologyStorageException e) {
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
		OWLNamedIndividual initInfo = null, finalInfo = null, formDataInd = df.getOWLNamedIndividual(IRI.create(nmsp + "formdata-" + uuid)); 
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
				addObjPropAssertAxiom(ont, conf.getIsAnswerToPropertyBinding(), dataInd, df.getOWLNamedIndividual(IRI.create(qFocus)));	// { data isResponseTo P*InformationDataElement }
			}
			addObjPropAssertAxiom(ont, conf.getHasAnswerPropertyBinding(), dataInd, answerInd);	// { data hasAnswer answer }
			processAnswerData(params, qIri, qIriAlias, type, ont, answerInd, initInfo, finalInfo);
			addComponentPartonomy(qIri, qIriAlias, dataInd, formDataInd, type, df, ont);
		}
		return ont;
	}
	
	
	/**
	 * Add hasComponent / isComponentOf partonomy 
	 * @param qIri	Question IRI
	 * @param qIriAlias	Question IRI alias
	 * @param dataInd	Data individual
	 * @param formDataInd	Form data individual
	 * @param type	Section type
	 * @param df	OWL data factory
	 * @param ont	OWL ontology
	 */
	private void addComponentPartonomy(String qIri, String qIriAlias, OWLNamedIndividual dataInd, OWLNamedIndividual formDataInd, SectionType type, OWLDataFactory df, OWLOntology ont) {
		FormElement element = eIri.get(qIri);
		FormElementList eleList = element.getFormElementList();
		IRI hasComp = conf.getHasComponentPropertyBinding(), isCompOf = conf.getIsComponentOfPropertyBinding();
		String questionListStr = getName(type, "qlist-" + eleList.getID(), "-data-" + uuid),
				questionListRepStr = getName(type, "qlist-" + eleList.getID(), getSubstring(qIriAlias, "-rep-") + "-data-" + uuid),
				childQuestionListStr = getName(type, "qlist-" + element.getIRI().getShortForm(), "-data-" + uuid),
				parentQuestionListStr = getName(type, "qlist-" + getParentQuestionList(element), "-data-" + uuid);
		
		OWLNamedIndividual questionListInd = df.getOWLNamedIndividual(IRI.create((questionListStr.startsWith(nmsp) ? "" : nmsp) + questionListStr)), 
				questionListRepInd = df.getOWLNamedIndividual(IRI.create((questionListRepStr.startsWith(nmsp) ? "" : nmsp) + questionListRepStr)),
				childQuestionListInd = df.getOWLNamedIndividual(IRI.create((childQuestionListStr.startsWith(nmsp) ? "" : nmsp) + childQuestionListStr)),
				parentQuestionListInd = df.getOWLNamedIndividual(IRI.create((parentQuestionListStr.startsWith(nmsp) ? "" : nmsp) + parentQuestionListStr));
		
		if(element.hasChildren()) {
			addObjPropAssertAxiom(ont, hasComp, questionListInd, childQuestionListInd);	// { questionListData hasComponent childQuestionListData }
			addObjPropAssertAxiom(ont, isCompOf, childQuestionListInd, questionListInd);	// { childQuestionListData isComponentOf questionListData }
		}
		if(eleList.isRepeating() || qIri != qIriAlias) {
			addObjPropAssertAxiom(ont, hasComp, questionListInd, questionListRepInd);	// { questionListData hasComponent questionListRepData }
			addObjPropAssertAxiom(ont, isCompOf, questionListRepInd, questionListInd);	// { questionListRepData isComponentOd questionListData }

			addObjPropAssertAxiom(ont, hasComp, questionListRepInd, dataInd);		// { questionListRepData hasComponent data }
			addObjPropAssertAxiom(ont, isCompOf, dataInd, questionListRepInd);		// { data isComponentOf questionListRepData }
		}
		else if(type.equals(SectionType.QUESTION_SECTION)) {
			addObjPropAssertAxiom(ont, hasComp, questionListInd, dataInd);			// { questionListData hasComponent data }
			addObjPropAssertAxiom(ont, isCompOf, dataInd, questionListInd);		// { data isComponentOf questionListData }
		}
		if(!element.hasParents()) {
			addObjPropAssertAxiom(ont, hasComp, formDataInd, questionListInd);		// { formData hasComponent questionListData }
			addObjPropAssertAxiom(ont, isCompOf, questionListInd, formDataInd);	// { questionListData isComponentOf formData }
		}
		else {
			addObjPropAssertAxiom(ont, hasComp, parentQuestionListInd, questionListInd);
			addObjPropAssertAxiom(ont, isCompOf, questionListInd, parentQuestionListInd);
		}
	}
	
	
	/**
	 * Get the text identifier of the 2nd most specific question list to the given element
	 * @param element	Form element
	 * @return Text identifier of 2nd most specific question list
	 */
	private String getParentQuestionList(FormElement element) {
		String name = "";
		Set<FormElementList> lists = element.getFormElementLists();
		FormElementList mostSpecific = element.getFormElementList(), secMostSpecific = null;
		int min = mostSpecific.size();
		for(FormElementList l : lists)
			if(!l.equals(mostSpecific))
				if(l.size() > min) {
					min = l.size();
					secMostSpecific = l;
				}
		if(secMostSpecific != null)
			name = secMostSpecific.getID();
		return name;
	}
	

	/**
	 * Get a substring of the given string starting at the specified point until the end of the string
	 * @param input	Input string
	 * @param begin	Beginning of the substring
	 * @return Substring of the given string
	 */
	private String getSubstring(String input, String begin) {
		String out = "";
		try { out = input.substring(input.lastIndexOf(begin), input.length()); }
		catch(IndexOutOfBoundsException e) { /* do nothing */ }
		return out;
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
		addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getQuestionSectionClassBinding()), answerInd));
		
		// { answer hasFocus focus }
		if(!qFocus.equals(""))
			addObjPropAssertAxiom(ont, conf.getQuestionFocusPropertyBinding(), answerInd, df.getOWLNamedIndividual(IRI.create(qFocus)));
		
		// { data isResponseTo question }
		addObjPropAssertAxiom(ont, conf.getIsAnswerToPropertyBinding(), dataInd, df.getOWLNamedIndividual(IRI.create(qIri)));
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
		addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getFinalSectionClassBinding()), answerInd));
		
		// { physicianCert : PhysicianCertification }
		OWLNamedIndividual physicianCertInd = df.getOWLNamedIndividual(IRI.create(nmsp + "certification-" + uuid));
		addAxiom(ont, df.getOWLClassAssertionAxiom(
				df.getOWLClass(conf.getPhysicianCertificationClassBinding()), physicianCertInd));
		
		// { physicianCert hasComponent answer }
		addObjPropAssertAxiom(ont, conf.getHasComponentPropertyBinding(), physicianCertInd, answerInd);
		
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
		addAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getFormDataClassBinding()), formDataInd));	// { formDataInd : FormData }
		addObjPropAssertAxiom(ont, conf.getHasFormPropertyBinding(), formDataInd, df.getOWLNamedIndividual(conf.getFormIndividualIRI())); // { formDataInd hasForm form }
		return ont;
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
				addObjPropAssertAxiom(ont,conf.getQuestionValuePropertyBinding(), answerInd, valInd);	// { answer hasValue val }

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
			output = nmsp + "patient" + mid;
		else if(type.equals(SectionType.PHYSICIAN_SECTION))
			output = nmsp + "physician" + mid;
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
	 * Add an object property assertion axiom to the specified ontology
	 * @param ont	OWL ontology
	 * @param propIri	Object property IRI
	 * @param ind1	OWL individual
	 * @param ind2	OWL individual
	 */
	private void addObjPropAssertAxiom(OWLOntology ont, IRI propIri, OWLNamedIndividual ind1, OWLNamedIndividual ind2) {
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		addAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(propIri), ind1, ind2));
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
			if(qText != null)
				qText = qText.replaceAll("\"", "\"\"");
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
			if(answer.contains("\""))	
				answer = answer.replaceAll("\"", "\"\"");
			if(aIri.equalsIgnoreCase(""))	
				aIri = answer;
			String parentIriStr = getParentElementIRI(eIri.get(qIri));
			csv += "\"" + (qIriAlias.equals(qIri) ? qIri : qIriAlias) + "\",\"" + aIri + "\",\"" + qText + "\",\"" + answer + "\",\"" + qFocus + "\",\"" + parentIriStr + "\"\n";
		}
		return csv;
	}
	
	
	/**
	 * Get the IRI of the form element that is the direct parent of the given form element 
	 * @param ele	Form element
	 * @return IRI of the parent form element to the one given
	 */
	private String getParentElementIRI(FormElement ele) {
		String parentIriStr = "";
		IRI parentIri = ele.getParentElement();
		if(parentIri != null) {
			parentIriStr = parentIri.toString();
			if(aliases.containsValue(parentIri))
				for(IRI iri : aliases.keySet())
					if(aliases.get(iri).equals(parentIri)) {
						parentIriStr = iri.toString();
						break;
					}
		}
		return parentIriStr;
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
		pw.append("<input type=\"submit\" value=\"OWL\" name=\"filetype\">\n");
		pw.append("</div>\n</form>\n<br>\n<p><a href=\"index.html\">Back to main page</a></p>\n");
		pw.append("</div>\n</body>\n</html>");
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
		return IRI.create(baseNmsp + formName + "_" + dateShortStr + "_" + uuid);
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