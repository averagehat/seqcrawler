package org.irisa.genouest.seqcrawler.index;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.irisa.genouest.seqcrawler.index.App;
import org.xml.sax.SAXException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
	
	private Log log = LogFactory.getLog(AppTest.class);
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	try {
			App.main(new String[] {"-f","./solr/test.gff","-b","GenBank","-C","-sh","./solr/","-sd","./solr/data/"});
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
