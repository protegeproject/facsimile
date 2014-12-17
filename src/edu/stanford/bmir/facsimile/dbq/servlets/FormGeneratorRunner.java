package edu.stanford.bmir.facsimile.dbq.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import edu.stanford.bmir.facsimile.dbq.Runner;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
@WebServlet("/FormGeneratorRunner")
public class FormGeneratorRunner extends HttpServlet {
	private static final long serialVersionUID = 1L;

	
    /**
     * Constructor
     */
    public FormGeneratorRunner() {}

    
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
		try {
			PrintWriter pw = response.getWriter();
			File config = null;
			FileItemFactory factory = new DiskFileItemFactory(); 		// Create a factory for disk-based file items
			ServletFileUpload upload = new ServletFileUpload(factory);	// Create a new file upload handler
			List<FileItem> items = upload.parseRequest(request); 		// Parse the request
			if(items != null) {
				// Process uploaded items
				Iterator<FileItem> iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					String name = item.getFieldName();
					if(name.equals("conf")) {
						InputStream input = item.getInputStream();
						if(input != null && input.available() != 0) {
							config = File.createTempFile("stream2file", ".xml");
							config.deleteOnExit();
							FileOutputStream out = new FileOutputStream(config);
							IOUtils.copy(input, out);
						}
					}
				}
			}
			Runner run = new Runner(config, false);
			String output = run.run();
			request.getSession().setAttribute("configuration", run.getConfiguration());
			request.getSession().setAttribute("sectionList", run.getSections());
			request.getSession().setAttribute("questionOptions", run.getQuestionOptions());
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html;charset=UTF-8");
			pw.append(output);
			pw.close();
		} catch (IOException | FileUploadException e) {
			e.printStackTrace();
		}
	}

}
