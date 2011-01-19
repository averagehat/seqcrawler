package org.irisa.genouest.tools.readseq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.sax.XHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BioseqParser {
	private Logger log = LoggerFactory.getLogger(BioseqParser.class);

	XHTMLContentHandler contentHandler = null;

	public XHTMLContentHandler getContentHandler() {
		return contentHandler;
	}

	public void setContentHandler(XHTMLContentHandler contentHandler) {
		this.contentHandler = contentHandler;
	}

	ArrayList keyValues = null;

	public ArrayList getKeyValues() {
		return keyValues;
	}

	public void setKeyValues(ArrayList keyValues) {
		this.keyValues = keyValues;
	}

	public BioseqParser() {
	}

	protected boolean indexdata = false;
	int ndone = 0;
	long mainrecStartbyte = 0;
	long nbytesread = 0;
	long subrecStartbyte = 0;
	long lineStartbyte = 0;
	boolean ended = false;

	protected void finishMainRecord(String curID) {
		try {
			if (curID != null) {
				log.debug("# Add id: " + curID);
				if (contentHandler == null) {
					keyValues.add(new String[] { "id", curID });
				} else {
					contentHandler.element("id", curID);
				}
			}
		} catch (SAXException e) {

			e.printStackTrace();
		}

		mainrecStartbyte = nbytesread;
		subrecStartbyte = nbytesread;

	}

	void addIndexField(String key, String val) {
		if (val.length() == 0)
			return;
		try {
			log.debug("##add field: " + key.toLowerCase() + " :: " + val);
			if (contentHandler == null) {
				keyValues.add(new String[] { key.toLowerCase(), val });
			} else {
				contentHandler.element(key.toLowerCase(), val);
			}
		} catch (SAXException e) {
			log.error(e.getMessage());
		}
	}

	void parseFasta(Reader in) {
		lineStartbyte = 0;
		nbytesread = 0;
		int lineendsize = 1;

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

		String aline, id = null;
		Matcher ma;
		int nids = 0;
		try {
			while (ended == false && (aline = bin.readLine()) != null) {

				if (aline.startsWith(">")) {
					int nkeys = 0;
					finishMainRecord(id);

					try {
						if (contentHandler != null) {
							contentHandler.startDocument();
						} 
					} catch (SAXException e) {
						e.printStackTrace();
						return;
					}
					// ? if (indexall)
					// addIndexField("contents",aline.substring(1));

					if (fancbi != null
							&& (ma = fancbi.matcher(aline)).lookingAt()) {
						int nm = ma.groupCount();
						id = ma.group(nm); // ? best is last ?
						addIndexField("accession", id);
						// addIndexField("name",id); //? or is it ma.group(0) -
						// all of x|y|z ?
						for (int i = nm - 1; i > 0; i--) {
							String aid = ma.group(i);
							if (digits.matcher(aid).find())
								addIndexField("accession", aid);
						}
						String descr = aline.substring(ma.end()).trim();
						if (descr.length() > 0)
							addIndexField("descr", descr);
						nids++;
					}

					else {
						int ide = 1;
						if ((ma = faid.matcher(aline)).lookingAt()) {
							id = ma.group(1);
							ide = ma.end(1);
							addIndexField("accession", id); // also id
						} else { // ? error
							id = "local" + nids;
							addIndexField("accession", id);
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
							addIndexField(key, val);
							nkeys++;
						}
					}

					// if (debug) logp.println( "parseFasta: n=" + nids + " id="
					// + id + " nkeys="+nkeys);
				}

				else {
					// ignore sequence?
					// optionally store full records in lucene index?
				}

				nbytesread += aline.length() + lineendsize;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		finishMainRecord(id);
		try {
			if(contentHandler!=null) {
				contentHandler.endDocument();
			}
			
		} catch (SAXException e) {
			e.printStackTrace();
		}
		ended = true;
	}

}