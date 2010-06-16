package org.irisa.genouest.seqcrawler.index.tokenizer;

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;
import org.irisa.genouest.seqcrawler.index.IndexTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental tokenizer to split according to a list of delimiters.
 * @author osallou
 *
 */
public class DelimiterTokenizer extends CharTokenizer {
	
	private Logger log = LoggerFactory.getLogger(DelimiterTokenizer.class);
	
	private String delimiters;
	
	public DelimiterTokenizer(Reader reader,String delimiters) {		
		super(reader);
	    this.delimiters = delimiters;
	}

	@Override
	protected boolean isTokenChar(char arg0) {
		if(delimiters==null) return true;
		for(int i=0;i<delimiters.length();i++) {
			if(arg0==delimiters.charAt(i))  {
				log.debug("token char to be removed "+arg0);
				return false;
			}
		}
		return true;
	}
}
