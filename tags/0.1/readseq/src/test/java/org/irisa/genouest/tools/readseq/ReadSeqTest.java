package org.irisa.genouest.tools.readseq;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ReadSeqTest 
    extends TestCase
   
    {    
    private Log log = LogFactory.getLog(ReadSeqTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ReadSeqTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ReadSeqTest.class );
    }

    /**
     * Rigourous Test :-)
     * @throws TikaException 
     * @throws SAXException 
     * @throws IOException 
     */
    public void testReadSeqEMBL() throws IOException, SAXException, TikaException
    {
    	File inputfile = new File("test/uniprot.dat");
    	if(!inputfile.exists()) fail("Test file test/uniprot.dat could not be found");
		InputStream input = new FileInputStream(inputfile);
		ContentHandler textHandler = new BodyContentHandler();
		Metadata metadata = new Metadata();
		ReadSeqParser parser = new ReadSeqParser();
		parser.parse(input, textHandler, metadata);
		input.close();
		log.info("content: " + textHandler.toString());
		log.info("content-type: "+metadata.get(Metadata.CONTENT_TYPE));
        assertEquals( "biosequence/embl" , metadata.get(Metadata.CONTENT_TYPE) );
        //assertTrue(textHandler.toString().startsWith("//MAP"));
    }
    
    public void testReadSeqFasta() throws IOException, SAXException, TikaException
    {
		String s = ">test\nacgtacgt\nacgtacgt\n";
		InputStream input = new ByteArrayInputStream(s.getBytes());
		ContentHandler textHandler = new BodyContentHandler();
		Metadata metadata = new Metadata();
		FastaParser parser = new FastaParser();
		parser.parse(input, textHandler, metadata);
		input.close();
		log.info("content: " + textHandler.toString());
		log.info("content-type: "+metadata.get(Metadata.CONTENT_TYPE));
        assertEquals( "biosequence/fasta" , metadata.get(Metadata.CONTENT_TYPE) );
        //assertTrue(textHandler.toString().startsWith("//MAP"));
    }
}
