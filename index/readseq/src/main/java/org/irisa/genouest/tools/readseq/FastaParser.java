package org.irisa.genouest.tools.readseq;

import iubio.readseq.BioseqDoc;
import iubio.readseq.Readseq;
import iubio.readseq.SeqFileInfo;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Tika Fasta Parser implementation for format detection and to extract structured text content and metadata 
 * @author osallou
 *
 */
public class FastaParser implements Parser {

	private Logger log = LoggerFactory.getLogger(ReadSeqParser.class);
	
	
	public void parse(InputStream in, ContentHandler handler, Metadata metadata)
			throws IOException, SAXException, TikaException {
	
		metadata.add(Metadata.CONTENT_TYPE,"biosequence/fasta");
        XHTMLContentHandler storeDoc =
            new XHTMLContentHandler(handler, metadata);
        BioseqParser parser =new BioseqParser();
  	  	parser.setContentHandler(storeDoc);
  	  	parser.parseFasta(new InputStreamReader(in));
  	  	
			   

		
	}

}