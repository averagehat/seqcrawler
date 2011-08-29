package org.irisa.genouest.seqcrawler.index.tokenizer;

import java.io.Reader;
import java.util.Map;


import org.apache.lucene.analysis.Tokenizer;
import org.apache.solr.analysis.TokenizerFactory;

/**
 * Experimental tokenizer, do not use
 */
public class DelimiterTokenizerFactory implements TokenizerFactory {
	
	Map<String, String> args = null;
	
	public Tokenizer create(Reader reader) {
		return new DelimiterTokenizer(reader,args.get("delimiters"));
	}

	public Map<String, String> getArgs() {
		return args;
	}

	public void init(Map<String, String> arg0) {
		args = arg0;
	}

	

}
