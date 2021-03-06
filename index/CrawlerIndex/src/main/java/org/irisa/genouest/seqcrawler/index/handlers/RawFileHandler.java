package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for raw data file. Data is not interpreted by the program. The file is sent "as is" to the indexing server.
 * The remote server can try to interpret the content (pdf, open office ...) based on  file extension and autodetection.
 * @author osallou
 *
 */
public class RawFileHandler implements SequenceHandler {
	
private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Logger log = LoggerFactory.getLogger(RawFileHandler.class);
	
    //private String bank=Constants.BANK_DEFAULT;

    public RawFileHandler() {
    	
    }
    
    public RawFileHandler(String lib) {
    	if(lib!=null) {
    	//this.bank = lib;
    	}
    }


	public long getNbParsingErrors() {
		return nbParsingErrors;
	}

	/**
	 * Takes a file as raw input data, not trying to interpret the content. Simply submit it as
	 * a document to the server. Server may interpret it if file format is known.
	 */
	public void parse(File f) throws IOException, IndexException {
		log.debug("Parse new file: "+ f.getAbsolutePath());
    	SolrServer server = indexManager.getServer();
    	
    	ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
        req.addFile(f);
        req.setAction(ACTION.COMMIT, false, false);
        
        org.apache.solr.common.util.NamedList<Object> result=null;
		try {
			result = server.request(req);
		} catch (SolrServerException e) {
			log.error("Error while adding document in index");
			nbParsingErrors++;
		}
        log.debug("Result: " + result);

	}

	public void parse(BufferedReader bf) throws IOException, IndexException {
		throw new IndexException("BufferedReader is not supported for Raw data files");
	}

	public void setIndexManager(IndexManager manager) {
		indexManager = manager;
	}

}
