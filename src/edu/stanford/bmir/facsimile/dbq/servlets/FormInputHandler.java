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

import edu.stanford.bmir.facsimile.dbq.question.Question;
import edu.stanford.bmir.facsimile.dbq.question.QuestionSection;

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
	private Map<String,String> qTextMap, qFocusMap;
	private Map<String,Map<String,String>> qOptions;
	
	
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
			System.out.println("\nParsing form input...");
			System.out.println("  UUID: " + uuid);
			System.out.println("  Date: " + date + "\n");
			createQuestionMaps(request);
			
			// CSV file
			if(request.getSession().getAttribute(uuid + "-csv") == null) {
				String csv = getCSVFile(request.getParameterNames(), request);
				request.getSession().setAttribute(uuid + "-csv", csv);
				outputOptions.add("csv");
			}
			
			printOutputPage(pw);
			pw.close();
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
			String qFocus = qFocusMap.get(qIri);	// question focus
			String qText = qTextMap.get(qIri);	// question text
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
			Map<String,String> aMap = qOptions.get(qIri);
			String aIri = "";
			if(aMap.values().contains(params[i])) {
				for(String s : aMap.keySet())
					if(aMap.get(s).equals(params[i]))
						aIri = s;
			}
			if(aIri.equalsIgnoreCase(""))
				aIri = params[i];
			
			System.out.println("  Question IRI: " + qIri);
			System.out.println("  Answer IRI: " + aIri);
			System.out.println("  Question text: " + qText);
			System.out.println("  Answer text: " + params[i]);
			System.out.println("  Question focus: " + qFocus);
			
			csv += qIri + "," + aIri + "," + qText + "," + params[i] + "," + qFocus + "\n"; 
			System.out.println();
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
	 * Gather back the text and focus of each question
	 * @param request	Http request
	 */
	@SuppressWarnings("unchecked")
	private void createQuestionMaps(HttpServletRequest request) {
		qTextMap = new HashMap<String,String>();
		qFocusMap = new HashMap<String,String>();
		qOptions = (Map<String,Map<String,String>>) request.getSession().getAttribute("questionMap");
		List<QuestionSection> questions = (List<QuestionSection>) request.getSession().getAttribute("questionList");
		for(QuestionSection s : questions) {
			for(Question q : s.getSectionQuestions()) {
				String qIri = q.getQuestionIndividual().getIRI().toString();
				qTextMap.put(qIri, q.getQuestionText());
				qFocusMap.put(qIri, q.getQuestionFocus());
			}
		}
	}
}