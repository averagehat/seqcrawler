package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.apache.solr.util.NamedList;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.handlers.gff.GFF3Record;

public class RawFileHandler implements SequenceHandler {
	
private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Log log = LogFactory.getLog(RawFileHandler.class);
	
    private String bank=Constants.BANK_DEFAULT;

    public RawFileHandler() {
    	
    }
    
    public RawFileHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
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
        req.setParam(ExtractingParams.EXTRACT_ONLY, "true");
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
