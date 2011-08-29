package org.irisa.genouest.seqcrawler.CrawlerSearch;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
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
     * @throws SolrServerException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws ParseException 
     * @throws IOException 
     */
    public void testExport() throws IOException, ParseException, ParserConfigurationException, SAXException, SolrServerException
    {
        Export.main(new String[] { "-f","outexport.xml","-query","protein","-ranges","0-10"});
        File f = new File("outexport.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);
        NodeList nodeLst = doc.getElementsByTagName("doc");
        assertEquals(nodeLst.getLength(),2);
        
    }
    
    public void testExportLowerRange() throws IOException, ParseException, ParserConfigurationException, SAXException, SolrServerException
    {
        Export.main(new String[] { "-f","outexport.xml","-query","protein","-ranges","1-1"});
        File f = new File("outexport.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);
        NodeList nodeLst = doc.getElementsByTagName("doc");
        assertEquals(nodeLst.getLength(),1);
        
    }
}
