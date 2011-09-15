package org.irisa.genouest.seqcrawler.index;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.irisa.genouest.seqcrawler.index.Index;
import org.irisa.genouest.seqcrawler.index.Constants.STORAGEIMPL;
import org.irisa.genouest.seqcrawler.index.exceptions.StorageException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.irisa.genouest.seqcrawler.index.storage.StorageManagerInterface;
import org.irisa.genouest.seqcrawler.index.storage.StorageObject;
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
			index.index(new String[] {"-f","./solr/dataset/datatest/test.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
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
			index.index(new String[] {"-f","./solr/dataset/datatest/testError.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
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
			index.index(new String[] {"-f","./solr/dataset/datatest/uniprot.dat","-b","UniProt","-C","-sh","./solr/","-sd","./solr/data/","-t","readseq"});
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
	        log.debug("Original content 1 : "+docs.get(0).getFieldValue("stream_content_type")+","+docs.get(0).getFieldValue("stream_name")+","+docs.get(0).getFieldValue("file"));
	        log.debug("Original content 2 : "+docs.get(1).getFieldValue("stream_content_type")+","+docs.get(1).getFieldValue("stream_name")+","+docs.get(1).getFieldValue("file"));
	        assertEquals("0-2368",docs.get(0).getFieldValue("file"));
	        assertEquals("2368-2367",docs.get(1).getFieldValue("file"));
    }

    
    public void testIndexFasta()
    {
    	try {
    	    Index index = new Index();
			index.index(new String[] {"-f","./solr/dataset/datatest/test.fasta","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/","-t","fasta"});
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
		// Look for id AF064181
		SolrDocumentList docs = execQuery("id:AF064181");
		assertTrue(docs.size()==1);
		log.debug(docs.get(0).toString());
		assertEquals(docs.get(0).getFieldValue("stream_content_type"),"biosequence/fasta");
    }

    public void testIndexFastaWithStorage() throws StorageException
    {
    	String host = System.getProperty("storageHost");
    	 
    	try {
    	    Index index = new Index();
    	   
    	    // If not set, do not run the test
    	    if(host==null) return;
    	    log.info("Using host: "+host);
			index.index(new String[] {"-f","./solr/dataset/datatest/test.fasta","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/","-t","fasta","-store","-stHost",host,"-storage","mock"});
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

		StorageManager storageMngr = new StorageManager();

			 HashMap<String,String> map = new HashMap<String,String>();
			 map.put("host", host);
			 map.put("max", "10");
			 storageMngr.setArgs(map);
			 log.info("Using host "+host);
		 
		StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.MOCK);

		
		try {
			StorageObject storedObject = storage.get("AF064181");
			log.debug(storedObject.getContent());
			assertTrue(storedObject.getContent().startsWith("acgcgggggggggggggg"));
		} catch (StorageException e) {
			log.error(e.getMessage());
			fail();
		}
		finally {
			storage.deleteAll("AF064181");
			storage.deleteAll("AF064185");
		}
		
    }
   
    
    public void testIndexGFFFastaWithStorage() throws StorageException
    {
    	String host = System.getProperty("storageHost");
    	 
    	try {
    	    Index index = new Index();
    	   
    	    // If not set, do not run the test
    	    if(host==null) return;
    	    log.error("Using host: "+host);
			index.index(new String[] {"-f","./solr/dataset/datatest/test.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/","-t","gff","-store","-stHost",host,"-storage","mock"});
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

		StorageManager storageMngr = new StorageManager();

			 HashMap<String,String> map = new HashMap<String,String>();
			 map.put("host", host);
			 storageMngr.setArgs(map);
			 log.info("Using host "+host);
		 
		StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.MOCK);

		
		try {
			StorageObject storedObject = storage.get("NC_002745");
			log.debug(storedObject.getContent());
			assertTrue(storedObject.getContent().startsWith("aaaaaaaaagggggggggggggggcccccc"));
		} catch (StorageException e) {
			log.error(e.getMessage());
			fail();
		}
		finally {
			storage.deleteAll("NC_002745");
		}
		
    }
    
    
    public void testMerge() {
    	Merge app = new Merge();
    	app.setIndexesDir("./solr");
    	app.setFinalDir("./solr/dataset/index");
    	app.setDEBUG(true);
    	app.setIncludeConditions(new String[] {"data"});
    	app.merge();
    	assertTrue(app.getIndexes().length>0);
    	app.setIncludeConditions(null);
    	app.setExcludeConditions(new String[] {"datamerge"});
    	app.merge();
    	assertTrue(app.getIndexes().length>0);
    	app.setExcludeConditions(null);
    	app.setIncludeConditions(null);
    	assertTrue(app.getIndexes().length>0);
    	app.merge();
    	app.setExcludeConditions(new String[] {".*"});
    	app.merge();
    	assertTrue(app.getIndexes().length==0);
    }
    
    
    public void testIndexEMBL()
    {
    	try {
    		Index index = new Index();
			index.index(new String[] {"-f","./solr/dataset/datatest/uniprot.dat","-b","UniProt","-C","-sh","./solr/","-sd","./solr/data/","-t","embl"});
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
	        log.debug("AC: "+docs.get(0).getFieldValue("ac").toString());
	        log.info("Original content 1 : "+docs.get(0).getFieldValue("stream_content_type")+","+docs.get(0).getFieldValue("stream_name")+","+docs.get(0).getFieldValue("file"));
	        log.info("Original content 2 : "+docs.get(1).getFieldValue("stream_content_type")+","+docs.get(1).getFieldValue("stream_name")+","+docs.get(1).getFieldValue("file"));
	        assertEquals("0-2368",docs.get(0).getFieldValue("file"));
	        assertEquals("2368-2368",docs.get(1).getFieldValue("file"));
    }
    
    public void testRecoder()
    {
    	try {
    	    Index index = new Index();
    	    index.setPROPFILE("./solr/bin/seqcrawler.properties");
			index.index(new String[] {"-f","./solr/dataset/datatest/testrecode.gff","-b","recode","-C","-sh","./solr/","-sd","./solr/data/"});
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
		SolrDocumentList docs = execQuery("sampletest:sampletest");
        assertTrue( docs.size()>0 );
    }
    
    
    public void testIndexPDB()
    {
    	try {
    	    Index index = new Index();
			index.index(new String[] {"-f","./solr/dataset/datatest/test.pdb","-b","PDB","-C","-sh","./solr/","-sd","./solr/data/","-t","pdb"});
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
		SolrDocumentList docs = execQuery("id:1ZZ0");
        assertTrue( docs.size()>0 );
		docs = execQuery("expdta:(+diffraction)");
        assertTrue( docs.size()>0 );
        docs = execQuery("title:(+crystal)");
        assertTrue( docs.size()>0 );       
    }
    
    
    
    public void testIndexJS() throws StorageException
    {
    	String host = System.getProperty("storageHost");
    	 
    	try {
    	    Index index = new Index();
    	   
    	    // If not set, do not run the test
    	    if(host==null) return;
    	    log.info("Using host: "+host);
			index.index(new String[] {"-f","./solr/dataset/datatest/test.fasta","-b","Scripting","-C","-sh","./solr/","-sd","./solr/data/","-t","test","-store","-stHost",host,"-storage","mock"});

			SolrDocumentList docs = execQuery("key1:test1");
			for(int i=0;i<docs.size();i++) {
				log.info("RES: "+docs.get(i).toString());
			}
			assertTrue(docs.size()==1);
			
			StorageManager storageMngr = new StorageManager();

			 HashMap<String,String> map = new HashMap<String,String>();
			 map.put("host", host);
			 map.put("max", "10");
			 storageMngr.setArgs(map);
			 log.info("Using host "+host);
		 
			 StorageManagerInterface storage = storageMngr.get(STORAGEIMPL.MOCK);
			 StorageObject storedObject = storage.get("1");
			 log.info(storedObject.getContent());
			 assertTrue(storedObject.getContent().startsWith("acgt"));
    	
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
		
    }
    
}
