package org.irisa.genouest.seqcrawler.CrawlerSearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Export class used to export a number of matches from a query to a file
 * @author osallou
 *
 */
public class Export {

	private Logger log = LoggerFactory.getLogger(Export.class);
	
	public static final String SOLRHOME = "/usr/share/seqcrawler/solr/apache-solr/seqcrawler/solr";
	public static final String SOLRDATA = "/var/lib/seqcrawler/index/solr";


	String outputFile=null;
	String tmpDir = "/tmp";
	
	
	static boolean DEBUG=false;
	
	
	String solrHome = SOLRHOME;
	String solrData = SOLRDATA;
	
	String[] exportRanges = null;
	
	String query=null;
	
	String queryType=null;
	
	String url=null;
	
	final static String XMLHEADER="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<docs>\n";
	final static String XMLFOOTER="</docs>";
	
	/**
	 * @param args
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws SolrServerException 
	 */
	public static void main(String[] args) throws IOException, ParseException, ParserConfigurationException, SAXException, SolrServerException {
		Export application = new Export();
    	application.init();
    	application.parseCommandLine(args);
    	application.export();

	}
	
	private void parseCommandLine(String[] args) throws ParseException, IOException {
	      Options options = new Options();
	        options.addOption("f", true, "output file");
	        options.addOption("sh", true, "solr home path");
	        options.addOption("sd", true, "solr data index path");
	        options.addOption("debug", false, "for debug, do not index");
	        options.addOption("h", false, "show usage");
	        options.addOption("v", false, "show version");
	        options.addOption("query", true, "original query");
	        options.addOption("queryType", true, "request handler to use");
	        options.addOption("ranges", true, "Ranges to export, format is:  A-B,C-D,E-F");
	        options.addOption("url", true, "URL to index server");
	        CommandLineParser parser = new PosixParser();
	        CommandLine cmd = parser.parse( options, args);	     
	        
	        if(cmd.hasOption("v")) {
	        	Class<Export> clazz = Export.class; 
	        	String classContainer = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
	        	URL manifestUrl = new URL("jar:" + classContainer + "!/META-INF/MANIFEST.MF");
	        	Manifest mf = new Manifest(manifestUrl.openStream());
	        	Attributes atts = mf.getMainAttributes();
	        	
	        	System.out.println("SeqCrawler searcher version: "+atts.getValue("Implementation-Version")+"-"+atts.getValue("Implementation-Build"));        	
	        	log.info("Version: " + atts.getValue("Implementation-Version"));
	        	log.info("Build: " + atts.getValue("Implementation-Build"));
	        	
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
	        	solrHome = cmd.getOptionValue("sh");
	        }
	        
	        if(cmd.hasOption("sd")) {
	        	solrData = cmd.getOptionValue("sd");
	        }   
	        
	        if(cmd.hasOption("ranges")) {
	        	String ranges = cmd.getOptionValue("ranges");
	        	exportRanges = ranges.split(",");
	        } 
	        else {
	        	log.error("No range given as input, please specify one");
	        	System.exit(0);
	        }
	        if(cmd.hasOption("query")) {
	        	query = cmd.getOptionValue("query");
	        } else {
	        	log.error("No query given as input, please specify one");
	        }
	        if(cmd.hasOption("queryType")) {
	        	queryType = cmd.getOptionValue("queryType");
	        }
	        
	        if(cmd.hasOption("url")) {
	        	url = cmd.getOptionValue("url");
	        } 
	        	
	        if(cmd.hasOption("f")) {
	        	outputFile = cmd.getOptionValue("f");
	        }
	        else {
	        	UUID random = UUID.randomUUID();
	        	outputFile = tmpDir+"/tmpexport_"+random.toString()+".txt";
	        }
	       
	        log.info("Starting application");
	        // Note that the following property could be set through JVM level arguments too
	        
	        System.setProperty("solr.solr.home", solrHome);
	        System.setProperty("solr.data.dir", solrData);     
	}

	public void export() throws IOException, ParseException, ParserConfigurationException, SAXException, SolrServerException {
	        
	        IndexManager indexMngr = null;	              
	        // Create manager
	        indexMngr = new IndexManager();
	        	     	        
	        FileWriter fw = new FileWriter(outputFile);
	        fw.write(XMLHEADER);
	        	        
	        indexMngr.initServer(url);
	        
	        for(String range : exportRanges){
	        	log.debug("export range "+range);
	        	String[] startstop = range.split("-");
	        	if(startstop.length!=2){
	        		continue;
	        	}
	        	/*
	        	 * Start and stop values starts at 1. Solr queries starts at 0, so lower each value by one.
	        	 */
	        	int start = Integer.parseInt(startstop[0]);
	        	int stop = Integer.parseInt(startstop[1]);
	        	if(start>0) start-=1;
	        	if(stop>0) stop-=1;
	        

			SolrQuery solrQuery = new SolrQuery();
	        solrQuery.setQuery(query);
	        if(queryType!=null) {
	        	solrQuery.setQueryType(queryType);
	        	log.debug("Using handler "+queryType);
	        }
	        solrQuery.setParam("start", String.valueOf(start));
	        //Do not get more than 100 matches for same request
	        boolean multipleQueries = false;
	        
	        if(stop-start>100) {
	        	multipleQueries = true;
	        	solrQuery.setParam("rows", "100");
	        }
	        else {
	        	solrQuery.setParam("rows", String.valueOf(stop-start+1));
	        }
	        
	        QueryResponse rsp = indexMngr.getServer().query(solrQuery);
	        SolrDocumentList docs = rsp.getResults();
	        
	        long nbFound = docs.getNumFound();
	        
	        writeResults(fw,docs,-1);
	        
	        long size = docs.size();
	        
	        if(multipleQueries) {
	        	
	        boolean over = false;
	        
	        while(!over) {
	        	start+=100;
	        	solrQuery = new SolrQuery();
		        solrQuery.setQuery(query);
		        if(queryType!=null) {
		        	solrQuery.setQueryType(queryType);
		        }
		        solrQuery.setParam("start", String.valueOf(start));
	        	solrQuery.setParam("rows", "100");
		        rsp = indexMngr.getServer().query(solrQuery);
		        docs = rsp.getResults();
		        size+=docs.size();
		        if(size>=(stop-start) || size>=nbFound) {
		        	over =true;
		        }
		        if(start+100>stop) {
		        	writeResults(fw,docs,stop-start+1);
		        }
		        else {
		        	writeResults(fw,docs,-1);	
		        }
	        }
	        }
	        
	        }
	        fw.write(XMLFOOTER);
	        fw.close();
	        
	        indexMngr.shutdownServer();
	}

	private void writeResults(FileWriter fw, SolrDocumentList docs, int max) {
		int size = docs.size();
		if(docs.size()==0) return;
		if(max>-1) {
			size = max;
		}
		for(int i=0;i<size;i++) {
			SolrDocument doc = null;
			try {
				doc = (SolrDocument)docs.get(i);
				String id = (String)doc.getFieldValue("id");
				if(id!=null) {
				fw.write("<doc id=\""+id+"\">\n");	
				}
				else {
				fw.write("<doc>\n");
				}
			} catch (IOException e1) {
				log.error(e1.getMessage());
				continue;
			}
			//Map<String,Object> map = docs.get(i).getFieldValueMap();
			Collection<String> fieldNames = doc.getFieldNames();
			for(Object fieldName : fieldNames) {
				try {
					fw.write("<"+(String) fieldName+">");
					if(doc.getFieldValue((String)fieldName) instanceof Integer) {
					fw.write(""+doc.getFieldValue((String)fieldName));
					}
					else if(doc.getFieldValue((String)fieldName) instanceof ArrayList) {
						ArrayList list = (ArrayList)doc.getFieldValue((String)fieldName);
						fw.write(""+list.get(0));
						for(int l=1;l<list.size();l++) {
						fw.write(","+(String)list.get(l));
						}
					}
					else {
					fw.write((String)doc.getFieldValue((String)fieldName));
					}
					fw.write("</"+(String) fieldName+">\n");
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			try {
				fw.write("</doc>\n");
			} catch (IOException e1) {
				log.error(e1.getMessage());
				continue;
			}
			
		}
		
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
			if(properties.containsKey("dir.tmp")) {
				tmpDir = properties.getProperty("dir.tmp");
				log.info("Using tmp dir "+tmpDir+" from properties");
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

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public String getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}


	public String getSolrHome() {
		return solrHome;
	}

	public void setSolrHome(String solrHome) {
		this.solrHome = solrHome;
	}

	public String getSolrData() {
		return solrData;
	}

	public void setSolrData(String solrData) {
		this.solrData = solrData;
	}

	public String[] getExportRanges() {
		return exportRanges;
	}

	public void setExportRanges(String[] exportRanges) {
		this.exportRanges = exportRanges;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getQueryType() {
		return queryType;
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

}
