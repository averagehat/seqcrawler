package org.irisa.genouest.seqcrawler.index.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
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
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.util.NamedList;
import org.irisa.genouest.seqcrawler.index.Constants;
import org.irisa.genouest.seqcrawler.index.Index;
import org.irisa.genouest.seqcrawler.index.IndexManager;
import org.irisa.genouest.seqcrawler.query.Query;
import org.irisa.genouest.seqcrawler.tools.Tools;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Class managing useful tools to analyse index.
 * <br/> listfields option can be time consuming, e.g. it should not be called on a per request basis. A cron like task should be ran at regular 
 * interval to write the list to a file that can be queried remotely.
 * @author osallou
 *
 */
public class Utils {

	private Logger log = LoggerFactory.getLogger(Utils.class);

	static boolean DEBUG=false;
	
	String solrHome = Constants.SOLRHOME;
	
	String[] solrData = new String [] { Constants.SOLRDATA };
	
	File file = null;

	public static boolean isDEBUG() {
		return DEBUG;
	}

	public static void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}

	public String getSolrHome() {
		return solrHome;
	}

	public void setSolrHome(String solrHome) {
		this.solrHome = solrHome;
	}

	public String[] getSolrData() {
		return solrData;
	}
	
	public void setSolrData(String[] solrData) {
		this.solrData = solrData;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws IOException, JSONException {
		Utils utils = new Utils();
		utils.init();
		
		Options options = new Options();
        options.addOption("sh", true, "solr home path");
        options.addOption("sd", true, "solr data index path, add one option per directory to analyse");
        options.addOption("debug", false, "for debug");
        options.addOption("h", false, "show usage");
        options.addOption("v", false, "show version");
        options.addOption("listfields",false,"Get list of fields");
        options.addOption("file",true,"Output file name");
        
        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			utils.log.error("Could not parse input parameter, check your command line. Use -h for help.\n"+e.getMessage());
			return;
		}
        
        
        if(cmd.hasOption("v")) {
        	Class<Index> clazz = Index.class; 
        	String classContainer = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
        	URL manifestUrl = new URL("jar:" + classContainer + "!/META-INF/MANIFEST.MF");
        	Manifest mf = new Manifest(manifestUrl.openStream());
        	Attributes atts = mf.getMainAttributes();
        	
        	System.out.println("SeqCrawler indexer version: "+atts.getValue("Implementation-Version")+"-"+atts.getValue("Implementation-Build"));        	
        	utils.log.info("Version: " + atts.getValue("Implementation-Version"));
        	utils.log.info("Build: " + atts.getValue("Implementation-Build"));
        	
        	System.exit(0);
        }
        
        if(cmd.hasOption("h")) {
        	showUsage(options);
        	System.exit(0);
        }
        
        if(cmd.hasOption("debug")) {
        	DEBUG=true;
        }
        
        if(cmd.hasOption("sh")) {
        	utils.solrHome = cmd.getOptionValue("sh");
        }
        
        if(cmd.hasOption("sd")) {
        	utils.solrData = cmd.getOptionValues("sd");
        }
        
        if(cmd.hasOption("file")) {
        	utils.file = new File(cmd.getOptionValue("file"));

        }

        if(cmd.hasOption("listfields")) {
        	String[] fields = utils.listFields();
        	utils.writeFields(fields);
        }
        
	}
	
	/**
	 * Outputs field list to file or log depending on file value
	 * @param fields List of field names
	 * @throws IOException
	 * @throws JSONException
	 */
	protected void writeFields(String[] fields) throws IOException, JSONException {
		if(fields.length>0) {
			if(file!=null) {
        	FileWriter fw = new FileWriter(file);
        	fw.write(" { fields = ["+fields[0]);
        	for(int i=1;i<fields.length;i++) {
        		fw.write(","+fields[i]);
        	}
        	fw.write(" ] } \n ");
        	fw.close();
			}
			else {
        	String test = " { fields = ["+fields[0];
        	for(int i=1;i<fields.length;i++) {
        		test+=","+fields[i];
        	}
        	test+=" ] } \n ";
        	JSONObject jsonobj = new JSONObject(test);
        	log.info("JSON: "+jsonobj.toString());
			}
    	}
    	else {
    		log.warn("No field found");
    	}
	}

	/**
	 * Get the list of indexed fields e.g. the field names that can be queried.
	 * @return List of field names.
	 */
	public String[] listFields() {
        log.info("Starting fields listing");
        // Note that the following property could be set through JVM level arguments too
        log.info("Warning, this task can be time consuming depending on index size.");

        Vector<String> fields = new Vector<String>();
        
        System.setProperty("solr.solr.home", solrHome);
        
        IndexManager indexMngr = null;
        
        for(String indexDir : solrData) {
        
        	log.info("Analysing index "+indexDir);
        	
        	System.setProperty("solr.data.dir", indexDir);        
        
	        indexMngr = new IndexManager();
	        try {
				indexMngr.initServer(null);
				
				/*
				 * Query index schema
				 */
				
			    LukeRequest luke = new LukeRequest();
			    luke.setShowSchema( true );
			    LukeResponse rsp = luke.process( indexMngr.getServer() );
			    Map<String,FieldInfo> fieldMap = rsp.getFieldInfo();
			    for(Entry<String,FieldInfo> field : fieldMap.entrySet()) {
			    	log.debug("Field "+field.getKey());
			    	log.debug("Flags: "+field.getValue().getFlags().toString());
			    	if(field.getValue().getFlags().contains(FieldFlag.INDEXED)) {
			    		// Is indexed, so can be queried
			    		if(!fields.contains(field.getKey())) {
			    			fields.add(field.getKey());
			    		}
			    	}
			    }
			    
			} catch (IOException e) {
				log.error(e.getMessage());
			} catch (ParserConfigurationException e) {
				log.error(e.getMessage());
			} catch (SAXException e) {
				log.error(e.getMessage());
			} catch (SolrServerException e) {
				log.error(e.getMessage());
			} 
	        indexMngr.shutdownServer();
        
        }
        
        log.info("Analysis over!");
        
        String[] resultFields = null;
        
        if(fields.size()>0) {
	        resultFields = new String[fields.size()];
	        
	        int i=0;
	        for(String field : fields) {
	        	log.debug("Field: "+field);
	        	resultFields[i] = field;
	        	i++;
	        }       
	        
        }
        
        Arrays.sort(resultFields);
        
        return resultFields;
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
	protected void init() {
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
				solrData = new String[]  { properties.getProperty("solr.data.dir") };
				log.info("Using solr data dir "+solrData+" from properties");
			}
		}
		catch (IOException e) { 
			log.error(e.getMessage());
		} 
	}

}
