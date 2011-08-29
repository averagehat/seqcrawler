package org.irisa.genouest.seqcrawler.index.utils;

import java.io.IOException;

import org.irisa.genouest.seqcrawler.index.IndexTest;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UtilsTest extends TestCase {

	private Logger log = LoggerFactory.getLogger(UtilsTest.class);
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UtilsTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( UtilsTest.class );
    }
    
    public void testListFields()
    {
    	Utils utils = new Utils();
    	utils.setSolrHome("./solr/");
    	utils.setSolrData(new String[] { "./solr/data/" } );
    	utils.init();
    	String[] list = utils.listFields();
    	boolean matched=false;
    	for(int i=0;i<list.length;i++) {
    		if(list[i].equalsIgnoreCase("dbxref")) {
    			matched = true;
    		}
    	}
    	assertTrue(matched);
    }
}
