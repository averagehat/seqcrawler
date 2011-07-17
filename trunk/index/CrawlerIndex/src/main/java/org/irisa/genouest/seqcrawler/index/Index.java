
package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
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
import org.apache.solr.client.solrj.SolrServerException;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;
import org.irisa.genouest.seqcrawler.index.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


/**
 * Main indexing application. Use -h command line option to get usage.
 * @author osallou
 *
 */
public class Index 
{
	private Logger log = LoggerFactory.getLogger(Index.class);
	
	private static String PROPFILE ="seqcrawler.properties";
	
	String bank=Constants.BANK_DEFAULT;
	String inputFile=null;
	
	Constants.FORMATS format = null;
	
	static boolean DEBUG=false;
	
	int shardsSize = 10;
	boolean useShards = false;
	
	String solrHome = Constants.SOLRHOME;
	String solrData = Constants.SOLRDATA;
	
	String solrUrl ="http://localhost/solr";
	boolean useEmbeddedServer = true;
	
    long nbErrors=0;
	
	public long getNbErrors() {
		return nbErrors;
	}

	boolean doStore = false;
	String stHost=null;
	String stPort=null;
	
	
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
    	application.init();
    	application.index(args);
    }
    
    /**
	 * Index entry point. Index one or more document in a Solr server. Index can be done with an embedded server for local writing.
	 * Should not be called in within different threads for a single index server.
	 * @param args See usage (-h)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SolrServerException
	 * @throws ParseException
	 * @throws IndexException 
	 */
    public void index(String[] args) throws SolrServerException, IOException, ParseException, ParserConfigurationException, SAXException {
    	    
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

        options.addOption("store", false, "Use database storage for raw data (Fasta in GFF file for example)");
        options.addOption("storage", true, "Storage implementation: riak(default), mongodb, mock(test)");
        
        options.addOption("stHost", true, "If store,Host for storage backend, default is localhost");
        options.addOption("stPort", true, "If store,Port for storage backend, default is 8098");
        
        options.addOption("max", true, "Maximum number of documents before a commit, default is "+Constants.MAXCOMMITS);
        
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
        	log.info("Version: " + atts.getValue("Implementation-Version"));
        	log.info("Build: " + atts.getValue("Implementation-Build"));
        	
        	System.exit(0);
        }
        
        if(cmd.hasOption("h")) {
        	showUsage(options);
        	System.exit(0);
        }
        
        if(cmd.hasOption("max")) {
        	Constants.MAXCOMMITS = Long.parseLong(cmd.getOptionValue("max"));
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
        	useEmbeddedServer = false;
        }
        
        if(cmd.hasOption("sd")) {
        	solrData = cmd.getOptionValue("sd");
        }              
        	
       
        log.info("Starting application");
        // Note that the following property could be set through JVM level arguments too
        
        System.setProperty("solr.solr.home", solrHome);
        System.setProperty("solr.data.dir", solrData);        
        // Create manager
        indexMngr = new IndexManager();
        
        if(cmd.hasOption("store")) {
        	log.debug("Use storage in addition to index");
        	indexMngr.getArgs().put("store", "true");
        	if(cmd.hasOption("stHost")) {
        		stHost = cmd.getOptionValue("stHost");
        		indexMngr.getArgs().put("host", stHost);
        	}
        	if(cmd.hasOption("stPort")) {
        		stPort = cmd.getOptionValue("stPort");
        		indexMngr.getArgs().put("port", stPort);
        	}
        	// If storage is selected, choose the implementation
        	if(cmd.hasOption("storage")) {
        			indexMngr.setStorageImpl(StorageManager.getStorageImpl(cmd.getOptionValue("storage")));
        	}
        }
        
        //has field recoders ?
        setModifiers(indexMngr);
        
        // Now init the server (embedded or remote connection)
        if(useEmbeddedServer) {
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
        	log.info("Indexing input file "+inputFile);
        }
        else {
        	log.error("WARNING: Input file command line option is missing (-f) ");
        	log.error("There is no data to index ");
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
        	else if(t.equalsIgnoreCase("raw")) {
        		format = Constants.FORMATS.RAW;
        	}
        	else if(t.equalsIgnoreCase("readseq")) {
        		format = Constants.FORMATS.READSEQ;
        	}
        	else if(t.equalsIgnoreCase("embl")) {
        		format = Constants.FORMATS.EMBL;
        	}
        	else if(t.equalsIgnoreCase("pdb")) {
        		format = Constants.FORMATS.PDB;
        	}
        }
        else {
        	// DEFAULT
        	format = Constants.FORMATS.GFF;
        }
        
        
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
        
        log.info("Start indexation - "+new Date());
        
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
                log.info("Using shard: "+System.getProperty("solr.data.dir"));                        
                
                if(useEmbeddedServer) {
                	indexMngr.initServer(null);
                }
                else {
                	indexMngr.initServer(solrUrl);
                }        		        		        		
        		shardId++;        		
        		newShard = false;
            }	
        SequenceHandler handler = null;
        
        // If format is not a specified one, then use custom handler to load external script
        if(format==null && cmd.hasOption("t")) {
        	 log.info("Custom Input file format is "+cmd.getOptionValue("t"));
        	handler = GenericSequenceHandler.getCustomHandler(cmd.getOptionValue("t"),bank);
        }
        else {
        	log.info("Input file format is "+format.toString());
        	handler = GenericSequenceHandler.getHandler(format,bank);
        }
        
        handler.setIndexManager(indexMngr);
        try {
			handler.parse(file);
			nbErrors+=handler.getNbParsingErrors();
		} catch (IndexException e) {
			log.error(e.getMessage());
		}
        countFile++;
        }
        
        log.info("Indexation over - "+new Date());
        log.info("Number of errors during indexation: "+nbErrors);
        
        }
        
        // Optimize the index
        if(cmd.hasOption("o")) {
        log.info("Optimizing index now...");
        indexMngr.getServer().optimize();
        }
        log.info("Index is ready - "+new Date());        
        indexMngr.shutdownServer();
    }

    /**
     * Adds recorders defined in properties file to the config, setup filters etc...
     * @param indexMngr Index Manager instance
     */
    private void setModifiers(IndexManager indexMngr) {
    	Properties properties = new Properties();
		try {
			File props = new File(PROPFILE);
			if(props.exists()) {
			properties.load(new FileInputStream(props)); }
			for(Entry<Object,Object> prop :properties.entrySet()) {
				String key = (String) prop.getKey();
				String value = (String) prop.getValue();
				if(key.endsWith(".recode")) {
					// If mybank.myfield.recode = org.irisa.....field.SampleRecoder , then add to recoders.
					log.debug("loading recoder "+key);
					indexMngr.getArgs().put(key, value);
				}
				if(key.equals("fields.include")) {
					IndexManager.setIncludeFilter(value);
				}
				if(key.equals("fields.exclude")) {
					IndexManager.setExcludeFilter(value);
				}
				if(key.endsWith(".add")) {
					String fieldname = key.replace(".add", "");
					IndexManager.additionalFields.put(fieldname, value);
				}
			}
		}
		catch (IOException e) { 
			log.error(e.getMessage());
		} 
		
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
			File props = new File(PROPFILE);
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

	public static String getPROPFILE() {
		return PROPFILE;
	}

	public static void setPROPFILE(String pROPFILE) {
		PROPFILE = pROPFILE;
	}
}
