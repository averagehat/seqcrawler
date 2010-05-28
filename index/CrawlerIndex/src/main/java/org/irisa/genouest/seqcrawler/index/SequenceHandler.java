package org.irisa.genouest.seqcrawler.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Interface that sequence handlers must implement
 * @author osallou
 *
 */
public interface SequenceHandler {
	/**
	 * Parse an input file
	 * @param f path to input file
	 * @throws IOException
	 */
	 public void parse(File f) throws IOException;
	 /**
	  * Parse an input stream
	  * @param bf stream to analyse
	  * @throws IOException
	  */
	 public void parse(BufferedReader bf) throws IOException;
}
