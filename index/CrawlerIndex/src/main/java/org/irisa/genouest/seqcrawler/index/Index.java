package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.xml.sax.SAXException;


/**
 * Main indexing application. Use -h command line option to get usage.
 * @author osallou
 *
 */
public class Index 
{
	private Log log = LogFactory.getLog(Index.class);
	
	static String bank=Constants.BANK_DEFAULT;
	static String inputFile=null;
	
	static Constants.FORMATS format = Constants.FORMATS.GFF;
	
	static boolean DEBUG=false;
	
	static int shardsSize = 10;
	static boolean useShards = false;
	
	static String solrHome = "/opt/solr/apache-solr-1.4.0/seqcrawler/solr";
	static String solrData = "/opt/solr/apache-solr-1.4.0/seqcrawler/solr/data/";
	static String solrUrl ="http://localhost/solr";
	boolean useEmbeddedServer = true;
	
	static long nbErrors=0;
	
	public static long getNbErrors() {
		return nbErrors;
	}

	
	
	
	/**
	 * Application entry point. Index one or more document in a Solr server. Index can be done with an embedded server for local writing.
	 * @param args See usage (-h)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SolrServerException
	 * @throws ParseException
	 * @throws IndexException 
	 */
    public static void main( String[] args ) throws IOException, ParserConfigurationException, SAXException, SolrServerException, ParseException
    {
    	
        Index application = new Index();
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
        
        IndexManager indexMngr = null;
        
        if(cmd.hasOption("v")) {
        	Class clazz = Index.class; 
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
        	solrHome = cmd.getOptionValue("sh");
        }
        
        if(cmd.hasOption("url")) {
        	solrUrl = cmd.getOptionValue("url");
        	application.useEmbeddedServer = false;
        }
        
        if(cmd.hasOption("sd")) {
        	solrData = cmd.getOptionValue("sd");
        }              
        	
       
        application.log.info("Starting application");
        // Note that the following property could be set through JVM level arguments too
        
        System.setProperty("solr.solr.home", solrHome);
        System.setProperty("solr.data.dir", solrData);        
        
        indexMngr = new IndexManager();
        
        if(application.useEmbeddedServer) {
        	indexMngr.initServer(null);
        }
        else {
        	indexMngr.initServer(solrUrl);
        }
        
        
        if(cmd.hasOption("c")) {
        	indexMngr.cleanIndexByBank(bank);
        }
        
        if(cmd.hasOption("C")) {
        	indexMngr.cleanIndex();
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
        	// Do not go through sub directories or manage hidden files such as svn files
        	if(file.isDirectory() || file.isHidden()) continue;
        	if(useShards && countFile > shardsSize) {
        		// If shardsSize is reached, reset counter and start a new shard index
        		newShard = true;
        		countFile = 1;
        	}
        	if(useShards && newShard) {
        		// Create a new index for index shard with shard id.
        		//coreContainer.shutdown();
        		indexMngr.shutdownServer();
        		System.setProperty("solr.solr.home", solrHome);
                System.setProperty("solr.data.dir", solrData+"/shard"+shardId);
                application.log.info("Using shard: "+System.getProperty("solr.data.dir"));                        
                
                if(application.useEmbeddedServer) {
                	indexMngr.initServer(null);
                }
                else {
                	indexMngr.initServer(solrUrl);
                }        		        		        		
        		shardId++;        		
        		newShard = false;
            }	
        SequenceHandler handler = GenericSequenceHandler.getHandler(format,bank);
        handler.setIndexManager(indexMngr);
        try {
			handler.parse(file);
			nbErrors+=handler.getNbParsingErrors();
		} catch (IndexException e) {
			application.log.error(e.getMessage());
		}
        countFile++;
        }
        
        application.log.info("Indexation over - "+new Date());
        application.log.info("Number of errors during indexation: "+nbErrors);
        
        }
        
        // Optimize the index
        if(cmd.hasOption("o")) {
        application.log.info("Optimizing index now...");
        indexMngr.getServer().optimize();
        }
        application.log.info("Index is ready - "+new Date());        
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
}
