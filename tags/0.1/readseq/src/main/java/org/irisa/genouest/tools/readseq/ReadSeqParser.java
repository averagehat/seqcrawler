package org.irisa.genouest.tools.readseq;


import iubio.readseq.BioseqDoc;
import iubio.readseq.Readseq;
import iubio.readseq.SeqFileInfo;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tika Parser implementation for format detection and to extract structured text content and metadata 
 * @author osallou
 *
 */
public class ReadSeqParser implements Parser {

	private Logger log = LoggerFactory.getLogger(ReadSeqParser.class);
	
	
	public void parse(InputStream in, ContentHandler handler, Metadata metadata)
			throws IOException, SAXException, TikaException {
	
		XHTMLContentHandler storeDoc =
            new XHTMLContentHandler(handler, metadata);
		
			    Readseq rd= new Readseq();
			    rd.setInputObject( in );
			    
		    	BioseqParser parser =new BioseqParser();
		    	parser.setContentHandler(storeDoc);
			    
			    
			    CharArrayWriter outwr= new CharArrayWriter(); 
			    SeqDocToIndexDoc sdt= new SeqDocToIndexDoc(); 
			    if ( rd.isKnownFormat() && rd.readInit() )  {
			    metadata.add(Metadata.CONTENT_TYPE,rd.getBioseqFormat().contentType());
			    storeDoc.startDocument();
			    while (rd.readNext()) {
			    	  

			        SeqFileInfo sfi= rd.nextSeq();
			          //? want also seq info - index in file
			        Object seqdoc= sfi.getdocObject();
			        
			        if (seqdoc instanceof BioseqDoc) {			   			         
			          sdt.setSourceDoc((BioseqDoc)seqdoc); 
			          sdt.writeContentHandler(parser, outwr);
			          }
			        
			        }
			    outwr.reset();		          
		        parser.finishMainRecord(sdt.getID());
			    storeDoc.endDocument();
					  }
			    else {
			    	throw new TikaException("Cannot parse input stream");
			    }
		
	}

}
