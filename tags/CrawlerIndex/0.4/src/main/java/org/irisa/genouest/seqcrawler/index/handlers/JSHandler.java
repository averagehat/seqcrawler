package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.handlers.gff.GFF3Record;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class JSHandler implements SequenceHandler {
	
	private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Logger log = LoggerFactory.getLogger(JSHandler.class);

    private String bank=Constants.BANK_DEFAULT;
    
    private String sourceFile = null;
    
    private String scriptFile = null;
    
    private String format=null;
    
	public String getScriptFile() {
		return scriptFile;
	}

	public void setScriptFile(String scriptFile) {
		this.scriptFile = scriptFile;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
	
	
    public JSHandler() {
    	
    }
    
    public JSHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
    	}
    }
	
	
	public long getNbParsingErrors() {
		return nbParsingErrors;
	}
	
	

	public void parse(File f) throws IOException, IndexException {
    	log.debug("Parse new file: "+ f.getAbsolutePath());
    	sourceFile = f.getAbsolutePath();
    	BufferedReader bf = new BufferedReader(new FileReader(f));
    	this.parse(bf);
    	bf.close();
	}

	public void parse(BufferedReader bf) throws IOException, IndexException {
		
		FileReader fr = new FileReader(scriptFile);
		
		
		DocumentHandler doc = new DocumentHandler(indexManager,format);
		doc.setBank(bank);
		doc.setSourceFile(sourceFile);
		Context cx = Context.enter();
		
		try {
		Scriptable scope = cx.initStandardObjects();
		Object wrappedDocument = Context.javaToJS(doc, scope);
		ScriptableObject.putProperty(scope, "doc", wrappedDocument);
		if(sourceFile!=null) {
			Object wrappedFile = Context.javaToJS(sourceFile, scope);
			ScriptableObject.putProperty(scope, "filePath", wrappedFile);
		}
		cx.evaluateReader(scope, fr, "<cmd>", 1, null);
		} finally {
		    Context.exit();
		}
	}

	public void setIndexManager(IndexManager manager) {
		 indexManager = manager;

	}
	 

	/**
	 * @param args
	 * @throws SolrServerException 
	 */
	public static void main(String[] args) throws SolrServerException {
		JSHandler js = new JSHandler();
		IndexManager indexMngr = new IndexManager();
		js.setIndexManager(indexMngr);
		try {
			indexMngr.initServer(null);
			indexMngr.cleanIndex();
			BufferedReader bf = null;
			js.setScriptFile("solr/plugin/test.js");
			js.parse(bf);

		} catch (IOException e) { 
			e.printStackTrace();
		} catch (IndexException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		indexMngr.shutdownServer();
	}

}
