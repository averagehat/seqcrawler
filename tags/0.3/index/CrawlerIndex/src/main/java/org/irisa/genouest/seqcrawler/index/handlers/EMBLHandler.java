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
import org.irisa.genouest.seqcrawler.index.handlers.field.FieldRecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>EMBLIndexer</b>
 * 
 * <p>
 * Optimized for EMBL-style Biosequence file indexing (including SwissProt,
 * UniProt) , separated from Readseq indexer
 * </p>
 * 
 * 
 * <p>
 * ------ EMBL File Format -----------
 * </p>
 * <p>
 * -- summary of main format items
 * </p>
 * 
 * <p>
 * ID XLRHODOP standard; RNA; VRT; 1684 BP.
 * </p>
 * <p>
 * ^^^^^^^^^^^^^ start of record, record id and summary data
 * </p>
 * <p>
 * AC L07770;
 * </p>
 * <p>
 * DE Xenopus laevis rhodopsin mRNA, complete cds.
 * </p>
 * <p>
 * FH Key Location/Qualifiers
 * </p>
 * <p>
 * FT source 1..1684
 * </p>
 * <p>
 * 123456789012345678901234567890
 * </p>
 * <p>
 * ^ feature key ^ feature values
 * </p>
 * <p>
 * FT /db_xref="taxon:8355"
 * </p>
 * <p>
 * FT /organism="Xenopus laevis"
 * </p>
 * <p>
 * FT CDS 110..1174
 * </p>
 * <p>
 * FT /codon_start=1
 * </p>
 * <p>
 * FT /db_xref="SWISS-PROT:P29403"
 * </p>
 * <p>
 * FT misc_feature 189..1684
 * </p>
 * <p>
 * SQ Sequence 1684 BP; 426 A; 431 C; 339 G; 488 T; 0 other;
 * </p>
 * <p>
 * ggtagaacag cttcagttgg gatcacaggc ttctagggat cctttgggca aaaaagaaac 60
 * </p>
 * <p>
 * acagaaggca ttctttctat acaagaaagg actttataga gctgctacca tgaacggaac 120
 * </p>
 * <p>
 * agaaggtcca aatttttatg tccccatgtc caacaaaact ggggtggtac gaagcccatt 180
 * </p>
 * <p>
 * ...
 * </p>
 * <p>
 * // -- end of record
 * </p>
 * <p>
 * ID XL23808 standard; DNA; VRT; 4734 BP.
 * </p>
 * <p>
 * AC U23808;
 * </p>
 * <p>
 * DE Xenopus laevis rhodopsin gene, complete cds.
 * </p>
 * <p>
 * SQ Sequence 4734 BP; 1315 A; 1046 C; 985 G; 1388 T; 0 other;
 * </p>
 * <p>
 * cgtaactagg accccaggtc gacacgacac cttccctttc ccagttattt cccctgtaga 60
 * </p>
 * <p>
 * cgttagaagg ggaaggggtg tacttatgtc acgacgaact acgtccttga ctacttaggg 120
 * </p>
 * <p>
 * ...
 * </p>
 * <p>
 * // -- end of record
 * </p>
 * <p>
 * Based on work from Lucegene by don gilbert
 * </p>
 * 
 * @author osallou
 * 
 */
public class EMBLHandler implements SequenceHandler {

	/**
	 * Available parsing status
	 *
	 */
	private enum STATES {
		PARSE_COMMENT, PARSE_CONTINUEVALUE, PARSE_STARTREC, PARSE_ENDREC, PARSE_ENDDOC, PARSE_STARTDOC, PARSE_KEYVALUE
	}

	long nbErrors = 0;
	IndexManager indexMngr = null;

	private Logger log = LoggerFactory.getLogger(EMBLHandler.class);

	public final static String xpathDelim=".";
	
	protected String featureTableKey = "FT";
	protected String emblid = null;
	protected boolean indexdata = false;
	protected String[] idkeys;

	private StringBuffer valbuf = new StringBuffer();

	private String streamName = null;

	private String bank = Constants.BANK_DEFAULT;

	long lineStartbyte = 0;
	long docStartbyte = 0;
	long nbytesread = 0;
	
	int nbDocstoCommit=0;

	String currentFieldName = null;
	
	int nbDocs = 0;

	SolrInputDocument doc = null;

	public EMBLHandler() {
		String v = "docclass; molecule; sequencelength";
		idkeys = v.split("\\s*[,;\\s]\\s*");
		log.debug("idkeys: "+idkeys.toString());
	}

	public EMBLHandler(String lib) {
		this();
		if (lib != null) {
			this.bank = lib;
		}
	}

	public long getNbParsingErrors() {
		return nbErrors;
	}

	public void parse(File f) throws IOException, IndexException {
		streamName = f.getAbsoluteFile().toString();
		BufferedReader bf = new BufferedReader(new FileReader(f));
		this.parse(bf);
		bf.close();

	}

	public void parse(BufferedReader bf) throws IOException, IndexException {
		int lineendsize = 1;
		boolean ended = false;
		long stime = System.currentTimeMillis();
		boolean lastDocClosed = false;

		String aline = null;

		// Manage first doc in file
		processField(null,null,STATES.PARSE_STARTDOC);

		while (ended == false && (aline = bf.readLine()) != null) {
			nbytesread += lineendsize + aline.length();
			
			int len = aline.length();
			if (len > 1) {
				int at = 2;
				String akey = aline.substring(0, at);
				while (at < len && aline.charAt(at) == ' ')
					at++;
				String aval = (at >= len) ? null : aline.substring(at);

				if (akey.equals("//")) { // EMBL-EOR
					processField(akey, aval, STATES.PARSE_ENDREC);
					lastDocClosed = true;
					emblid = null;
				}

				else if (akey.equals(featureTableKey)) {

					if (at > 20)
						processField(null, aval, STATES.PARSE_CONTINUEVALUE);

					else {
						int at2 = aval.indexOf(' ');
						int len2 = aval.length();
						String akey2 = akey + xpathDelim
								+ aval.substring(0, at2); // ? do this or drop
															// FT. ?
						while (at2 < len2 && aval.charAt(at2) == ' ')
							at2++;
						String aval2 = (at2 >= len2) ? null : aval
								.substring(at2);
						processField(akey2, aval2, STATES.PARSE_KEYVALUE);
					}
				}
				else if (akey.equals("  ")) { // only for seq data , skip 												
					processField("sequence", aval, STATES.PARSE_COMMENT);
				}
				else if ("ID".equals(akey)) {
					// special case this ?? -- is always PARSE_STARTREC
					// ID ENTRY_NAME DATA_CLASS; MOLECULE_TYPE; SEQUENCE_LENGTH.
					// == swissprot
					// ID entryname dataclass; molecule; division;
					// sequencelength BP. == embl
					// ID GRAA_HUMAN STANDARD; PRT; 262 AA.
					// ID XLRHODOP standard; RNA; VRT; 1684 BP.

					int e = aval.indexOf(' ');
					if (e <= 0) {
						e = aval.length();
					}
					emblid = aval.substring(0, e);
					processField("id", emblid, STATES.PARSE_STARTDOC);
					lastDocClosed = false;
					log.info("Processing new document with id: "+emblid);
					// split out other parts of ID line
					String[] flds = aval.substring(e).trim().split("\\s*[;]\\s*");
					if(flds!=null) {
					String[] fkeys = idkeys; // get from props
					for (int i = 0; i < flds.length; i++) {
						String fkey = (i < fkeys.length) ? fkeys[i] : "fld" + i;
						processField(fkey, flds[i], STATES.PARSE_KEYVALUE);
					}
					}

					// processField( akey, aval, PARSE_STARTDOC); //?
					// PARSE_KEYVALUE
				}
				else if("CC".equalsIgnoreCase(akey)) {
					//Comment, ignore
					processField(akey, aval, STATES.PARSE_COMMENT);
				}
				else {
					processField(akey, aval, STATES.PARSE_KEYVALUE);
				}

			}
			lineStartbyte += aline.length() + lineendsize;
		}

		ended = true;
		if(lastDocClosed == false) {
		processField(null, null, STATES.PARSE_ENDDOC); // finish any record (end of file with no end mapping)
		}

		long elapsed = System.currentTimeMillis() - stime;
		log.info("Number of documents indexed: "+nbDocs);
		log.debug("parse time (ms): " + elapsed);
	}


	public void setIndexManager(IndexManager manager) {
		indexMngr = manager;

	}

	/**
	 * Add document to index server, parsing of current document is over
	 * @throws IOException
	 * @throws SolrServerException
	 */
	protected void finishMainRecord() throws IOException, SolrServerException {
		// Store document
		log.debug("end of document, storing it");
		finishfield("file", new StringBuffer(docStartbyte+"-"+nbytesread));
		nbDocs++;
		nbDocstoCommit++;
		indexMngr.filterDoc(doc);
		indexMngr.getServer().add(doc);
		if(nbDocstoCommit>=Constants.MAXCOMMITS) {
			indexMngr.getServer().commit();
			nbDocstoCommit=0;
		}
	}

	/**
	 * End concatenation of field, store it as a dopcument field
	 * @param key Name of the field
	 * @param value Value of the field as a string.
	 */
	private void finishfield(String key, StringBuffer value) {
		//Multiple values are not accepted, check if present. If present concat field
		key = key.toLowerCase();
		
    	String recodeKey = bank+"."+key+".recode";
    	if(indexMngr.getArgs().containsKey(recodeKey)) {
    		recodeField(key,value.toString());
    	}
		else {
		if(doc.containsKey(key)) {
			String tmpVal = (String)(doc.removeField(key)).getValue();
			doc.addField(key,tmpVal+" "+value.toString());
		}
		else {
			doc.addField(key, value.toString());
		}
		}
		value.setLength(0);
		
	}
	
	
	/**
	 * Recode input field according to configured recoder
	 * @param key field name
	 * @param value field value
	 */
	private void recodeField(String key, String value) {
    	    String recodeKey = bank+"."+key+".recode";
    		String className = indexMngr.getArgs().get(recodeKey);
    		try {
				Class recodeClass = Class.forName(className);
				FieldRecoder recoder = (FieldRecoder) recodeClass.newInstance();
				String[][] newAttributes = recoder.recode(key, value);
				if(newAttributes!=null) {
				for(int na = 0; na < newAttributes.length; na++) {
					if(doc.containsKey(newAttributes[na][0])) {
						String tmpVal = (String)(doc.removeField(newAttributes[na][0])).getValue();
						doc.addField(newAttributes[na][0],tmpVal+" "+newAttributes[na][1]);
					}
					else {
						doc.addField(newAttributes[na][0], newAttributes[na][1]);
					}
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

	/**
	 * Manage input field depending on current document parsing status. May store document, add field, skip field...
	 * @param key Name of field, null for documents
	 * @param val Value of the field, null for documents
	 * @param state  status of the parsing
	 * @throws IOException
	 */
	protected void processField(String key, String val,	STATES state) throws IOException {
		log.debug("process field:  "+key+" = "+val+" in state "+state.toString());
		if (state == STATES.PARSE_COMMENT) {
			return;
		} else if (state == STATES.PARSE_CONTINUEVALUE) { // also
															// key.equals(currentFieldName)
			valbuf.append("\n").append(val);
			return;
		}

		if ((currentFieldName!=null && !currentFieldName.equals(key)) || state == STATES.PARSE_ENDREC
				|| state == STATES.PARSE_ENDDOC)
			finishfield(currentFieldName, valbuf);

		if (state == STATES.PARSE_ENDREC || state == STATES.PARSE_ENDDOC) {
			try {
				finishMainRecord();
				nbytesread=0;
			} catch (SolrServerException e) {
				nbErrors++;
				log.error(e.getMessage());
			}
		}

		else if (state == STATES.PARSE_STARTREC
				|| state == STATES.PARSE_STARTDOC) {
			docStartbyte = lineStartbyte;
			log.debug("new document");
			doc = new SolrInputDocument();
			doc.addField("stream_content_type", "biosequence/embl");
			if(streamName!=null) {
			doc.addField("stream_name", streamName);
			}
			doc.addField("bank", bank);
		}


		if (key != null)
			currentFieldName = key;
		if (val != null)
			valbuf.append(val);

	}

	
}
