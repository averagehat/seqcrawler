package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.handlers.gff.GFF3Record;


/**
 * GFF sequence handler. Reads a GFF3 records file and index all records
 * @author osallou
 */
public class GFFHandler implements SequenceHandler {
	
	private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Log log = LogFactory.getLog(GFFHandler.class);
	
    private Vector<GFF3Record> records = new Vector<GFF3Record>();

    private String bank=Constants.BANK_DEFAULT;
    
    public Vector<GFF3Record> getRecords() {
        return records;
    }
    
    public GFFHandler() {
    	
    }
    
    public GFFHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
    	}
    }
    
    /**
	 * Parse an input file
	 * @param f path to input file
	 * @throws IOException
     * @throws IndexException 
	 */
    public void parse(File f) throws IOException, IndexException {
    	log.debug("Parse new file: "+ f.getAbsolutePath());
    	BufferedReader bf = new BufferedReader(new FileReader(f));
    	this.parse(bf);
    }
    
    /**
	  * Parse an input stream
	  * @param bf stream to analyse
	  * @throws IOException
     * @throws IndexException 
	  */
    public void parse(BufferedReader bf) throws IOException, IndexException {
    	long nbDocs = 0;
    	if(indexManager==null) throw new IndexException("IndexManager is not set");
        BufferedReader fr = bf;
        String line = null;
        while ((line = fr.readLine()) != null) {
            if (!line.equals("") && !line.startsWith("#")) {            	
                GFF3Record rec = new GFF3Record();
                Pattern gffPattern = Pattern.compile("(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)");
                Matcher gffMatcher = gffPattern.matcher(line);
                if (gffMatcher.find()) {
                	boolean inError = false;
                	log.debug("new GFF line");
                	try {
                	rec.setBank(bank);
                    rec.setSequenceID(gffMatcher.group(1));
                    rec.setType(gffMatcher.group(3));
                    rec.setStart(Integer.valueOf(gffMatcher.group(4)));
                    rec.setEnd(Integer.valueOf(gffMatcher.group(5)));
                    rec.setStrand(gffMatcher.group(6));                   
                    rec.setAttributes(gffMatcher.group(9));
                    String[] attributes = gffMatcher.group(9).split(";");
                    for (int count = 0; count < attributes.length; count++) {
                        String[] attribute = attributes[count].split("=");
                        if (attribute != null && attribute.length > 1) {
                            rec.getAnnotations().setProperty(attribute[0], attribute[1]);
                        }
                    }
                    
                	}
                	catch (NumberFormatException indexReadError) {
                		log.error("#LINEINDEXERROR: "+line);
                		nbParsingErrors++;
                		inError = true;
                	}
                	if(!inError) {
                    SolrInputDocument doc = rec.getDocument();                    
                    try {
            			this.log.debug("Index new GFF record "+doc.toString());
            			indexManager.getServer().add(doc);			
            		} catch (SolrServerException e) {
            			this.log.error(e.getMessage());
            		} catch (IOException e) {
            			this.log.error(e.getMessage());
            		}
                    
                    nbDocs++;
                    
                	}
                	

                }

            }
        }
        try {
			indexManager.getServer().commit();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
		}
		log.info("Number of documents indexed: "+nbDocs);
    }


	public void setIndexManager(IndexManager manager) {
		this.indexManager = manager;
		
	}

	public long getNbParsingErrors() {
		return nbParsingErrors;
	}
}

