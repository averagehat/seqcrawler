package org.irisa.genouest.seqcrawler.CrawlerSearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Main class to manage index server connections. IndexManager holds a single instance of server to the remote index server.
 * The class does not support multiple instances for embedded server.
 * @author osallou
 *
 */
public class IndexManager {
	
	private Logger log = LoggerFactory.getLogger(IndexManager.class);

	
	//private static SolrServer server = null;
	private SolrServer server = null;
	// For embedded server only
	private CoreContainer coreContainer = null;
	
	Map<String,String> args = new HashMap<String,String>();
	
	
	/**
	 * Main constructor. Will set storage implentation to Riak by default. If a system environment variable "storageImplementation" 
	 * is set, default will be replaced. See {@link Constants}.
	 */
	public IndexManager() {
	}

	public Map<String, String> getArgs() {
		return args;
	}

	/**
	 * Initiate index server manager and starts an embedded server if url is null instead of connecting to a remote URL.
	 * In case of embedded server, a shutdown operation is required at the end of the operations.
	 * @param url URL to the server, null if embedded server
	 * @return The instance of the server manager
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public SolrServer initServer(String url) throws IOException, ParserConfigurationException, SAXException {
        log.info("Using solr home: "+System.getProperty("solr.solr.home"));                
        if(coreContainer!=null) {
        	log.warn("CoreContainer of embedded server has not been shutdown, shutting it down NOW");
        	coreContainer.shutdown();
        	coreContainer = null;
        }
        if(url==null) {
        	 log.info("Using embedded server");
             CoreContainer.Initializer initializer = new CoreContainer.Initializer();
             coreContainer = initializer.initialize();
        	 EmbeddedSolrServer embeddedServer = new EmbeddedSolrServer(coreContainer, "");
        	 this.server=embeddedServer;
        	 return this.server;
        }
        else {
        	log.info("Using remote server "+url);
        	SolrServer remoteserver = new CommonsHttpSolrServer( url );
        	this.server=remoteserver;
        	return this.server;
        }
		
	}
	
	/**
	 * Shutdown the server connection, required for embedded server.
	 */
	public void shutdownServer() {
		if(this.coreContainer!=null) { 
			coreContainer.shutdown();
			coreContainer = null;
		}
	}
	
	/**
	 * Gets the instanciated server manager
	 * @return A SolrServer instance
	 */
	public SolrServer getServer() {
		return this.server;
	}

	/**
	 * Sets the server manager instance
	 * @param remoteserver
	 */
	public void setServer(SolrServer remoteserver) {
		this.server = remoteserver;
	}
	
	public SolrDocumentList queryServer(String query) throws SolrServerException {
		SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        QueryResponse rsp = server.query(solrQuery);
        SolrDocumentList docs = rsp.getResults();
        return docs;
	}
}
