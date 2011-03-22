package org.irisa.genouest.seqcrawler.index;

public class Constants {
	public static final String BANK_DEFAULT = "GenOuest";
	
	public static final String SOLRHOME = "/opt/solr/apache-solr-1.4.1/seqcrawler/solr";
	public static final String SOLRDATA = "/opt/solr/apache-solr-1.4.1/seqcrawler/solr/data/";
	
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
		READSEQ,
		EMBL,
		PDB
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
	
	/**
	 * Max number of documents of manager before a commit.
	 */
	public static long MAXCOMMITS = 100;
}
