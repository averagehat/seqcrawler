package org.irisa.genouest.seqcrawler.webapp;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.irisa.genouest.seqcrawler.CrawlerSearch.Export;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Servlet to export a query range to a file. Returns a link to the file. The webapp needs
 *  a directory where to place the file which can be accessed via a web server for download.
 * @author osallou
 *
 */
public class ExportServlet extends HttpServlet {

	private Logger log = LoggerFactory.getLogger(ExportServlet.class);
	
	String downloadUrl = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	
		downloadUrl =  getServletConfig().getInitParameter("downloadUrl");
		Export export = new Export();
		export.setQuery(request.getParameter("query"));
		export.setQueryType(getServletConfig().getInitParameter("queryType"));
		export.setExportRanges(request.getParameter("ranges").split(","));
		UUID random = UUID.randomUUID();
    	String outputFile = getServletConfig().getInitParameter("downloadDir")+"/tmpexport_"+random.toString()+".txt";
		export.setOutputFile(outputFile);
		export.setUrl(getServletConfig().getInitParameter("solrUrl"));
		
		try {
			export.export();
		} catch (ParseException e) {
			log.error(e.getMessage());
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
		} catch (SAXException e) {
			log.error(e.getMessage());
		} catch (SolrServerException e) {
			log.error(e.getMessage());
		}
		
		response.getWriter().print("{ \"url\" : \""+downloadUrl+"/tmpexport_"+random.toString()+".txt\"}");
		
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
