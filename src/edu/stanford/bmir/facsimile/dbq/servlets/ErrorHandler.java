package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class ErrorHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processError(request, response);
	}
	
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processError(request, response);
	}
	
	
	/**
	 * Process exception or error
	 * @param request	Http request
	 * @param response	Http response
	 * @throws IOException	IO exception
	 */
	private void processError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Analyze the servlet exception       
		Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");
		if(servletName == null)
			servletName = "Unknown";

		String requestUri = (String)request.getAttribute("javax.servlet.error.request_uri");
		if(requestUri == null)
			requestUri = "Unknown";

		// Set response content type
		response.setContentType("text/html");

		PrintWriter out = response.getWriter();
		String title = "Error/Exception Information";
		String docType = "<!DOCTYPE html>\n";
		out.println(docType + "<html>\n<head>\n<title>" + title + "</title>\n<meta charset=\"utf-8\"/>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"style/style.css\">\n" 
				+ "<link rel=\"icon\" type=\"image/png\" href=\"style/favicon.ico\"/>\n" + "<link href=\"http://fonts.googleapis.com/css?family=Bitter\" rel=\"stylesheet\" type=\"text/css\">\n"
				+ "</head>\n<body>\n<div class=\"bmir-style\">");

		if(throwable == null && statusCode == null) {
			out.println("<h1>Error occurred (error information missing)</h1>");
			out.println("Please return to the <a href=\"" + response.encodeURL("http://localhost:8080/") + "\">Home Page</a>.");
		} 
		else {	
			out.println("<h1>Error occurred</h1>\n<p>");
			if (statusCode != null)
				out.println("Status code: " + statusCode + "<br><br>");
			out.println("Servlet class name: " + servletName + "<br><br>");
			if(throwable != null) out.println("Exception type: " + throwable.getClass().getName() + "<br><br>");
			out.println("Request URI: " + requestUri + "<br><br>");
			
			if(throwable!=null) {
				String message = throwable.getMessage();
				if(message != null && !message.isEmpty()) {
					message = message.replaceAll("<", "'");
					message = message.replaceAll(">", "'");
					out.println("Exception message: " + message + "<br><br>");
				}
				out.println("Stack trace:\n</p>");
				out.println("<div class=\"inner-wrap\">\n<p>");
				StackTraceElement[] arr = throwable.getStackTrace();
				for(int i = 0; i < arr.length; i++)
					out.println("at " + arr[i] + "<br>" );
				out.println("</p>\n</div>");
			}
		}
		out.println("</div>\n</body>");
		out.println("</html>");
	}
}