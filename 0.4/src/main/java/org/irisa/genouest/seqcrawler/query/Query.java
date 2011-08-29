package org.irisa.genouest.seqcrawler.query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.Index;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class Query {

	private Logger log = LoggerFactory.getLogger(Query.class);
	
	String query=null;
	
	
	static boolean DEBUG=false;
	

	
	String solrHome = Constants.SOLRHOME;
	String solrData = Constants.SOLRDATA;
	String solrUrl ="http://localhost/solr";
	boolean useEmbeddedServer = true;
	
	
	/**
	 * Main class to send a query to an index, outputs results to logger
	 * @param args See -h option for usage
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws SolrServerException 
	 */
	public static void main(String[] args) throws ParseException, IOException, ParserConfigurationException, SAXException, SolrServerException {
		    Query application = new Query();
		    application.init();
	        Options options = new Options();
	        options.addOption("sh", true, "solr home path");
	        options.addOption("sd", true, "solr data index path");
	        options.addOption("debug", false, "for debug");
	        options.addOption("h", false, "show usage");
	        options.addOption("v", false, "show version");
	        options.addOption("q", false, "query to be sent");
	        options.addOption("url", true, "url of Solr server");

	        CommandLineParser parser = new PosixParser();
	        CommandLine cmd = parser.parse( options, args);
	        
	        IndexManager indexMngr = null;
	        
	        if(cmd.hasOption("v")) {
	        	Class<Index> clazz = Index.class; 
	        	String classContainer = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
	        	URL manifestUrl = new URL("jar:" + classContainer + "!/META-INF/MANIFEST.MF");
	        	Manifest mf = new Manifest(manifestUrl.openStream());
	        	Attributes atts = mf.getMainAttributes();
	        	
	        	System.out.println("SeqCrawler indexer version: "+atts.getValue("Implementation-Version")+"-"+atts.getValue("Implementation-Build"));        	
	        	application.log.info("Version: " + atts.getValue("Implementation-Version"));
	        	application.log.info("Build: " + atts.getValue("Implementation-Build"));
	        	
	        	System.exit(0);
	        }
	        
	        if(cmd.hasOption("h")) {
	        	showUsage(options);
	        	System.exit(0);
	        }
	        
	        if(cmd.hasOption("q")) {
	        	application.query = cmd.getOptionValue("q");
	        }
	        
	        if(cmd.hasOption("debug")) {
	        	DEBUG=true;
	        }
	        
	        if(cmd.hasOption("sh")) {
	        	application.solrHome = cmd.getOptionValue("sh");
	        }
	        
	        if(cmd.hasOption("url")) {
	        	application.solrUrl = cmd.getOptionValue("url");
	        	application.useEmbeddedServer = false;
	        }
	        
	        if(cmd.hasOption("sd")) {
	        	application.solrData = cmd.getOptionValue("sd");
	        }              
	        	
	       
	        application.log.info("Starting application");
	        // Note that the following property could be set through JVM level arguments too
	        
	        System.setProperty("solr.solr.home", application.solrHome);
	        System.setProperty("solr.data.dir", application.solrData);        
	        
	        indexMngr = new IndexManager();
	        
	        if(application.useEmbeddedServer) {
	        	indexMngr.initServer(null);
	        }
	        else {
	        	indexMngr.initServer(application.solrUrl);
	        }
	        
	        application.log.info("Query server with : "+application.query);
	        
	        SolrQuery solrQuery = new SolrQuery();
	        solrQuery.setQuery(application.query);
	        QueryResponse rsp = indexMngr.getServer().query(solrQuery);
	        SolrDocumentList docs = rsp.getResults();
	        for(int i=0;i<docs.size();i++) {
	        	application.log.info("Document "+i);
	        	for(Entry<String, Object> doc : docs.get(i).entrySet()) {
	        		application.log.info(doc.getKey()+" : "+doc.getValue().toString());
	        	}
	        }
	        
	        indexMngr.shutdownServer();

	}

	
    /**
     * Prints to console the command line usage
     * @param options Options supported
     */
	private static void showUsage(Options options) {
		 HelpFormatter formatter = new HelpFormatter();
		 formatter.printHelp( "OptionsTip", options );

	}
	
	/**
	 * Gives the debug status of the application
	 * @return true or false
	 */
	public static boolean debug() {
		return DEBUG;
	}
	
	/**
	 * Initialize properties if a property file is present in current path.
	 */
	private void init() {
		// Read properties file. 
		Properties properties = new Properties();
		try {
			File props = new File("seqcrawler.properties");
			if(props.exists()) {
			properties.load(new FileInputStream(props)); }
			if(properties.containsKey("solr.solr.home")) {
				solrHome = properties.getProperty("solr.solr.home");
				log.info("Using solr home "+solrHome+" from properties");
			}
			if(properties.containsKey("solr.data.dir")) {
				solrData = properties.getProperty("solr.data.dir");
				log.info("Using solr data dir "+solrData+" from properties");
			}
		}
		catch (IOException e) { 
			log.error(e.getMessage());
		} 
	}
	
}
