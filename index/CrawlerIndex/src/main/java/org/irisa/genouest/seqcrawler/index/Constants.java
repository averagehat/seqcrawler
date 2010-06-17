package org.irisa.genouest.seqcrawler.index;

public class Constants {
	public static final String BANK_DEFAULT = "GenOuest";
	
	public static enum FORMATS {
		GFF,
		FASTA,
		GENBANK,
		RAW,
		READSEQ
	}
	
	public static enum STORAGEIMPL {
		RIAK
	}
}
