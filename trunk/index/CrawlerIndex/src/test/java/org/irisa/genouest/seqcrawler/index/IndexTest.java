package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.irisa.genouest.seqcrawler.index.Index;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.xml.sax.SAXException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class IndexTest 
    extends TestCase
{
	
	private Log log = LogFactory.getLog(IndexTest.class);
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public IndexTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( IndexTest.class );
    }

    /**
     * Main app testing
     */
    
    public void testIndex()
    {
    	try {
			Index.main(new String[] {"-f","./solr/test.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
		} catch (IOException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
			fail();
		} catch (SAXException e) {
			log.error(e.getMessage());
			fail();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParseException e) {
			log.error(e.getMessage());
			fail();
		}
        assertTrue( true );

    }
    

    
    public void testShardIndex()
    {
    	try {
			Index.main(new String[] {"-f","./solr/dataset/data","-b","GenBank","-C","-sh","./solr/","-sd","./solr/dataset/index","-shard","2"});
		} catch (IOException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
			fail();
		} catch (SAXException e) {
			log.error(e.getMessage());
			fail();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParseException e) {
			log.error(e.getMessage());
			fail();
		} 
		// 6 input files, shard size = 2, we should have 3 shard indexes
		File f1 = new File("./solr/dataset/index/shard0/index");
        assertTrue( f1.exists() );
		File f2 = new File("./solr/dataset/index/shard1/index");
        assertTrue( f2.exists() );
		File f3 = new File("./solr/dataset/index/shard2/index");
        assertTrue( f3.exists() );
    }
    
    public void testIndexError()
    {
    	try {
			Index.main(new String[] {"-f","./solr/testError.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
		} catch (IOException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
			fail();
		} catch (SAXException e) {
			log.error(e.getMessage());
			fail();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParseException e) {
			log.error(e.getMessage());
			fail();
		}
        assertTrue( Index.getNbErrors() == 1 );
    }
    
   
    
    public void testIndexReadSeq()
    {
    	try {
			Index.main(new String[] {"-f","./solr/uniprot.dat","-b","UniProt","-C","-sh","./solr/","-sd","./solr/data/","-t","readseq"});
		} catch (IOException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParserConfigurationException e) {
			log.error(e.getMessage());
			fail();
		} catch (SAXException e) {
			log.error(e.getMessage());
			fail();
		} catch (SolrServerException e) {
			log.error(e.getMessage());
			fail();
		} catch (ParseException e) {
			log.error(e.getMessage());
			fail();
		}
		
        assertTrue( true );
    }
    
}
