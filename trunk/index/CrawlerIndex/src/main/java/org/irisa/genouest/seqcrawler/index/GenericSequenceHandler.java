package org.irisa.genouest.seqcrawler.index;

import org.irisa.genouest.seqcrawler.index.handlers.GFFHandler;

/**
 * Generic Handler class. Returns a sequence handler according to the input format.
 * @author osallou
 *
 */
public class GenericSequenceHandler {
	
	
	/**
	 * Returns a sequence handler instance according to input type.
	 * @param format Format of the input file
	 * @param bank Bank name,if null will use defaults.
	 * @return A new instance of a sequence handler.
	 */
	public static SequenceHandler getHandler(Constants.FORMATS format,String bank) {
		SequenceHandler handler = null;
		switch(format) {
		case GFF: 
		{
			handler = new GFFHandler(bank);
			break;
		}
		default:
		{
			handler = new GFFHandler(bank);
			break;
		}
		}
		
		return handler;
	}
	
	
	public static SequenceHandler getHandler(Constants.FORMATS format) {
		return getHandler(format,null);
	}

}
