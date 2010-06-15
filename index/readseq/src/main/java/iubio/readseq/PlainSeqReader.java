package iubio.readseq;

import java.io.File;
import java.io.IOException;

public class PlainSeqReader  extends BioseqReader
{
	// very simple format -- blank line b/n sequences, no seqid
	protected File inputFile;
	
	public PlainSeqReader() {
		margin	=  0;
		addfirst= true;
		addend 	= true;  
		ungetend= false;
		//formatId= 13;
		}
	
	public boolean endOfSequence() {
	  return (nWaiting == 0 || getreadbuf(0) == '\n' || getreadbuf(0) == '\r');
		}

	public void setInputFile(File inf) { inputFile= inf; }
	
	protected void read() throws IOException 
	{ 
		if (inputFile!=null) {
			seqid= SeqFileInfo.cleanSeqID(inputFile.getName());  
			seqid= SeqFileInfo.getNextBlankID(seqid);
			}
		super.read();
	}
	
}
