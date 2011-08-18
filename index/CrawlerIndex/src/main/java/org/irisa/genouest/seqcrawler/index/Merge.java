package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry to merge multiple indexes or optimize a single one
 * @author osallou
 *
 */
public class Merge {

	private Logger log = LoggerFactory.getLogger(Merge.class);
	
	private String finalDir = null;
	private String indexesDir = null;
	private String[] excludeConditions = null;
	private String[] includeConditions = null;
	
	private Directory indexes[];
	

	
	public Directory[] getIndexes() {
		return indexes;
	}

	public void setIndexes(Directory[] indexes) {
		this.indexes = indexes;
	}

	public IndexWriterConfig getIndexConfig() {
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_33,new StandardAnalyzer(Version.LUCENE_33));
		conf.setRAMBufferSizeMB(50);
		LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
		//policy.setMaxMergeDocs(100000);
		conf.setMergePolicy(policy);
		return conf;
	}
	

	private boolean DEBUG = false;
	
	/**
	 * Entry point to merge a list of index directories
	 * @param args List of directory index to merge. First directory is final index destination.
	 * If only one directory is set, then this index is optimized.
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
		Merge mergeApp = new Merge();	
		
        Options options = new Options();
        options.addOption("h", false, "Show usage.");
        options.addOption("in", true, "[merge,optimize] location where are stored all indexes to merge (indexesdir/myindex/index) for merge operation, or directory to optimize for optimization operation");
        options.addOption("out", true, "[merge] Output index path");
        options.addOption("O", false, "[optimize] Optimize the input index.");
        options.addOption("inc", true, "[merge] include index name expression, default is include all");
        options.addOption("exc", true, "[merge] exclude index name expression, default is exclude none");
        options.addOption("debug", false, "[merge] Debug usage, simulate operation, do not run the merge operation.");
        
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse( options, args);
        if(cmd.hasOption("h")) {
        	showUsage(options);
        	System.exit(0);
        }
        if(cmd.hasOption("debug")) {
        	mergeApp.DEBUG = true;
        }
        if(cmd.hasOption("in")) {
        	mergeApp.indexesDir = cmd.getOptionValue("in");
        }
        else {
        	showUsage(options);
        	System.exit(1);
        }
        if(cmd.hasOption("out")) {
        	mergeApp.finalDir = cmd.getOptionValue("out");
        }
        else if(!cmd.hasOption("O")){
        	showUsage(options);
        	System.exit(1);
        }
        
        if(cmd.hasOption("O")) {
        	mergeApp.optimize(cmd.getOptionValue("in"));
        	System.exit(0);
        }
        else {
        	if(cmd.hasOption("inc")) {
            	mergeApp.includeConditions = cmd.getOptionValues("inc");
            }
        	if(cmd.hasOption("exc")) {
            	mergeApp.excludeConditions = cmd.getOptionValues("exc");
            }
        	if(cmd.hasOption("O")) {
            	mergeApp.optimize(cmd.getOptionValue("in"));
            	System.exit(0);
            }
        	mergeApp.merge();
        }

	}

	/**
	 * Run Lucene optimization on the index
	 * @param dir input directory where to run the optimization
	 */
	private void optimize(String dir) {
		File INDEX_DIR    = new File(dir);

		Date start = new Date();

		try {
			IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR),getIndexConfig() );

			log.info("Optimizing index...");
			writer.optimize();
			writer.close();
			log.info("done");

			Date end = new Date();
			log.info("It took: "+((end.getTime() - start.getTime()) / 1000)
											+ "\"");

		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

	/**
	 * Merge a list of directory
	 */
	public void merge() {
		File INDEXES_DIR  = new File(indexesDir);
		File INDEX_DIR    = new File(finalDir);

		INDEX_DIR.mkdir();
		
		List<File> indexList = new ArrayList<File>();

		Date start = new Date();

		try {
			/*IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR),
												new StandardAnalyzer(Version.LUCENE_CURRENT),
												true, IndexWriter.MaxFieldLength.UNLIMITED);
			writer.setMergeFactor(10);
			writer.setRAMBufferSizeMB(50);*/
			

			IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR),getIndexConfig() );


			for (int i = 0; i < INDEXES_DIR.list().length; i++) {
				// check for include/exclude conditions
				String dirName = INDEXES_DIR.list()[i];
				if(includeConditions!=null) {
					boolean match = false;
					for(String condition : includeConditions) {
						 Pattern p = Pattern.compile(".*"+condition+".*");
						 Matcher m = p.matcher(dirName);
						 if(m.matches()) {
							// Matched one include condition, continue
							match = true;
							break;
						 }
					}
					if(match == false) {
						// No match , skip it
						log.info("Skipping dir "+dirName);
						continue;
					}				
					
				}
				if(excludeConditions!=null) {
					boolean match = true;
					for(String condition : excludeConditions) {
						 Pattern p = Pattern.compile(".*"+condition+".*");
						 Matcher m = p.matcher(dirName);
						 if(m.matches()) {
							// Matched one exclude condition, skip it
							match = false;
							break;
						 }
					}
					if(match == false) {
						// No match , skip it
						log.info("Skipping dir "+dirName);
						continue;
					}
				}
				log.info("Adding: " + INDEXES_DIR.list()[i]);
				indexList.add(new File(INDEXES_DIR.getAbsolutePath()+ "/" + INDEXES_DIR.list()[i] + "/index"));
				
			}

			
			log.info("Merging added indexes...");
			indexes = new Directory[indexList.size()];
			int i=0;
			for(File f : indexList) {
				indexes[i] = FSDirectory.open(f);
				i++;
			}
			if(!DEBUG) {
				writer.addIndexes(indexes);
			//writer.addIndexesNoOptimize(indexes);
			log.info("done");
			}


			writer.close();
			log.info("done");

			Date end = new Date();
			log.info("It took: "+((end.getTime() - start.getTime()) / 1000)
											+ "\"");

		} catch (IOException e) {
			e.printStackTrace();
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

	public String getFinalDir() {
		return finalDir;
	}

	public void setFinalDir(String finalDir) {
		this.finalDir = finalDir;
	}

	public String getIndexesDir() {
		return indexesDir;
	}

	public void setIndexesDir(String indexesDir) {
		this.indexesDir = indexesDir;
	}

	public String[] getExcludeConditions() {
		return excludeConditions;
	}

	public void setExcludeConditions(String[] excludeConditions) {
		this.excludeConditions = excludeConditions;
	}

	public String[] getIncludeConditions() {
		return includeConditions;
	}

	public void setIncludeConditions(String[] includeConditions) {
		this.includeConditions = includeConditions;
	}

	public boolean isDEBUG() {
		return DEBUG;
	}

	public void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}
}
