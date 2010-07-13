package org.irisa.genouest.seqcrawler.index.handlers.field;

/**
 * Interface implemented by Field recorder classes, used to transform a field in one or many others
 * @author osallou
 *
 */
public interface FieldRecoder {

	/**
	 * Recode an input key value to a list of new key value pairs.
	 * @param key Original field name
	 * @param value Original field value
	 * @return A list of strings with key value pairs.
	 */
	public String[][] recode(String key,String value);
	
}
