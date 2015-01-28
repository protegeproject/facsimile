package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import edu.stanford.bmir.facsimile.dbq.Runner;
import edu.stanford.bmir.facsimile.dbq.exception.MissingConfigurationFileException;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class FormGeneratorRunner extends HttpServlet {
	private static final long serialVersionUID = 1L;

    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		generateForm(request, response);
	}

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		generateForm(request, response);
	}
	
	
	/**
	 * Generate form from the given configuration file
	 * @param request	Html request
	 * @param response	Html response
	 */
	private void generateForm(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(true);
		if(!session.isNew()) session.invalidate();
		session = request.getSession(true);
		
		File config = getConfigurationFile(request, response);
		Runner runner = new Runner(config, false);
		
		PrintWriter pw = null;
		String output = null;
		
		try {
			pw = response.getWriter();
			output = runner.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(output != null) {
			session.setAttribute("configuration", runner.getConfiguration());
			session.setAttribute("sectionList", runner.getSections());
			session.setAttribute("questionOptions", runner.getQuestionOptions());
			session.setAttribute("ontology", runner.getOntology());
			session.setAttribute("iri", runner.getOntology().getOntologyID().getOntologyIRI().get());
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html;charset=UTF-8");
			pw.append(output);
			pw.close();
		}
	}
	
	
	/**
	 * Get the configuration file given as input
	 * @param request	Html request
	 * @param response	Html response
	 * @return Configuration file
	 */
	private File getConfigurationFile(HttpServletRequest request, HttpServletResponse response) {
		File config = null;
		if(request.getContentType() != null)
			config = getFileFromUpload(request, response);
		else
			config = getFileFromURL(request, response);
		if(config == null || !config.exists())
			throw new MissingConfigurationFileException("The given configuration file could not be found. Make sure the URL or file path is correct");
		return config;
	}
	
	
	/**
	 * Get file object from a URL
	 * @param request	Http request
	 * @param response	Http response
	 * @return Configuration file
	 */
	private File getFileFromURL(HttpServletRequest request, HttpServletResponse response) {
		File config = null;
		String path = request.getParameter("conf");
		try {
			if(!path.contains(":"))
//				File classes = new File(this.getClass().getClassLoader().getResource("").getPath());
//				String baseDir = classes.getParentFile().getParentFile().getAbsolutePath();
//				String finalDir = baseDir + File.separator + "conf" + File.separator + path;
				config = new File("conf" + File.separator + path);
			else {
				URL url = new URL(path);
				InputStream input = url.openStream();
				config = File.createTempFile("stream2file", ".xml"); config.deleteOnExit();
				FileOutputStream out = new FileOutputStream(config);
				IOUtils.copy(input, out);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return config;
	}

	
	/**
	 * Get file object from 'upload file' option
	 * @param request	Http request
	 * @param response	Http response
	 * @return Configuration file
	 */
	private File getFileFromUpload(HttpServletRequest request, HttpServletResponse response) {
		File config = null;
		FileItemFactory factory = new DiskFileItemFactory(); 		// Create a factory for disk-based file items
		ServletFileUpload upload = new ServletFileUpload(factory);	// Create a new file upload handler
		try {
			List<FileItem> items = upload.parseRequest(request);
			if(items != null) {
				Iterator<FileItem> iter = items.iterator();
				while (iter.hasNext()) {	// Process uploaded items
					FileItem item = (FileItem) iter.next();
					String name = item.getFieldName();
					if(name.equals("conf")) {
						InputStream input = item.getInputStream();
						if(input != null && input.available() != 0) {
							config = File.createTempFile("stream2file", ".xml"); config.deleteOnExit();
							FileOutputStream out = new FileOutputStream(config);
							IOUtils.copy(input, out);
						}
					}
				}
			}
		} catch (IOException | FileUploadException e) {
			e.printStackTrace();
		}
		return config;
	}
}