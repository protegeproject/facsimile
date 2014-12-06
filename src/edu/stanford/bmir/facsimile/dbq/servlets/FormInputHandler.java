package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
@WebServlet("/FormInputHandler")
public class FormInputHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public FormInputHandler() {}

    
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
			PrintWriter pw = response.getWriter();
			System.out.println("Parsing form input...");
			Enumeration<String> paramNames = request.getParameterNames();
			String csv = "";
			while(paramNames.hasMoreElements()) {
				String paramName = (String)paramNames.nextElement();
				csv += paramName + ",";
				System.out.print("Question: " + paramName + "\n");
				String[] params = request.getParameterValues(paramName);
				System.out.print("Answer(s): ");
				for(int i = 0; i < params.length; i++) {
					csv += params[i];
					System.out.print(params[i] + " ");
					if(i<(params.length-1)) csv += ",";
					else System.out.println();
				}
				System.out.println();
				csv += "\n";
			}
			System.out.println(csv);
			printOutputPage(pw);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		pw.append("<p>Your input has been successfully received. Feel free to close this window.</p>\n");
		pw.append("</div>\n</body>\n</html>");
	}
}
