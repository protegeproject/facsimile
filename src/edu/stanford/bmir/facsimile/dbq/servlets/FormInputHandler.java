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

import edu.stanford.bmir.facsimile.dbq.form.elements.FormElement;
import edu.stanford.bmir.facsimile.dbq.form.elements.Question;
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
			
			printOutputPage(pw);
			pw.close();
			System.out.println("done");
			System.out.println("  UUID: " + uuid);
			System.out.println("  Date: " + date);
			System.out.println("finished");
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			String qIri = (String)paramNames.nextElement(); // question iri
			String[] params = request.getParameterValues(qIri);
			String qFocus = eFocusMap.get(qIri);	// question focus
			String qText = eTextMap.get(qIri);	// question text
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
			if(aMap.values().contains(params[i])) {
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
			pw.append("<input type=\"submit\" value=\"" + type.toUpperCase() + "\" name=\"filetype\">");
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
			for(FormElement q : s.getSectionElements()) {
				String qIri = "";
				if(q instanceof Question)
					qIri = ((Question)q).getQuestionIndividual().getIRI().toString();
				else
					qIri = "element-s" + q.getSectionNumber() + ".e" + q.getElementNumber();
				eTextMap.put(qIri, q.getText());
				eFocusMap.put(qIri, q.getFocus());
			}
		}
	}
}