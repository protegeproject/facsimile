package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class OutputFileHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String uuid, date;
	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			processRequest(request, response);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			processRequest(request, response);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Process the file type request and return the desired file
	 * @param request	Html request
	 * @param response	Html response
	 * @throws IOException	IO exception
	 * @throws OWLOntologyStorageException	Ontology storage exception
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, OWLOntologyStorageException {
		HttpSession session = request.getSession();
		uuid = (String) session.getAttribute("uuid");
		date = (String) session.getAttribute("date");
		PrintWriter pw = response.getWriter();
		response.setContentType("application/octet-stream");
		String filetype = request.getParameter("filetype");
		String file = null;
		switch(filetype) {
		case "CSV":
			file = (String) session.getAttribute(uuid + "-csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + date + "-form-" + uuid + ".csv\"");
			break;
		case "RDF":
			StringDocumentTarget rdf_target = new StringDocumentTarget();
			OWLOntology rdf = (OWLOntology) session.getAttribute(uuid + "-rdf");
			rdf.saveOntology(new NTriplesDocumentFormat(), rdf_target); 
			rdf_target.getWriter().flush();
			file = rdf_target.getWriter().toString();
			response.setHeader("Content-Disposition", "attachment; filename=\"" + date + "-form-" + uuid + ".nt\"");
			break;
		case "OWL":
			StringDocumentTarget target = new StringDocumentTarget();
			OWLOntology ont = (OWLOntology) session.getAttribute(uuid + "-owl");
			ont.saveOntology(new RDFXMLDocumentFormat(), target); 
			target.getWriter().flush();
			file = target.getWriter().toString();
			response.setHeader("Content-Disposition", "attachment; filename=\"" + date + "-form-" + uuid + ".owl\"");
			break;
		}
		pw.write(file);
		pw.close();
	}
}
