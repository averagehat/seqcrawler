package org.irisa.genouest.seqcrawler.index.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.Constants.STORAGEIMPL;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.handlers.field.FieldRecoder;
import org.irisa.genouest.seqcrawler.index.handlers.gff.GFF3Record;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * GFF sequence handler. Reads a {@link GFF3Record} records file and index all records.
 * <br/> <p>If storage is active, it expects to find a <b>##FASTA</b> line separation between GFF records and Fasta content.</p>
 * <p>When separation line is found, all following content should be FASTA formated.</p>
 * <p>Use {@link FastaHandler} to handle the FASTA part</p>
 * @author osallou
 */
public class GFFHandler implements SequenceHandler {
	
	private IndexManager indexManager = null;
	
	private long nbParsingErrors = 0;

	private Logger log = LoggerFactory.getLogger(GFFHandler.class);
	
    private Vector<GFF3Record> records = new Vector<GFF3Record>();

    private String bank=Constants.BANK_DEFAULT;
    
    private String sourceFile = null;
    
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
    	sourceFile = f.getAbsolutePath();
    	BufferedReader bf = new BufferedReader(new FileReader(f));
    	this.parse(bf);
    	bf.close();
    }
    
    /**
	  * Parse an input stream
	  * @param bf stream to analyse
	  * @throws IOException
     * @throws IndexException 
	  */
    public void parse(BufferedReader bf) throws IOException, IndexException {
    	long nbDocs = 0;
    	boolean goFasta = false;
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
                        	// recode?
                        	String recodeKey = bank+"."+attribute[0]+".recode";
                        	if(indexManager.getArgs().containsKey(recodeKey)) {          
                        		String className = indexManager.getArgs().get(recodeKey);
                        		try {
									Class recodeClass = Class.forName(className);
									FieldRecoder recoder = (FieldRecoder) recodeClass.newInstance();
									String[][] newAttributes = recoder.recode(attribute[0], attribute[1]);
									if(newAttributes!=null) {
									for(int na = 0; na < newAttributes.length; na++) {
									rec.getAnnotations().setProperty(newAttributes[na][0], newAttributes[na][1]);
									}
									}
									
								} catch (ClassNotFoundException e) {
									log.error(e.getMessage());
								} catch (InstantiationException e) {
									log.error(e.getMessage());
								} catch (IllegalAccessException e) {
									log.error(e.getMessage());
								}          
                        	}
                        	else {
                            rec.getAnnotations().setProperty(attribute[0], attribute[1]);
                        	}
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
                    doc.addField("stream_content_type", "biosequence/gff");
                    // If stream_name not already present in gff attributes
    			    if(sourceFile!=null && doc.getFieldValue("stream_name")==null) {
    			    	doc.addField("stream_name", sourceFile);
    			    }
    			    
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
            else {
            	if(line.startsWith("##FASTA")) {
            		log.debug("GFF definitions are over, now parsing fasta content");
            		goFasta = true;
            		break;
            	}
            }
        }
        
        try {
			indexManager.getServer().commit();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
		}
		
		if(goFasta && indexManager.getArgs().containsKey(Constants.STORE)) {
			log.info("Parse Fasta of GFF file");
			FastaHandler fastaHandler = new FastaHandler(this.bank);
			fastaHandler.setIndexManager(indexManager);
			fastaHandler.parseFasta(fr);
		}
		
		log.info("Number of documents indexed: "+nbDocs);
    }


	public void setIndexManager(IndexManager manager) {
		this.indexManager = manager;
		
	}

	public long getNbParsingErrors() {
		return nbParsingErrors;
	}
	
	/**
	 * @deprecated
	 * Sample function to send FASTA raw data in GFF file to a remote backend
	 */
	@SuppressWarnings("unused")
	private void storeFasta() {
		
		 StorageManager storageMngr = new StorageManager();
		 String host = indexManager.getArgs().get("stHost");
		 String port = indexManager.getArgs().get("stPort");
		 HashMap<String,String> map = new HashMap<String,String>();
		 if(host!=null) {
			 map.put("host", host);
			 log.debug("Using host "+host);
		 }
		 if(port!=null) {
			 map.put("port", host);
			 log.debug("Using port "+host);
		 }
		 if(map.size()>0) {
		 storageMngr.setArgs(map);
		 }
		 StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.RIAK);

		 StorageObject stObj = new StorageObject();
		  stObj.setId("sampleid");
		  HashMap<String,String> list = new HashMap<String,String>();
		  list.put("meta1", "value1");
		  list.put("meta2", "value2");  
		  stObj.setMetadata(list);
		  String content = "abcdeaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabba";
		  stObj.setContent(content);
		  
			try {
				storage.store(stObj);
			} catch (StorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  
		  
	}
}

