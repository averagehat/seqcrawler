package iubio.readseq;

import java.io.IOException;



//public
class PirSeqReader  extends BioseqReader
{
	final static String kEntry = "ENTRY ";
	final static String kSequence = "SEQUENCE";
	
	public PirSeqReader() {
		margin	=  0;
		addfirst= false;
		addend 	= false;  
		ungetend= true;
		//formatId= 14;
		}


	public boolean endOfSequence() {
		ungetend= (indexOfBuf(kEntry) == 0);
	  return (ungetend || indexOfBuf("///") >= 0);
		}

	protected void read() throws IOException
	{  
	  while (!allDone) {
	    while (!(endOfFile() || sWaiting.startsWith(kSequence) 
	    	|| sWaiting.startsWith(kEntry)
	    	)) getline();
	    if (nWaiting > 16) seqid= sWaiting.substring(16).toString();
	    while (!(endOfFile() || sWaiting.startsWith(kSequence)))
	    	getline();
	    readLoop();
			if (!allDone) {
	    	while (!(endOfFile() || (nWaiting > 0 && indexOfBuf(kEntry) == 0)))
	        getline();
	      }
	    if (endOfFile()) allDone = true;
	  }
	}

	
};
