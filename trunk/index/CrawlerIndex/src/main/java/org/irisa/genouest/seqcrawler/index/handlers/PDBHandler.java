package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDBHandler implements SequenceHandler {

	private Logger log = LoggerFactory.getLogger(PDBHandler.class);
	
	private IndexManager indexManager = null;
	
	private String bank=Constants.BANK_DEFAULT;
	
	final static String HEADER = "HEADER";
	
	final static int ID_START = 63;
	final static int ID_END = 66;
	
	final static String TITLE = "TITLE";
	final static int TITLE_START = 11;
	final static int TITLE_END = 80;
	
	final static String EXPDTA = "EXPDTA";
	final static int EXPDTA_START = 11;
	final static int EXPDTA_END = 79;
	
	String title = "";
	String exp = "";
	
	long nberrors=0;
	String sourceFile = null;
	
	
    public PDBHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
    	}
    }
	
	public long getNbParsingErrors() {
		return nberrors;
	}

	public void parse(File f) throws IOException, IndexException {
    	log.debug("Parse new file: "+ f.getAbsolutePath());
    	sourceFile = f.getAbsolutePath();
    	BufferedReader bf = new BufferedReader(new FileReader(f));
    	this.parse(bf);
    	bf.close();
    	
    	try {
			indexManager.getServer().commit();
			//indexManager.getServer().commit(false, false);
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			nberrors++;
		} catch (IOException e) {
			log.error(e.getMessage());
			nberrors++;
		}
 
    	
	}

	public void parse(BufferedReader bf) throws IOException, IndexException {
		String line=null;
		SolrInputDocument doc = null;
		
		long lineStartbyte = 0;
		long docStartbyte= 0;
		long nbytesread = 0;
		int lineendsize = 1;
		String id=null;
		
		while ((line = bf.readLine()) != null) {
			if(line.startsWith(HEADER)) {
				if(doc!=null) {
					doc.addField("title", title);
					doc.addField("expdta", exp);
					doc.addField("bank", bank);
					doc.addField("file", docStartbyte+"-"+nbytesread);
					log.debug("Position in file: "+docStartbyte+"-"+nbytesread);
					nbytesread = 0;
					docStartbyte = lineStartbyte;
					try {
						indexManager.filterDoc(doc);
						indexManager.getServer().add(doc);
					} catch (SolrServerException e) {
						nberrors++;
						log.error(e.getMessage());
					}
				}
				int end = Math.min(line.length(), ID_END);
				id = line.substring(ID_START-1, end);
				
				doc = new SolrInputDocument();
				doc.addField("stream_content_type", "biosequence/pdb");
				if (sourceFile!=null) {
					doc.addField("stream_name", sourceFile);
				}
				doc.addField("id", id);
				log.debug("Found new doc with id "+id);
			}
			if(line.startsWith(TITLE)) {
				int end = Math.min(line.length(), TITLE_END);
				title += " "+line.substring(TITLE_START-1, end);
			}
			if(line.startsWith(EXPDTA)) {
				int end = Math.min(line.length(), EXPDTA_END);
				exp += " "+line.subSequence(EXPDTA_START-1, end);
			}
			nbytesread += line.length() + lineendsize;
			lineStartbyte += line.length() + lineendsize;
			
		}
		
		if(doc!=null) {
			doc.addField("title", title);
			doc.addField("expdta", exp);
			doc.addField("bank", bank);
			doc.addField("file", docStartbyte+"-"+nbytesread);
			log.debug("Position in file: "+docStartbyte+"-"+nbytesread);
			nbytesread = 0;
			docStartbyte = lineStartbyte;
			try {
				indexManager.filterDoc(doc);
				indexManager.getServer().add(doc);
			} catch (SolrServerException e) {
				nberrors++;
				log.error(e.getMessage());
			}
		}

	}

	public void setIndexManager(IndexManager manager) {
		this.indexManager = manager;
	}

}
