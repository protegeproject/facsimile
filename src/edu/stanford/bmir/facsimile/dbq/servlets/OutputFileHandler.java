package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.rdf.model.RDFGraph;

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
			StringWriter writer = new StringWriter();
			RDFGraph graph = (RDFGraph) session.getAttribute(uuid + "-rdf");
			dumpTriples(graph, writer);
			file = writer.getBuffer().toString();
			response.setHeader("Content-Disposition", "attachment; filename=\"" + date + "-form-" + uuid + ".nt\"");
			break;
		case "OWL":
			StringDocumentTarget target = new StringDocumentTarget();
			OWLOntology ont = (OWLOntology) session.getAttribute(uuid + "-owl");
			ont.saveOntology(target); 
			target.getWriter().flush();
			file = target.getWriter().toString();
			response.setHeader("Content-Disposition", "attachment; filename=\"" + date + "-form-" + uuid + ".owl\"");
			break;
		}
		pw.write(file);
		pw.close();
	}
	
	
	/**
	 * Append to a given writer the triples in the specified RDF graph
	 * @param graph	RDF graph
	 * @param writer	String writer
	 * @throws IOException	IO exception
	 */
	private void dumpTriples(RDFGraph graph, StringWriter writer) throws IOException {
		for(RDFTriple triple : graph.getAllTriples()) {
			writer.append(triple.getSubject() + " " + triple.getPredicate() + " " + (triple.getObject().isLiteral() ? "\"" + triple.getObject() + "\"" : triple.getObject()) + ".\n");
		}
		writer.close();
	}
}
