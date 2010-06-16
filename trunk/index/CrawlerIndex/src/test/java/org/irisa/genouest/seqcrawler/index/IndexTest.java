package org.irisa.genouest.seqcrawler.index;

import java.io.File;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.ParseException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.irisa.genouest.seqcrawler.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private Logger log = LoggerFactory.getLogger(IndexTest.class);
	
	private SolrDocumentList execQuery(String query) {
		IndexManager indexMngr = new IndexManager();
		SolrDocumentList docs=null;
		try {
			indexMngr.initServer(null);
	        docs = indexMngr.queryServer(query);	        
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
		} finally {
			indexMngr.shutdownServer();
		}
		return docs;
	}
	
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
    	    Index index = new Index();
			index.index(new String[] {"-f","./solr/test.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
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
		// Look for chr NC_002745
		SolrDocumentList docs = execQuery("chr:NC_002745");
        assertTrue( docs.size()>0 );
        assertEquals(docs.get(0).getFieldValue("chr"),"NC_002745");

    }
    
    
    public void testShardIndex()
    {
    	try {
			Index index = new Index();
			index.index(new String[] {"-f","./solr/dataset/data","-b","GenBank","-C","-sh","./solr/","-sd","./solr/dataset/index","-shard","2"});
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
    	Index index =null;
    	try {
			index = new Index();
			index.index(new String[] {"-f","./solr/testError.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
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
        assertTrue( index.getNbErrors() == 1 );
    }
    
   
    
    public void testIndexReadSeq()
    {
    	try {
    		Index index = new Index();
			index.index(new String[] {"-f","./solr/uniprot.dat","-b","UniProt","-C","-sh","./solr/","-sd","./solr/data/","-t","readseq"});
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
		
			SolrDocumentList docs  = execQuery("bank:UniProt");
	        assertTrue(docs.size()>=2);
	        log.info("Original content 1 : "+docs.get(0).getFieldValue("stream_content_type")+","+docs.get(0).getFieldValue("stream_name")+","+docs.get(0).getFieldValue("file"));
	        log.info("Original content 2 : "+docs.get(1).getFieldValue("stream_content_type")+","+docs.get(1).getFieldValue("stream_name")+","+docs.get(1).getFieldValue("file"));
	        assertEquals(docs.get(0).getFieldValue("file"),"0-2368");
	        assertEquals(docs.get(1).getFieldValue("file"),"2368-4735");
				
    }


    
}
