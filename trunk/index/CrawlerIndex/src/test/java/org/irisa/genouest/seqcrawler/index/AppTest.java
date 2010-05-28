package org.irisa.genouest.seqcrawler.index;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
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
			App.main(new String[] {"-f","C:/NOSAVE/test.gff","-b","GenBank","-C","-sh","c:/NOSAVE/apache-solr-1.4.0/example/solr/","-sd","c:/NOSAVE/apache-solr-1.4.0/example/solr/data/"});
		} catch (IOException e) {
			fail();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (SAXException e) {
			fail();
		} catch (SolrServerException e) {
			fail();
		} catch (ParseException e) {
			fail();
		}
        assertTrue( true );
    }
}
