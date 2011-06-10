package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.IOException;
import java.util.HashMap;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentHandler {

	private IndexManager indexManager = null;
	private StorageManagerInterface storageInterface = null;
	
    private String bank=Constants.BANK_DEFAULT;
    
    public String sourceFile = null;

	private Logger log = LoggerFactory.getLogger(DocumentHandler.class);
	
	private HashMap<String,String> values = new HashMap<String,String>();
	
	public DocumentHandler(IndexManager manager) {
	 indexManager = manager;
	 String doStore = indexManager.getArgs().get(Constants.STORE);
	 if(doStore!=null && !doStore.equalsIgnoreCase("false")) {
			StorageManager storageManager = new StorageManager();
			storageManager.setArgs(indexManager.getArgs());
		    storageInterface = storageManager.get(indexManager.getStorageImpl());
	 }
	}
	
	public int addRaw(String key, String value) {
		StorageObject object = new StorageObject();
		object.setId(key);
		object.setContent(value);
		try {
			storageInterface.store(object);
		}
		catch (StorageException e) {
			log.error(e.getMessage());
			return 1;
		}
		return 0;
	}
	
	
	public void addField(String key,String value) {
		values.put(key, value);
	}
	
	/**
	 * Adds a document from an identifier and a list of key/values
	 * @param id ID of the doc (unique)
	 * @param values Map of key/value
	 * @param start If from a file, start position in the file
	 * @param size Number of characters read in the file
	 * @return Indexation status
	 */
	public int addDoc(String id,long start, long size) {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id",id);
		doc.addField("bank", bank);
		if(sourceFile!=null) {
		doc.addField("stream_name", sourceFile);
		doc.addField("file", start+"-"+size);
		}
		doc.addField("stream_content_type", "biosequence/fasta");
		for(String keyval : values.keySet()) {
			doc.addField(keyval, values.get(keyval));
		}
		try {
			indexManager.getServer().add(doc);
			values.clear();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			return 1;
		} catch (IOException e) {
			log.error(e.getMessage());
			return 1;
		}
		return 0;
	}

	public void setBank(String bank2) {
		this.bank =  bank2;
	}

	public void setSourceFile(String sourceFile2) {
		this.sourceFile = sourceFile2;
	}
}
