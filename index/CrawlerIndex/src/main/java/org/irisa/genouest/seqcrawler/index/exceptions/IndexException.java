package org.irisa.genouest.seqcrawler.index.exceptions;

public class IndexException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4281441003336015258L;

	private String indexData = null;
	
	public IndexException(String message) {
		super(message);
	}

	public String getIndexData() {
		return indexData;
	}

	public void setIndexData(String indexData) {
		this.indexData = indexData;
	}

}
