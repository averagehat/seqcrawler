package org.irisa.genouest.seqcrawler.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.irisa.genouest.seqcrawler.index.exceptions.IndexException;

/**
 * Interface that sequence handlers must implement
 * @author osallou
 *
 */
public interface SequenceHandler {
	
	/**
	 * Sets the IndexManager to be used by the sequence handler
	 * @param manager IndexManager instance
	 */
	public void setIndexManager(IndexManager manager);
	 
	
	/**
	 * Parse an input file
	 * @param f path to input file
	 * @throws IOException
	 * @throws IndexException 
	 */
	 public void parse(File f) throws IOException, IndexException;
	 /**
	  * Parse an input stream
	  * @param bf stream to analyse
	  * @throws IOException
	 * @throws IndexException 
	  */
	 public void parse(BufferedReader bf) throws IOException, IndexException;
}
