package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;


/**
 * Main indexing application. Use -h command line option to get usage.
 * @author osallou
 *
 */
public class App 
{
	private Log log = LogFactory.getLog(App.class);
	
	static String bank=Constants.BANK_DEFAULT;
	static String inputFile=null;
	
	static Constants.FORMATS format = Constants.FORMATS.GFF;
	
	static boolean DEBUG=false;
	
	static int shardsSize = 10;
	static boolean useShards = false;
	
	static String SOLRHOME = "/opt/solr/apache-solr-1.4.0/seqcrawler/solr";
	static String SOLRDATA = "/opt/solr/apache-solr-1.4.0/seqcrawler/solr/data/";
	static String SOLRURL ="http://localhost/solr";
	boolean useEmbeddedServer = true;
	
	static final String VERSION = "1.0";
	
	/**
	 * Application entry point. Index one or more document in a Solr server. Index can be done with an embedded server for local writing.
	 * @param args See usage (-h)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SolrServerException
	 * @throws ParseException
	 */
    public static void main( String[] args ) throws IOException, ParserConfigurationException, SAXException, SolrServerException, ParseException
    {
        App application = new App();
        Options options = new Options();
        options.addOption("c", false, "Clean index before with bank name");
        options.addOption("C", false, "Clean all index");
        options.addOption("b", true, "bank name");
        options.addOption("f", true, "input file or directory path");
        options.addOption("t", true, "format type of the input file (GFF is default)");
        options.addOption("sh", true, "solr home path");
        options.addOption("sd", true, "solr data index path");
        options.addOption("debug", false, "for debug, do not index");
        options.addOption("h", false, "show usage");
        options.addOption("v", false, "show version");
        options.addOption("o", false, "Optimize index");
        options.addOption("shard", true, "For directories, cut index in shards of specified size");
        
        options.addOption("url", true, "url of Solr server");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse( options, args);
        
        
        if(cmd.hasOption("v")) {
        	application.log.info("Current version is: "+VERSION);
        }
        
        if(cmd.hasOption("h")) {
        	showUsage(options);
        }
        
        if(cmd.hasOption("b")) {
        	bank = cmd.getOptionValue("b");
        }
        
        if(cmd.hasOption("shard")) {        
        	shardsSize = Integer.valueOf(cmd.getOptionValue("shard"));
        	useShards = true;
        }
        
        if(cmd.hasOption("debug")) {
        	DEBUG=true;
        }
        
        if(cmd.hasOption("sh")) {
        	SOLRHOME = cmd.getOptionValue("sh");
        }
        
        if(cmd.hasOption("url")) {
        	SOLRURL = cmd.getOptionValue("url");
        	application.useEmbeddedServer = false;
        }
        
        if(cmd.hasOption("sd")) {
        	SOLRDATA = cmd.getOptionValue("sd");
        }              
        	
       
        application.log.info("Starting application");
        // Note that the following property could be set through JVM level arguments too
        
        System.setProperty("solr.solr.home", SOLRHOME);
        System.setProperty("solr.data.dir", SOLRDATA);
        application.log.info("Using solr home: "+System.getProperty("solr.solr.home"));        
        CoreContainer.Initializer initializer = new CoreContainer.Initializer();
        CoreContainer coreContainer = initializer.initialize();
        
        if(application.useEmbeddedServer) {
        	 application.log.info("Using embedded server");
        	 EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "");
        	 IndexUtils.setServer(server);
        }
        else {
        	application.log.info("Using remote server "+SOLRURL);
        	SolrServer remoteserver = new CommonsHttpSolrServer( SOLRURL );
        	IndexUtils.setServer(remoteserver);
        }
        
        
        if(cmd.hasOption("c")) {
        	IndexUtils.getInstance().cleanIndexByBank(bank);
        }
        
        if(cmd.hasOption("C")) {
        	IndexUtils.getInstance().cleanIndex();
        }
        
        if(cmd.hasOption("f")) {
        	inputFile = cmd.getOptionValue("f");
        }
        else {
        	application.log.error("WARNING: Input file command line option is missing (-f) ");
        	application.log.error("There is no data to index ");
        }
        if(cmd.hasOption("t")) {
        	String t = cmd.getOptionValue("t");
        	if(t.equalsIgnoreCase("gff")) {
        		format = Constants.FORMATS.GFF;
        	}
        	else if(t.equalsIgnoreCase("gb")) {
        		format = Constants.FORMATS.GENBANK;
        	}
        	else if(t.equalsIgnoreCase("fasta")) {
        		format = Constants.FORMATS.FASTA;
        	}
        }
        application.log.info("Input file format is "+format.toString());
        
        File in = null;
        File[] files=null;
        
        if(inputFile!=null) {
        	
        in = new File(inputFile);        
        // If input is directory parse all files of the directory
        if(in.isDirectory()) {
        	files = in.listFiles();
        }
        else {
        // Parse only input file
        files = new File[] { new File(inputFile)};
        }
        
        application.log.info("Start indexation - "+new Date());
        
        int shardId = 0;
        boolean newShard=true;
        int countFile = 1;
        
        for(File file : files) {
        	if(useShards && countFile > shardsSize) {
        		// If shardsSize is reached, reset counter and start a new shard index
        		newShard = true;
        		countFile = 1;
        	}
        	if(useShards && newShard) {
        		// Create a new index for index shard with shard id.
        		coreContainer.shutdown();
        		System.setProperty("solr.solr.home", SOLRHOME);
                System.setProperty("solr.data.dir", SOLRDATA+"/shard"+shardId);
                application.log.info("Using shard: "+System.getProperty("solr.data.dir"));        
                coreContainer = initializer.initialize();
                
                if(application.useEmbeddedServer) {
                	 EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "");
                	 IndexUtils.setServer(server);
                }
                else {
                	SolrServer remoteserver = new CommonsHttpSolrServer( SOLRURL );
                	IndexUtils.setServer(remoteserver);
                }        		        		        		
        		shardId++;        		
        		newShard = false;
            }	
        SequenceHandler handler = GenericSequenceHandler.getHandler(format,bank);        
        handler.parse(file);
        countFile++;
        }
        
        application.log.info("Indexation over - "+new Date());
        
        }
        
        // Optimize the index
        if(cmd.hasOption("o")) {
        application.log.info("Optimizing index now...");
        IndexUtils.getServer().optimize();
        }
        application.log.info("Index is ready - "+new Date());
        
        coreContainer.shutdown();
        
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
}
