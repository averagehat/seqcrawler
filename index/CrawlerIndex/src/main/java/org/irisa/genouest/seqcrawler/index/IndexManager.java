package org.irisa.genouest.seqcrawler.index;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;

/**
 * Main class to manage index server connections. IndexManager holds a single instance of server to the remote index server.
 * The class does not support multiple instances for embedded server.
 * @author osallou
 *
 */
public class IndexManager {
	
	private Log log = LogFactory.getLog(IndexManager.class);

	
	//private static SolrServer server = null;
	private SolrServer server = null;
	// For embedded server only
	private CoreContainer coreContainer = null;
	
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

	/**
	 * Clean the remote index
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void cleanIndex() throws SolrServerException, IOException {
		log.warn("Deleting all index");
	    server.deleteByQuery( "*:*" );// delete everything!
	    server.commit();
	}
	
	/**
	 * Clean the remote index according to the bank name filter
	 * @param bank Bank name in the index
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void cleanIndexByBank(String bank) throws SolrServerException, IOException {
		log.warn("Deleting from index bank: "+bank);
	    server.deleteByQuery( "bank:"+bank );// delete everything!
	    server.commit();
	}
}
