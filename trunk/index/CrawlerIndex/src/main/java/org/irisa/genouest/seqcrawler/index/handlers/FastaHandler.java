package org.irisa.genouest.seqcrawler.index.handlers;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.index.SequenceHandler;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastaHandler implements SequenceHandler {

	private IndexManager indexManager = null;
	
	private StorageManagerInterface storageInterface = null;
	
	public StorageManagerInterface getStorageInterface() {
		return storageInterface;
	}

	private long nbParsingErrors = 0;

	private Logger log = LoggerFactory.getLogger(FastaHandler.class);
	
    private String bank=Constants.BANK_DEFAULT;
	
    private String streamName = null;
	
	public long getNbParsingErrors() {
		return nbParsingErrors;
	}
	
	public FastaHandler(String lib) {
    	if(lib!=null) {
    	this.bank = lib;
    	}
    }

	public void parse(File f) throws IOException, IndexException {
		
		
		streamName = f.getAbsoluteFile().toString();
		
		FileReader reader = new FileReader(f);
	    
		this.parseFasta(reader);	
		
		reader.close();
			    
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

	}

	public void parse(BufferedReader bf) throws IOException, IndexException {
		throw new IndexException("BufferedReader is not supported for ReadSeq data files");
	}

	public void setIndexManager(IndexManager manager) {
		 indexManager = manager;
		 String doStore = indexManager.getArgs().get(Constants.STORE);
		 if(doStore!=null && !doStore.equalsIgnoreCase("false") && storageInterface == null) {
				StorageManager storageManager = new StorageManager();
				storageManager.setArgs(indexManager.getArgs());
			    storageInterface = storageManager.get(indexManager.getStorageImpl());
		 }
	}
	
	
	public void parseFasta(Reader in) {
		long nbDocs = 0;
		String doStore = indexManager.getArgs().get(Constants.STORE);
		
		long lineStartbyte = 0;
		long docStartbyte= 0;
		long nbytesread = 0;
		int lineendsize = 1;
		boolean ended = false;
		boolean firstdoc = true;
		SolrInputDocument doc = null;

		BufferedReader bin;
		if (in instanceof BufferedReader)
			bin = (BufferedReader) in;
		else
			bin = new BufferedReader(in);

		// use props for this
		String regex_fasta_id = "^>(\\S+)";
		String regex_fasta_vals = "[;,\\|\\s]+([^;,\\|\\s]+)";
		String regex_fasta_keyval = "(\\w+)[=:](.*)";

		String onep = "\\|?([^|\\s]*)";
		String regex_fasta_ncbi = "^>(\\w+)\\|([^|\\s]+)" + onep + onep + onep
				+ onep; // +"\\s*(.*)";
		// >gi|4027974|gb|AF064181.1|AF064181 Description here...

		Pattern faid = Pattern.compile(regex_fasta_id);
		Pattern faval = Pattern.compile(regex_fasta_vals);
		Pattern fakv = Pattern.compile(regex_fasta_keyval);
		Pattern fancbi = (regex_fasta_ncbi.length() == 0) ? null : Pattern
				.compile(regex_fasta_ncbi);
		Pattern digits = Pattern.compile("[0-9]+");

		String content="";
		
		String aline, id = null;
		Matcher ma;
		int nids = 0;
		try {
			while (ended == false && (aline = bin.readLine()) != null) {

				if (aline.startsWith(">")) {
					log.debug ("New doc "+aline);
					nbDocs++;
					int nkeys = 0;
					if(!firstdoc) {
					  doc.addField("file", docStartbyte+"-"+nbytesread);	
					  try {
						finishMainRecord(doc,id,doStore,content);
					  } catch (SolrServerException e) {
						log.error(e.getMessage());
						nbParsingErrors++;
					  }
					  content="";
					  nbytesread = 0;
					  docStartbyte = lineStartbyte;
					}
					firstdoc = false;
					
					doc = new SolrInputDocument();
				    doc.addField("stream_content_type", "biosequence/fasta");
				    if(streamName!=null) {
				    doc.addField("stream_name", streamName);
				    }
				    doc.addField("bank", bank);


					if (fancbi != null
							&& (ma = fancbi.matcher(aline)).lookingAt()) {
						int nm = ma.groupCount();
						id = ma.group(nm-1); // ? best is last ?
						doc.addField("accession", id);
						log.info("New fasta doc: "+id);
						// addIndexField("name",id); //? or is it ma.group(0) -
						// all of x|y|z ?
						for (int i = nm - 2; i > 1; i--) {
							String aid = ma.group(i);
							if (digits.matcher(aid).find())
								doc.addField("accession", aid);
						}
						String descr = aline.substring(ma.end()).trim();
						if (descr.length() > 0)
							doc.addField("descr", descr);
						nids++;
					}

					else {
						int ide = 1;
						if ((ma = faid.matcher(aline)).lookingAt()) {
							id = ma.group(1);
							ide = ma.end(1);
							doc.addField("accession", id); // also id
						} else { // ? error
							id = "local" + nids;
							doc.addField("accession", id);
						}
						// addIndexField("name",id); //? dont need to duplicate
						// data?
						nids++;

						ma = faval.matcher(aline.substring(ide));
						while (ma.find()) {
							String key = "descr"; // other?
							String val = ma.group(1);
							Matcher mkv = fakv.matcher(val);
							if (mkv.matches() && mkv.groupCount() > 1) {
								key = mkv.group(1);
								val = mkv.group(2);
							}
							doc.addField(key, val);
							nkeys++;
						}
					}

				
				}

				else {
					// Fasta content
					if(doStore!=null && doStore.equals("true")) {
						content+= aline;
					}
				}
				nbytesread += aline.length() + lineendsize;
				lineStartbyte += aline.length() + lineendsize;
			}
		} catch (IOException e1) {
			log.error(e1.getMessage());
		}
		log.debug("file: "+docStartbyte+"-"+nbytesread);
		doc.addField("file", docStartbyte+"-"+nbytesread);
		try {
			finishMainRecord(doc,id,doStore,content);
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		} catch (IOException e) {
			log.error(e.getMessage());
			nbParsingErrors++;
		}
		
		ended = true;
		log.info("Number of documents indexed: "+nbDocs);
	}

	/**
	 * Adds id field and send doc to index server. If store property is set, storage it on storage backend
	 * @param doc Current index document
	 * @param curID id of the document
	 * @param doStore true or false, to store fasta content in backend storage
	 * @param content Data to be stored
	 * @throws SolrServerException
	 * @throws IOException 
	 * @throws IOException
	 */
	private void finishMainRecord(SolrInputDocument doc, String curID,String doStore,String content) throws SolrServerException, IOException {
		
		if (curID != null) {
				log.debug("# Add id: " + curID);
				doc.addField("id", curID);
				}
		
		indexManager.getServer().add(doc);
		
		if(doStore!=null && doStore.equals("true") && curID!=null) {
			
			StorageObject object = new StorageObject();
			object.setId(curID);
			object.setContent(content);
			try {
				storageInterface.store(object);
			} catch (StorageException e) {
				log.error(e.getMessage());
				nbParsingErrors++;
			}	
	    }
		
	}

	

}
