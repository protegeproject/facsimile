package edu.stanford.bmir.facsimile.dbq.servlets;

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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Section;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
@WebServlet("/FormInputHandler")
public class FormInputHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<String> outputOptions;
	private final String uuid, date;
	private Map<String,String> eTextMap, eFocusMap;
	private Map<String,Map<String,String>> eOptions;
	private Configuration conf;
	private OWLOntology inputOnt;
	
	
    /**
     * Constructor
     */
    public FormInputHandler() {
    	outputOptions = new ArrayList<String>();
    	uuid = getID();
    	date = getDate();
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
		try {
			request.getSession().setAttribute("uuid", uuid);
			PrintWriter pw = response.getWriter();
			System.out.print("\nParsing form input... ");
			createElementMaps(request);
			
			// CSV file
			if(request.getSession().getAttribute(uuid + "-csv") == null) {
				String csv = getCSVFile(request.getParameterNames(), request);
				request.getSession().setAttribute(uuid + "-csv", csv);
				outputOptions.add("csv");
			}
			
			if(request.getSession().getAttribute(uuid + "-owl") == null) {
				OWLOntology ont = getOntology(request.getParameterNames(), request);
				request.getSession().setAttribute(uuid + "-owl", ont);
				outputOptions.add("owl");
			}
			
			printOutputPage(pw);
			pw.close();
			System.out.println("done");
			System.out.println("  Submission UUID: " + uuid);
			System.out.println("  Submission date: " + date);
			System.out.println("finished");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private OWLOntology getOntology(Enumeration<String> paramNames, HttpServletRequest request) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		OWLOntology ont = null;
		try { ont = man.createOntology(); } 
		catch (OWLOntologyCreationException e) { e.printStackTrace(); }
		
		while(paramNames.hasMoreElements()) {
			String qIri = (String)paramNames.nextElement(); // element iri: Question individual IRI, or InformationElement property IRI
			String[] params = request.getParameterValues(qIri);
			String qFocus = eFocusMap.get(qIri);	// element focus
			
			// { data : AnnotatedData }
			OWLNamedIndividual dataInd = df.getOWLNamedIndividual(IRI.create(qIri + "-ans"));
			man.applyChange(new AddAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getOutputClass()), dataInd)));

			// { obs : Observation }
			OWLNamedIndividual result = df.getOWLNamedIndividual(IRI.create(qIri + "-obs"));
			if(inputOnt.containsIndividualInSignature(IRI.create(qIri)))
				man.applyChange(new AddAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLClass(conf.getObservationClass()), result)));
			else {
				// Patient / Physician information
			}

			// { data hasAnswer obs }
			man.applyChange(new AddAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getHasAnswerPropertyBinding()), dataInd, result)));

			// { data isAnswerTo question }
			if(inputOnt.containsIndividualInSignature(IRI.create(qIri), Imports.INCLUDED))
				man.applyChange(new AddAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getIsAnswerToPropertyBinding()), dataInd, df.getOWLNamedIndividual(IRI.create(qIri)))));
			
			// { obs hasFocus focus }
			if(inputOnt.containsIndividualInSignature(IRI.create(qFocus), Imports.INCLUDED))
				man.applyChange(new AddAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding()), result, df.getOWLNamedIndividual(IRI.create(qFocus)))));
			else if(inputOnt.containsClassInSignature(IRI.create(qFocus)))
				man.applyChange(new AddAxiom(ont, df.getOWLClassAssertionAxiom(df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(conf.getQuestionFocusPropertyBinding()), df.getOWLClass(IRI.create(qFocus))), result)));
			
			// handle multiple answers to single question
			for(int i = 0; i < params.length; i++) {
				Map<String,String> aMap = eOptions.get(qIri);
				String aIri = "";
				if(aMap != null && aMap.values().contains(params[i])) {
					for(String s : aMap.keySet())
						if(aMap.get(s).equals(params[i]))
							aIri = s;
				}
				if(aIri.equalsIgnoreCase(""))
					aIri = params[i];
				
				// { obs hasValue val }
				if(inputOnt.containsIndividualInSignature(IRI.create(aIri), Imports.INCLUDED))
					man.applyChange(new AddAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding()), result, df.getOWLNamedIndividual(IRI.create(aIri)))));
				else {
					OWLNamedIndividual valInd = df.getOWLNamedIndividual(IRI.create(qIri + "-val")); 
					man.applyChange(new AddAxiom(ont, df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(conf.getQuestionValuePropertyBinding()), result, valInd)));
					man.applyChange(new AddAxiom(ont, df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(conf.getQuestionDataValuePropertyBinding()), valInd, aIri)));
				}
			}
		}
		return ont;
	}
	
	
	/**
	 * Get the CSV file output
	 * @param paramNames	Enumeration of parameters
	 * @param request	Http request
	 * @return String containing the output CSV file
	 */
	private String getCSVFile(Enumeration<String> paramNames, HttpServletRequest request) {
		String csv = "date," + date + "\n";
		csv += "uuid," + uuid + "\n";
		csv += "question IRI,answer IRI (where applicable),question text,answer text,question focus\n"; 
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
			Map<String,String> aMap = eOptions.get(qIri);
			String aIri = "";
			if(aMap != null && aMap.values().contains(params[i])) {
				for(String s : aMap.keySet())
					if(aMap.get(s).equals(params[i]))
						aIri = s;
			}
			if(aIri.equalsIgnoreCase(""))
				aIri = params[i];
			csv += qIri + "," + aIri + "," + qText + "," + params[i] + "," + qFocus + "\n"; 
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
	 * Get current date and time in the format "yyyy/MM/dd-HH:mm:ss"
	 * @return String containing current date and time
	 */
	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
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
	 * Gather back the text and focus of each form element
	 * @param request	Http request
	 */
	@SuppressWarnings("unchecked")
	private void createElementMaps(HttpServletRequest request) {
		eTextMap = new HashMap<String,String>();
		eFocusMap = new HashMap<String,String>();
		eOptions = (Map<String,Map<String,String>>) request.getSession().getAttribute("questionMap");
		List<Section> sections = (List<Section>) request.getSession().getAttribute("questionList");
		for(Section s : sections) {
			for(FormElement ele : s.getSectionElements()) {
				String qIri = ele.getEntity().getIRI().toString();
				eTextMap.put(qIri, ele.getText());
				eFocusMap.put(qIri, ele.getFocus());
			}
		}
		conf = (Configuration)request.getSession().getAttribute("configuration");
		inputOnt = (OWLOntology)request.getSession().getAttribute("ontology");
	}
}