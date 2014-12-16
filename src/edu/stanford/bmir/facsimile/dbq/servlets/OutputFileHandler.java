package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
@WebServlet("/OutputFileHandler")
public class OutputFileHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	
    /**
     * Constructor
     */
    public OutputFileHandler() { }

    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}
	
	
	/**
	 * Process the file type request and return the desired file
	 * @param request	Html request
	 * @param response	Html response
	 * @throws IOException	IO exception
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String uuid = (String) request.getSession().getAttribute("uuid");
		PrintWriter pw = response.getWriter();
		response.setContentType("application/octet-stream");
		String filetype = request.getParameter("filetype");
		String file = null;
		switch(filetype) {
		case "CSV":
			file = (String) request.getSession().getAttribute(uuid + "-csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"form.csv\"");
			break;
		case "RDF":
			file = (String) request.getSession().getAttribute(uuid + "-rdf");
			response.setHeader("Content-Disposition", "attachment; filename=\"form.xml\"");
			break;
		case "OWL":
			OWLOntology ont = (OWLOntology) request.getSession().getAttribute(uuid + "-owl");
			ont.getOWLOntologyManager().setOntologyFormat(ont, new OWLXMLDocumentFormat());
			StringDocumentTarget target = new StringDocumentTarget();
			try { ont.saveOntology(target); } 
			catch (OWLOntologyStorageException e) { e.printStackTrace(); }
			target.getWriter().flush();
			file = target.getWriter().toString();
			response.setHeader("Content-Disposition", "attachment; filename=\"form.owl\"");
			break;
		}
		pw.write(file);
		pw.flush();
		pw.close();
	}
}
