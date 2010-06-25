package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
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
	
	/**
	 * Entry point to merge a list of index directories
	 * @param args List of directory index to merge. First directory is final index destination.
	 * If only one directory is set, then this index is optimized.
	 */
	public static void main(String[] args) {
		Merge mergeApp = new Merge();
		if(args.length==0 || args[0].trim().equalsIgnoreCase("-h")) {
			System.err.println("No argument given.");
			System.err.println("Usage arguments: finaldir indexesdir");
			System.err.println("indexesdir is location where are stored all indexes to merge (indexesdir/myindex/index");
			System.err.println("--/finaldir");
			System.err.println("--/indexesdir");
			System.err.println("----/index1");
			System.err.println("------/index");
			System.err.println("----/index2");
			System.err.println("------/index");
			System.err.println("If only finaldir is set, optimization is run against it.");
			mergeApp.log.error("Missing arguments");
			System.exit(0);
		}
		if(args.length==1) {
		mergeApp.optimize(args[0]);	
		}
		else {
		mergeApp.merge(args);
		}

	}

	/**
	 * Run Lucene optimization on the index
	 * @param dir input directory where to run the optimization
	 */
	private void optimize(String dir) {
		File INDEX_DIR    = new File(dir);

		INDEX_DIR.mkdir();

		Date start = new Date();

		try {
			IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR),
												new StandardAnalyzer(Version.LUCENE_CURRENT),
												true, IndexWriter.MaxFieldLength.UNLIMITED);
			writer.setMergeFactor(10);
			writer.setRAMBufferSizeMB(50);


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
	 * @param args Directory where to create the index and directory hosting the indexes to merge
	 */
	public void merge(String[] args) {
		File INDEXES_DIR  = new File(args[1]);
		File INDEX_DIR    = new File(args[0]);

		INDEX_DIR.mkdir();

		Date start = new Date();

		try {
			IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR),
												new StandardAnalyzer(Version.LUCENE_CURRENT),
												true, IndexWriter.MaxFieldLength.UNLIMITED);
			writer.setMergeFactor(10);
			writer.setRAMBufferSizeMB(50);

			Directory indexes[] = new Directory[INDEXES_DIR.list().length];

			for (int i = 0; i < INDEXES_DIR.list().length; i++) {
				log.info("Adding: " + INDEXES_DIR.list()[i]);
				indexes[i] = FSDirectory.open(new File(INDEXES_DIR.getAbsolutePath()
													+ "/" + INDEXES_DIR.list()[i] + "/index"));
			}

			log.info("Merging added indexes...");
			writer.addIndexesNoOptimize(indexes);
			log.info("done");


			writer.close();
			log.info("done");

			Date end = new Date();
			log.info("It took: "+((end.getTime() - start.getTime()) / 1000)
											+ "\"");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
