package org.irisa.genouest.seqcrawler.index.handlers;

import iubio.readseq.BioseqDoc;
import iubio.readseq.BioseqReader;
import iubio.readseq.Readseq;
import iubio.readseq.SeqFileInfo;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.apache.solr.util.NamedList;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.handlers.gff.GFF3Record;
import org.irisa.genouest.tools.readseq.BioseqParser;
import org.irisa.genouest.tools.readseq.SeqDocToIndexDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadSeqFileHandler implements SequenceHandler {
	
    private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Logger log = LoggerFactory.getLogger(ReadSeqFileHandler.class);
	
    private String bank=Constants.BANK_DEFAULT;
    
    private String streamName = null;

    public ReadSeqFileHandler() {
    	
    }
    
    public ReadSeqFileHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
    	}
    }


	public long getNbParsingErrors() {
		return nbParsingErrors;
	}
	

	/**
	 * Reads a file using ReadSeq to transform content to key/value structure. Sends content
	 * to the index server. If several documents are in file, each document is indexed as a single document.
	 */
	public void parse(File f) {
		long nbDocs = 0;
		long startPos = 0;
		
		streamName = f.getAbsoluteFile().toString();
		
		Readseq rd= new Readseq();
		ArrayList<String[]> keyValues =  new ArrayList<String[]>();
	    try {
			rd.setInputObject( new FileInputStream(f) );
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		} catch (IOException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		}
	    
    	BioseqParser parser =new BioseqParser();
    	parser.setKeyValues(keyValues);
	    
	    CharArrayWriter outwr= new CharArrayWriter(); 
	    SeqDocToIndexDoc sdt= new SeqDocToIndexDoc(); 
	    if ( rd.isKnownFormat() && rd.readInit() )  {
	    try {
			while (rd.readNext()) {

			    SeqFileInfo sfi= rd.nextSeq();
			      //? want also seq info - index in file
			    Object seqdoc= sfi.getdocObject();
			    
			    if (seqdoc instanceof BioseqDoc) {			   			         
			      sdt.setSourceDoc((BioseqDoc)seqdoc); 
			      sdt.writeContentHandler(parser, outwr);
			      }
			
			    outwr.reset();
			    SolrInputDocument doc = new SolrInputDocument();
			    doc.addField("stream_content_type", rd.getBioseqFormat().contentType());
			    doc.addField("stream_name", f.getAbsoluteFile());
			    doc.addField("file", startPos+"-"+rd.getInsReadlen());
			    doc.addField("bank", bank);
			    if(rd instanceof Readseq) {
			    
			    }
			    log.debug("Document position: "+startPos+"-"+rd.getInsReadlen());
			    
			    for(String[] keyval : keyValues) {
			    	String value = keyval[1];
			    	if(doc.containsKey(keyval[0])) {
			    		value += "\n" + (String) doc.getFieldValue(keyval[0]);
			    		doc.removeField(keyval[0]);
			    	}
			    	doc.addField(keyval[0], value);
			    }
			    indexManager.getServer().add(doc);
			    nbDocs++;
			    startPos += rd.getInsReadlen();
			    
			    keyValues.clear();
			    }
		} catch (IOException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		}
		try {
			indexManager.getServer().commit();
			//indexManager.getServer().commit(false, false);
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		} catch (IOException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		}
		log.info("Number of documents indexed: "+nbDocs);
	    
			  }
	    
		
	}
	

	/**
	 * Takes a file as raw input data, not trying to interpret the content. Simply submit it as
	 * a document to the server. Server may interpret it if file format is known ( use Tika and Solr Cell).
	 */
	public void sendFile(File f) throws IOException, IndexException {
		//mime type is biosequence/document to be catched by Tika / Solr Cell
		log.debug("Parse new file: "+ f.getAbsolutePath());
    	SolrServer server = indexManager.getServer();
    	ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
        req.addFile(f);
        req.setParam("stream.type", "biosequence/document");
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
		throw new IndexException("BufferedReader is not supported for ReadSeq data files");
	}

	public void setIndexManager(IndexManager manager) {
		indexManager = manager;
	}

}
