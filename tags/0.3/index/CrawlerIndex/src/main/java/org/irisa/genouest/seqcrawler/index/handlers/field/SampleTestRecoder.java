package org.irisa.genouest.seqcrawler.index.handlers.field;

import org.irisa.genouest.seqcrawler.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example recode implementation
 * @author osallou
 *
 */
public class SampleTestRecoder implements FieldRecoder {

	private Logger log = LoggerFactory.getLogger(SampleTestRecoder.class);
	
	public String[][] recode(String key, String value) {
		log.warn("recode "+key+"="+value);
		return new String[][] {
				new String[] {"sample"+key , "sample"+value}
		};
	}

}
