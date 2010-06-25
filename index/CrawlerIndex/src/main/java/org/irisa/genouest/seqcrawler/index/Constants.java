package org.irisa.genouest.seqcrawler.index;

public class Constants {
	public static final String BANK_DEFAULT = "GenOuest";
	
	/**
	 * Available parsers for input sequences/documents. RAW format is sent to solr server and server will try to parse the file itself if format is recognized.
	 * @author osallou
	 *
	 */
	public static enum FORMATS {
		GFF,
		FASTA,
		GENBANK,
		RAW,
		READSEQ
	}
	
	/**
	 * Available backend storage implementations
	 * @author osallou
	 *
	 */
	public static enum STORAGEIMPL {
		RIAK,
		CASSANDRA,
		MONGODB,
		MOCK // For test, simulate storage backend
	}
	
	/**
	 * Option to use storage
	 */
	public static final String STORE = "store";
}
