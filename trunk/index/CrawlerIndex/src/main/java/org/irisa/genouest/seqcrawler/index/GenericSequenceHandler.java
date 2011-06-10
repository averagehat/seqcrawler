package org.irisa.genouest.seqcrawler.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.irisa.genouest.seqcrawler.index.handlers.EMBLHandler;
import org.irisa.genouest.seqcrawler.index.handlers.FastaHandler;
import org.irisa.genouest.seqcrawler.index.handlers.GFFHandler;
import org.irisa.genouest.seqcrawler.index.handlers.JSHandler;
import org.irisa.genouest.seqcrawler.index.handlers.PDBHandler;
import org.irisa.genouest.seqcrawler.index.handlers.RawFileHandler;
import org.irisa.genouest.seqcrawler.index.handlers.ReadSeqFileHandler;

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
		case RAW:
		{
			handler = new RawFileHandler(bank);
			break;
		}
		case READSEQ:
		{
			handler = new ReadSeqFileHandler(bank);
			break;
		}
		case FASTA:
		{
			handler = new FastaHandler(bank);
			break;
		}
		case EMBL:
		{
			handler = new EMBLHandler(bank);
			break;
		}
		case PDB:
		{
			handler = new PDBHandler(bank);
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
	
	
	public static SequenceHandler getCustomHandler(String format, String bank) {
		SequenceHandler handler = null;
		JSHandler jshandler = new JSHandler(bank);
		jshandler.setScriptFile(System.getProperty("solr.solr.home")+"/plugin/"+format+".js");
		handler = jshandler;
		
		return handler;
	}
	
	public static SequenceHandler getCustomHandler(String format) {
		return getCustomHandler(format,null);
	}

}
