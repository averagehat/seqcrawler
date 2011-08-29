package org.irisa.genouest.seqcrawler.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ToolsTest extends TestCase{

	private Logger log = LoggerFactory.getLogger(ToolsTest.class);
	
	   /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ToolsTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ToolsTest.class );
    }
    
    public void testGetSize()
    {
    try {
    	long value = 5;
		assertEquals(Tools.getSize("5"),5);
		value = 5 * 1000;
		assertEquals(Tools.getSize("5k"),value);
		assertEquals(Tools.getSize("5K"),value);
		value = 5 * 1000 * 1000;
		assertEquals(Tools.getSize("5m"),value);
		
		value = 5000000000L;
		assertEquals(Tools.getSize("5g"),value);

	} catch (Exception e) {
		log.error(e.getMessage());
		fail();
	}	
    }
	
}
